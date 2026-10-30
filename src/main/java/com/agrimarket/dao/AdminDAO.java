package com.agrimarket.dao;

import com.agrimarket.db.DBConnection;
import com.agrimarket.model.Account;
import com.agrimarket.model.Admin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AdminDAO {

    public Optional<Account<Admin>> findByUsername(String username) throws SQLException {
        String sql = "SELECT admin_id, username, password_hash FROM admins WHERE username = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Admin a = new Admin(rs.getInt("admin_id"), rs.getString("username"));
                return Optional.of(new Account<>(a, rs.getString("password_hash")));
            }
        }
    }

    /** Sets a new password hash; returns false if there is no such admin. */
    public boolean updatePassword(String username, String passwordHash) throws SQLException {
        String sql = "UPDATE admins SET password_hash = ? WHERE username = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, username);
            return ps.executeUpdate() == 1;
        }
    }
}
