package com.bank.data.dao;

import com.bank.data.DatabaseConfig;
import com.bank.data.models.BankAccount;

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
                        rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

    public void createAccount(BankAccount account) {
        String sql = "INSERT INTO accounts (account_number, account_holder, pin_code, balance) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, account.getAccountNumber());
            stmt.setString(2, account.getAccountHolder());
            stmt.setString(3, account.getPinCode());
            stmt.setDouble(4, account.getBalance());
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }
}
