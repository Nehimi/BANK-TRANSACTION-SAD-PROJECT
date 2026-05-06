package com.bank.data.dao;

import com.bank.data.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * DAO for persisting command execution history.
 * Enables the system to undo transactions even after a restart.
 */
public class CommandHistoryDAO {

    /**
     * Saves a command record to the database after execution.
     */
    public void saveCommand(Connection conn, String accountNumber, String commandType,
                            double amount, String destinationAccount, LocalDateTime executedAt) throws SQLException {

        String sql = "INSERT INTO command_history (account_number, command_type, amount, destination_account, executed_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            stmt.setString(2, commandType);
            stmt.setDouble(3, amount);
            stmt.setString(4, destinationAccount);  // null for non-transfer commands
            stmt.setObject(5, executedAt);
            stmt.executeUpdate();
        }
    }

    /**
     * Fetches the last non-undone command for a given account.
     * Returns a ResultSet-like object with the command details.
     */
    public CommandRecord getLastUndoableCommand(String accountNumber) {
        String sql = "SELECT * FROM command_history WHERE account_number = ? AND is_undone = FALSE ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new CommandRecord(
                        rs.getInt("id"),
                        rs.getString("account_number"),
                        rs.getString("command_type"),
                        rs.getDouble("amount"),
                        rs.getString("destination_account"),
                        rs.getTimestamp("executed_at").toLocalDateTime()
                );
            }
        } catch (SQLException e) {
            System.out.println("Error fetching last command: " + e.getMessage());
        }
        return null;
    }

    /**
     * Marks a command as undone in the database.
     */
    public void markAsUndone(Connection conn, int commandId) throws SQLException {
        String sql = "UPDATE command_history SET is_undone = TRUE WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commandId);
            stmt.executeUpdate();
        }
    }

    /**
     * Inner class to hold command record data retrieved from the database.
     */
    public static class CommandRecord {
        private int id;
        private String accountNumber;
        private String commandType;
        private double amount;
        private String destinationAccount;
        private LocalDateTime executedAt;

        public CommandRecord(int id, String accountNumber, String commandType,
                             double amount, String destinationAccount, LocalDateTime executedAt) {
            this.id = id;
            this.accountNumber = accountNumber;
            this.commandType = commandType;
            this.amount = amount;
            this.destinationAccount = destinationAccount;
            this.executedAt = executedAt;
        }

        public int getId() { return id; }
        public String getAccountNumber() { return accountNumber; }
        public String getCommandType() { return commandType; }
        public double getAmount() { return amount; }
        public String getDestinationAccount() { return destinationAccount; }
        public LocalDateTime getExecutedAt() { return executedAt; }
    }
}
