package com.agrimarket.dao;

import com.agrimarket.db.DBConnection;
import com.agrimarket.model.Account;
import com.agrimarket.model.Buyer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class BuyerDAO {

    public int insert(String name, String phone, String city, String passwordHash) throws SQLException {
        String sql = "INSERT INTO buyers (name, phone, city, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, city);
            ps.setString(4, passwordHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public boolean phoneExists(String phone) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM buyers WHERE phone = ?")) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Optional<Account<Buyer>> findByPhone(String phone) throws SQLException {
        String sql = "SELECT buyer_id, name, phone, city, password_hash FROM buyers WHERE phone = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Buyer b = new Buyer(rs.getInt("buyer_id"), rs.getString("name"), rs.getString("phone"),
                        rs.getString("city"));
                return Optional.of(new Account<>(b, rs.getString("password_hash")));
            }
        }
    }
}
