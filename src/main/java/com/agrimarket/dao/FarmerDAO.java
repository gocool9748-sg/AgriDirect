package com.agrimarket.dao;

import com.agrimarket.db.DBConnection;
import com.agrimarket.model.Account;
import com.agrimarket.model.Farmer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class FarmerDAO {

    public int insert(String name, String phone, String village, String district, String passwordHash)
            throws SQLException {
        String sql = "INSERT INTO farmers (name, phone, village, district, password_hash) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, village);
            ps.setString(4, district);
            ps.setString(5, passwordHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public boolean phoneExists(String phone) throws SQLException {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM farmers WHERE phone = ?")) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Optional<Account<Farmer>> findByPhone(String phone) throws SQLException {
        String sql = "SELECT farmer_id, name, phone, village, district, password_hash FROM farmers WHERE phone = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Farmer f = new Farmer(rs.getInt("farmer_id"), rs.getString("name"), rs.getString("phone"),
                        rs.getString("village"), rs.getString("district"));
                return Optional.of(new Account<>(f, rs.getString("password_hash")));
            }
        }
    }
}
