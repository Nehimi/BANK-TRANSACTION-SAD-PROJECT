package com.bank.data.dao;

import com.bank.data.DatabaseConfig;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO {

    public boolean authenticateAdmin(String username, String password) {
        String sql = "SELECT password FROM admins WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                try {
                    // Try checking as a BCrypt hash
                    if (storedPassword.startsWith("$2")) {
                        return BCrypt.checkpw(password, storedPassword);
                    }
                } catch (IllegalArgumentException e) {
                    // If it's not a valid hash, it might be plain text (legacy)
                }
                // Fallback to plain text comparison for legacy support
                return password.equals(storedPassword);
            }
            return false;

        } catch (SQLException e) {
            System.out.println("Database error during admin authentication: " + e.getMessage());
            return false;
        }
    }
}
