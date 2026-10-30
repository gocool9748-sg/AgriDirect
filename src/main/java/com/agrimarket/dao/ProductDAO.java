package com.agrimarket.dao;

import com.agrimarket.db.DBConnection;
import com.agrimarket.model.Product;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {

    private static final String SELECT_PRODUCT =
            "SELECT p.product_id, p.farmer_id, f.name AS farmer_name, f.village, f.district, "
            + "p.category_id, c.name AS category_name, p.name, p.description, "
            + "p.price_per_kg, p.qty_available, p.is_active "
            + "FROM products p "
            + "JOIN farmers f ON f.farmer_id = p.farmer_id "
            + "JOIN categories c ON c.category_id = p.category_id ";

    /** Row returned while an order is being placed (the row is locked with FOR UPDATE). */
    public record StockRow(int productId, int farmerId, String name, BigDecimal price, BigDecimal qty,
                           boolean active) {
    }

    public int insert(int farmerId, int categoryId, String name, String description,
                      BigDecimal price, BigDecimal qty) throws SQLException {
        String sql = "INSERT INTO products (farmer_id, category_id, name, description, price_per_kg, qty_available) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, farmerId);
            ps.setInt(2, categoryId);
            ps.setString(3, name);
            ps.setString(4, description);
            ps.setBigDecimal(5, price);
            ps.setBigDecimal(6, qty);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Updates a listing; the farmer_id condition stops one farmer editing another's product. */
    public boolean update(int productId, int farmerId, int categoryId, String name, String description,
                          BigDecimal price, BigDecimal qty) throws SQLException {
        String sql = "UPDATE products SET category_id = ?, name = ?, description = ?, price_per_kg = ?, "
                + "qty_available = ? WHERE product_id = ? AND farmer_id = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setBigDecimal(4, price);
            ps.setBigDecimal(5, qty);
            ps.setInt(6, productId);
            ps.setInt(7, farmerId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateStock(int productId, int farmerId, BigDecimal qty) throws SQLException {
        String sql = "UPDATE products SET qty_available = ? WHERE product_id = ? AND farmer_id = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, qty);
            ps.setInt(2, productId);
            ps.setInt(3, farmerId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Listings are hidden rather than deleted, because past orders refer to them. */
    public boolean setActive(int productId, int farmerId, boolean active) throws SQLException {
        String sql = "UPDATE products SET is_active = ? WHERE product_id = ? AND farmer_id = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, productId);
            ps.setInt(3, farmerId);
            return ps.executeUpdate() == 1;
        }
    }

    public List<Product> findByFarmer(int farmerId) throws SQLException {
        String sql = SELECT_PRODUCT + "WHERE p.farmer_id = ? ORDER BY p.is_active DESC, p.name";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, farmerId);
            return readAll(ps);
        }
    }

    public Optional<Product> findById(int productId) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(SELECT_PRODUCT + "WHERE p.product_id = ?")) {
            ps.setInt(1, productId);
            List<Product> list = readAll(ps);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        }
    }

    /**
     * Buyer search over active, in-stock listings. Any filter may be null/blank.
     * @param text matches product name or description
     * @param location matches the farmer's village or district
     */
    public List<Product> search(String text, Integer categoryId, String location, BigDecimal maxPrice)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_PRODUCT)
                .append("WHERE p.is_active = TRUE AND p.qty_available > 0 ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            sql.append("AND (p.name LIKE ? OR p.description LIKE ?) ");
            params.add("%" + text.trim() + "%");
            params.add("%" + text.trim() + "%");
        }
        if (categoryId != null) {
            sql.append("AND p.category_id = ? ");
            params.add(categoryId);
        }
        if (location != null && !location.isBlank()) {
            sql.append("AND (f.district LIKE ? OR f.village LIKE ?) ");
            params.add("%" + location.trim() + "%");
            params.add("%" + location.trim() + "%");
        }
        if (maxPrice != null) {
            sql.append("AND p.price_per_kg <= ? ");
            params.add(maxPrice);
        }
        sql.append("ORDER BY p.price_per_kg, p.name");
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return readAll(ps);
        }
    }

    // ---- used inside the order transaction (caller owns the connection) ----

    public StockRow lockForOrder(Connection c, int productId) throws SQLException {
        String sql = "SELECT product_id, farmer_id, name, price_per_kg, qty_available, is_active "
                + "FROM products WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new StockRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getBigDecimal(4),
                        rs.getBigDecimal(5), rs.getBoolean(6));
            }
        }
    }

    public void reduceStock(Connection c, int productId, BigDecimal qty) throws SQLException {
        String sql = "UPDATE products SET qty_available = qty_available - ? "
                + "WHERE product_id = ? AND qty_available >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, qty);
            ps.setInt(2, productId);
            ps.setBigDecimal(3, qty);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Stock changed while ordering product " + productId);
            }
        }
    }

    public void restockFromOrder(Connection c, int orderId) throws SQLException {
        // Standard SQL (works on MySQL and on the built-in database): add back each item's quantity.
        String sql = "UPDATE products SET qty_available = qty_available + "
                + "(SELECT SUM(oi.quantity_kg) FROM order_items oi WHERE oi.order_id = ? AND oi.product_id = products.product_id) "
                + "WHERE product_id IN (SELECT product_id FROM order_items WHERE order_id = ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    private List<Product> readAll(PreparedStatement ps) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Product(
                        rs.getInt("product_id"),
                        rs.getInt("farmer_id"),
                        rs.getString("farmer_name"),
                        rs.getString("village"),
                        rs.getString("district"),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBigDecimal("price_per_kg"),
                        rs.getBigDecimal("qty_available"),
                        rs.getBoolean("is_active")));
            }
        }
        return list;
    }
}
