package com.bank.data.dao;

import com.bank.data.models.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TransactionDAO {

    // Save a new transaction to the database
    public void saveTransaction(Connection conn, Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (account_number, type, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transaction.getAccountNumber());
            stmt.setString(2, transaction.getType());
            stmt.setDouble(3, transaction.getAmount());
            stmt.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));

            stmt.executeUpdate();

        }
    }

    // Get transaction history for an account
    public java.util.List<Transaction> getTransactionsByAccount(String accountNumber) {
        java.util.List<Transaction> history = new java.util.ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_number = ? ORDER BY timestamp DESC";

        try (Connection conn = com.bank.data.DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);
            java.sql.ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction t = new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getString("account_number"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getTimestamp("timestamp").toLocalDateTime());
                history.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    // Get all system transactions
    public java.util.List<Transaction> getAllTransactions() {
        java.util.List<Transaction> history = new java.util.ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC";

        try (Connection conn = com.bank.data.DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                java.sql.ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Transaction t = new Transaction(
                        rs.getInt("transaction_id"),
                        rs.getString("account_number"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getTimestamp("timestamp").toLocalDateTime());
                history.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }
}
