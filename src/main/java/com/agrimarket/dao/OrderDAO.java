package com.agrimarket.dao;

import com.agrimarket.db.DBConnection;
import com.agrimarket.model.Order;
import com.agrimarket.model.OrderItem;
import com.agrimarket.model.OrderStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    /** Orders with a one-line summary of their items, e.g. "Tomato 10 kg, Brinjal 5 kg". */
    private static final String SELECT_ORDER =
            "SELECT o.order_id, o.buyer_id, b.name AS buyer_name, b.phone AS buyer_phone, "
            + "o.farmer_id, f.name AS farmer_name, f.phone AS farmer_phone, o.order_date, o.total_amount, "
            + "o.status, o.payment_mode, o.delivery_address, "
            + "GROUP_CONCAT(CONCAT(p.name, ' ', "
            + "  TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM oi.quantity_kg)), ' kg') "
            + "  ORDER BY oi.item_id SEPARATOR ', ') AS items "
            + "FROM orders o "
            + "JOIN buyers b ON b.buyer_id = o.buyer_id "
            + "JOIN farmers f ON f.farmer_id = o.farmer_id "
            + "JOIN order_items oi ON oi.order_id = o.order_id "
            + "JOIN products p ON p.product_id = oi.product_id ";

    private static final String GROUP_BY =
            " GROUP BY o.order_id, o.buyer_id, b.name, b.phone, o.farmer_id, f.name, f.phone, o.order_date, "
            + "o.total_amount, o.status, o.payment_mode, o.delivery_address ";

    /** Status and owners of an order, read with a row lock while its status changes. */
    public record OrderLock(int orderId, int buyerId, int farmerId, OrderStatus status) {
    }

    public List<Order> findByBuyer(int buyerId) throws SQLException {
        return query(SELECT_ORDER + "WHERE o.buyer_id = ?" + GROUP_BY + "ORDER BY o.order_date DESC", buyerId);
    }

    /** @param status null for all statuses */
    public List<Order> findByFarmer(int farmerId, OrderStatus status) throws SQLException {
        if (status == null) {
            return query(SELECT_ORDER + "WHERE o.farmer_id = ?" + GROUP_BY + "ORDER BY o.order_date DESC",
                    farmerId);
        }
        return query(SELECT_ORDER + "WHERE o.farmer_id = ? AND o.status = ?" + GROUP_BY
                + "ORDER BY o.order_date DESC", farmerId, status.name());
    }

    public List<Order> findAll() throws SQLException {
        return query(SELECT_ORDER + GROUP_BY + "ORDER BY o.order_date DESC");
    }

    public List<Order> findRecent(int limit) throws SQLException {
        return query(SELECT_ORDER + GROUP_BY + "ORDER BY o.order_date DESC LIMIT ?", limit);
    }

    public List<Order> findRecentForFarmer(int farmerId, int limit) throws SQLException {
        return query(SELECT_ORDER + "WHERE o.farmer_id = ?" + GROUP_BY + "ORDER BY o.order_date DESC LIMIT ?",
                farmerId, limit);
    }

    public List<OrderItem> findItems(int orderId) throws SQLException {
        String sql = "SELECT oi.product_id, p.name, oi.quantity_kg, oi.unit_price FROM order_items oi "
                + "JOIN products p ON p.product_id = oi.product_id WHERE oi.order_id = ? ORDER BY oi.item_id";
        List<OrderItem> items = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(rs.getInt(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4)));
                }
            }
        }
        return items;
    }

    /** {buyerId, farmerId} of an order, or null if it does not exist. */
    public int[] owners(int orderId) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT buyer_id, farmer_id FROM orders WHERE order_id = ?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new int[] {rs.getInt(1), rs.getInt(2)} : null;
            }
        }
    }

    // ---- used inside transactions (caller owns the connection) ----

    public int insertOrder(Connection c, int buyerId, int farmerId, BigDecimal total, String paymentMode,
                           String address) throws SQLException {
        String sql = "INSERT INTO orders (buyer_id, farmer_id, total_amount, status, payment_mode, delivery_address) "
                + "VALUES (?, ?, ?, 'PLACED', ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, farmerId);
            ps.setBigDecimal(3, total);
            ps.setString(4, paymentMode);
            ps.setString(5, address);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void insertItem(Connection c, int orderId, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity_kg, unit_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, item.productId());
            ps.setBigDecimal(3, item.quantityKg());
            ps.setBigDecimal(4, item.unitPrice());
            ps.executeUpdate();
        }
    }

    public OrderLock lock(Connection c, int orderId) throws SQLException {
        String sql = "SELECT order_id, buyer_id, farmer_id, status FROM orders WHERE order_id = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new OrderLock(rs.getInt(1), rs.getInt(2), rs.getInt(3), OrderStatus.valueOf(rs.getString(4)));
            }
        }
    }

    public void updateStatus(Connection c, int orderId, OrderStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET status = ? WHERE order_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    private List<Order> query(String sql, Object... params) throws SQLException {
        List<Order> list = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp date = rs.getTimestamp("order_date");
                    list.add(new Order(
                            rs.getInt("order_id"),
                            rs.getInt("buyer_id"),
                            rs.getString("buyer_name"),
                            rs.getString("buyer_phone"),
                            rs.getInt("farmer_id"),
                            rs.getString("farmer_name"),
                            rs.getString("farmer_phone"),
                            date == null ? null : date.toLocalDateTime(),
                            rs.getBigDecimal("total_amount"),
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getString("payment_mode"),
                            rs.getString("delivery_address"),
                            rs.getString("items")));
                }
            }
        }
        return list;
    }
}
