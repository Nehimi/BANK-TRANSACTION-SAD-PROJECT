package com.bank.data.dao;

import com.bank.data.DatabaseConfig;
import com.bank.data.models.BankAccount;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BankAccountDAO {

    public BankAccount getAccount(String accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new BankAccount(
                        rs.getString("account_number"),
                        rs.getString("account_holder"),
                        rs.getString("pin_code"),
                        rs.getDouble("balance"),
                        rs.getString("status"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // New method for Row-Level Locking (Concurrency Prevention)
    public BankAccount getAccountForUpdate(Connection conn, String accountNumber) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new BankAccount(
                        rs.getString("account_number"),
                        rs.getString("account_holder"),
                        rs.getString("pin_code"),
                        rs.getDouble("balance"),
                        rs.getString("status"));
            }
        }
        return null;
    }

    public void updateAccountBalance(Connection conn, BankAccount account) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, account.getBalance());
            stmt.setString(2, account.getAccountNumber());
            stmt.executeUpdate();

        }
    }

    public void createAccount(BankAccount account) throws SQLException {
        String sql = "INSERT INTO accounts (account_number, account_holder, pin_code, balance, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash the PIN before storing
            String hashedPin = BCrypt.hashpw(account.getPinCode(), BCrypt.gensalt());

            stmt.setString(1, account.getAccountNumber());
            stmt.setString(2, account.getAccountHolder());
            stmt.setString(3, hashedPin);
            stmt.setDouble(4, account.getBalance());
            stmt.setString(5, account.getStatus() != null ? account.getStatus() : "ACTIVE");
            stmt.executeUpdate();

        }
    }

    public void updatePin(String accountNumber, String newPin) throws SQLException {
        String sql = "UPDATE accounts SET pin_code = ? WHERE account_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash the new PIN
            String hashedPin = BCrypt.hashpw(newPin, BCrypt.gensalt());

            stmt.setString(1, hashedPin);
            stmt.setString(2, accountNumber);
            stmt.executeUpdate();
        }
    }

    public void updateAccountStatus(String accountNumber, String newStatus) throws SQLException {
        String sql = "UPDATE accounts SET status = ? WHERE account_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setString(2, accountNumber);
            stmt.executeUpdate();
        }
    }
}
