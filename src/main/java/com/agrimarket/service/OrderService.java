package com.agrimarket.service;

import com.agrimarket.dao.OrderDAO;
import com.agrimarket.dao.ProductDAO;
import com.agrimarket.db.DBConnection;
import com.agrimarket.model.CartItem;
import com.agrimarket.model.Order;
import com.agrimarket.model.OrderItem;
import com.agrimarket.model.OrderStatus;
import com.agrimarket.util.Format;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Order placement and status changes. Every operation that touches more than one row
 * runs inside a single JDBC transaction: either everything is saved or nothing is.
 */
public class OrderService {

    public static final String CASH_ON_DELIVERY = "CASH_ON_DELIVERY";
    public static final String UPI_ON_DELIVERY = "UPI_ON_DELIVERY";

    private final OrderDAO orders = new OrderDAO();
    private final ProductDAO products = new ProductDAO();

    /**
     * Places the cart. Items are grouped by farmer, and each farmer gets their own order,
     * so each farmer can confirm and ship independently.
     * @return the new order ids
     */
    public List<Integer> placeOrder(int buyerId, List<CartItem> cart, String address, String paymentMode)
            throws SQLException {
        if (cart == null || cart.isEmpty()) {
            throw new ValidationException("Your cart is empty.");
        }
        String deliveryAddress = Validate.required(address, "Delivery address", 255);
        String payment = UPI_ON_DELIVERY.equals(paymentMode) ? UPI_ON_DELIVERY : CASH_ON_DELIVERY;

        Map<Integer, List<CartItem>> byFarmer = new LinkedHashMap<>();
        for (CartItem item : cart) {
            if (item.quantityKg().signum() <= 0) {
                throw new ValidationException("Quantity for " + item.product().name() + " must be more than zero.");
            }
            byFarmer.computeIfAbsent(item.product().farmerId(), k -> new ArrayList<>()).add(item);
        }

        List<Integer> orderIds = new ArrayList<>();
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                for (Map.Entry<Integer, List<CartItem>> group : byFarmer.entrySet()) {
                    List<OrderItem> lines = new ArrayList<>();
                    BigDecimal total = BigDecimal.ZERO;
                    for (CartItem item : group.getValue()) {
                        ProductDAO.StockRow row = products.lockForOrder(c, item.product().id());
                        if (row == null || !row.active()) {
                            throw new ValidationException(item.product().name() + " is no longer listed.");
                        }
                        if (row.qty().compareTo(item.quantityKg()) < 0) {
                            throw new ValidationException("Only " + Format.kg(row.qty()) + " of " + row.name()
                                    + " is left. Please reduce the quantity.");
                        }
                        products.reduceStock(c, row.productId(), item.quantityKg());
                        // Price is taken from the database, not from what the screen showed earlier.
                        lines.add(new OrderItem(row.productId(), row.name(), item.quantityKg(), row.price()));
                        total = total.add(row.price().multiply(item.quantityKg()));
                    }
                    int orderId = orders.insertOrder(c, buyerId, group.getKey(), total, payment, deliveryAddress);
                    for (OrderItem line : lines) {
                        orders.insertItem(c, orderId, line);
                    }
                    orderIds.add(orderId);
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        return orderIds;
    }

    /** Farmer confirms, ships, delivers or rejects an order they received. */
    public void farmerUpdateStatus(int orderId, int farmerId, OrderStatus next) throws SQLException {
        changeStatus(orderId, next, lock -> {
            if (lock.farmerId() != farmerId) {
                throw new ValidationException("This order does not belong to you.");
            }
            if (!lock.status().farmerCanMoveTo(next)) {
                throw new ValidationException("An order that is " + lock.status() + " cannot be marked " + next + ".");
            }
        });
    }

    /** Buyer cancels an order that has not been shipped yet. */
    public void buyerCancel(int orderId, int buyerId) throws SQLException {
        changeStatus(orderId, OrderStatus.CANCELLED, lock -> {
            if (lock.buyerId() != buyerId) {
                throw new ValidationException("This order does not belong to you.");
            }
            if (!lock.status().buyerCanCancel()) {
                throw new ValidationException("Only orders that are PLACED or CONFIRMED can be cancelled.");
            }
        });
    }

    public List<Order> buyerOrders(int buyerId) throws SQLException {
        return orders.findByBuyer(buyerId);
    }

    public List<Order> farmerOrders(int farmerId, OrderStatus filter) throws SQLException {
        return orders.findByFarmer(farmerId, filter);
    }

    public List<OrderItem> items(int orderId) throws SQLException {
        return orders.findItems(orderId);
    }

    private interface Check {
        void verify(OrderDAO.OrderLock lock);
    }

    private void changeStatus(int orderId, OrderStatus next, Check check) throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                OrderDAO.OrderLock lock = orders.lock(c, orderId);
                if (lock == null) {
                    throw new ValidationException("Order #" + orderId + " was not found.");
                }
                check.verify(lock);
                orders.updateStatus(c, orderId, next);
                if (next == OrderStatus.CANCELLED) {
                    products.restockFromOrder(c, orderId); // return the stock to the listing
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
