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
}
