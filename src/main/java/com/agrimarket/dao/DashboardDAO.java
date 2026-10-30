package com.agrimarket.dao;

import com.agrimarket.db.DBConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregate queries behind the admin and farmer dashboards. Periods are the last 30 days. */
public class DashboardDAO {

    public record AdminStats(int farmers, int buyers, int activeListings, int orders30d, BigDecimal revenue30d,
                             int openOrders) {
    }

    public record FarmerStats(BigDecimal earnings30d, int pendingOrders, int activeListings, int lowStock,
                              BigDecimal kgSold30d) {
    }

    public record TopProduct(String product, String farmer, BigDecimal kgSold, BigDecimal revenue) {
    }

    public record FarmerRow(int id, String name, String phone, String village, String district, int listings,
                            int orders, BigDecimal earnings, java.time.LocalDateTime joined) {
    }

    public record BuyerRow(int id, String name, String phone, String city, int orders, BigDecimal spent,
                           java.time.LocalDateTime joined) {
    }

    public AdminStats adminStats() throws SQLException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM farmers), "
                + "(SELECT COUNT(*) FROM buyers), "
                + "(SELECT COUNT(*) FROM products WHERE is_active = TRUE AND qty_available > 0), "
                + "(SELECT COUNT(*) FROM orders WHERE status <> 'CANCELLED' AND order_date >= NOW() - INTERVAL '30' DAY), "
                + "(SELECT COALESCE(SUM(total_amount), 0) FROM orders "
                + "   WHERE status = 'DELIVERED' AND order_date >= NOW() - INTERVAL '30' DAY), "
                + "(SELECT COUNT(*) FROM orders WHERE status IN ('PLACED','CONFIRMED','SHIPPED'))";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new AdminStats(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getBigDecimal(5),
                    rs.getInt(6));
        }
    }

    /** Number of (non-cancelled) orders per category in the last 30 days, largest first. */
    public Map<String, Integer> ordersByCategory() throws SQLException {
        String sql = "SELECT c.name, COUNT(DISTINCT oi.order_id) AS n "
                + "FROM order_items oi "
                + "JOIN orders o ON o.order_id = oi.order_id "
                + "JOIN products p ON p.product_id = oi.product_id "
                + "JOIN categories c ON c.category_id = p.category_id "
                + "WHERE o.status <> 'CANCELLED' AND o.order_date >= NOW() - INTERVAL '30' DAY "
                + "GROUP BY c.name ORDER BY n DESC, c.name";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getInt(2));
            }
        }
        return result;
    }

    public List<TopProduct> topProducts(int limit) throws SQLException {
        String sql = "SELECT p.name, f.name, SUM(oi.quantity_kg), SUM(oi.quantity_kg * oi.unit_price) AS revenue "
                + "FROM order_items oi "
                + "JOIN orders o ON o.order_id = oi.order_id "
                + "JOIN products p ON p.product_id = oi.product_id "
                + "JOIN farmers f ON f.farmer_id = p.farmer_id "
                + "WHERE o.status <> 'CANCELLED' "
                + "GROUP BY p.product_id, p.name, f.name ORDER BY revenue DESC LIMIT ?";
        List<TopProduct> list = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopProduct(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4)));
                }
            }
        }
        return list;
    }

    public FarmerStats farmerStats(int farmerId) throws SQLException {
        String sql = "SELECT "
                + "(SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE farmer_id = ? AND status = 'DELIVERED' "
                + "   AND order_date >= NOW() - INTERVAL '30' DAY), "
                + "(SELECT COUNT(*) FROM orders WHERE farmer_id = ? AND status IN ('PLACED','CONFIRMED')), "
                + "(SELECT COUNT(*) FROM products WHERE farmer_id = ? AND is_active = TRUE AND qty_available > 0), "
                + "(SELECT COUNT(*) FROM products WHERE farmer_id = ? AND is_active = TRUE AND qty_available < 10), "
                + "(SELECT COALESCE(SUM(oi.quantity_kg), 0) FROM order_items oi JOIN orders o ON o.order_id = oi.order_id "
                + "   WHERE o.farmer_id = ? AND o.status <> 'CANCELLED' AND o.order_date >= NOW() - INTERVAL '30' DAY)";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setInt(i, farmerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new FarmerStats(rs.getBigDecimal(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getBigDecimal(5));
            }
        }
    }

    public List<FarmerRow> farmers() throws SQLException {
        String sql = "SELECT f.farmer_id, f.name, f.phone, f.village, f.district, "
                + "(SELECT COUNT(*) FROM products p WHERE p.farmer_id = f.farmer_id AND p.is_active = TRUE), "
                + "(SELECT COUNT(*) FROM orders o WHERE o.farmer_id = f.farmer_id AND o.status <> 'CANCELLED'), "
                + "(SELECT COALESCE(SUM(o.total_amount), 0) FROM orders o "
                + "   WHERE o.farmer_id = f.farmer_id AND o.status = 'DELIVERED'), "
                + "f.created_at FROM farmers f ORDER BY f.name";
        List<FarmerRow> list = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp t = rs.getTimestamp(9);
                list.add(new FarmerRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getInt(6), rs.getInt(7), rs.getBigDecimal(8),
                        t == null ? null : t.toLocalDateTime()));
            }
        }
        return list;
    }

    public List<BuyerRow> buyers() throws SQLException {
        String sql = "SELECT b.buyer_id, b.name, b.phone, b.city, "
                + "(SELECT COUNT(*) FROM orders o WHERE o.buyer_id = b.buyer_id AND o.status <> 'CANCELLED'), "
                + "(SELECT COALESCE(SUM(o.total_amount), 0) FROM orders o "
                + "   WHERE o.buyer_id = b.buyer_id AND o.status <> 'CANCELLED'), "
                + "b.created_at FROM buyers b ORDER BY b.name";
        List<BuyerRow> list = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp t = rs.getTimestamp(7);
                list.add(new BuyerRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getInt(5), rs.getBigDecimal(6), t == null ? null : t.toLocalDateTime()));
            }
        }
        return list;
    }
}
