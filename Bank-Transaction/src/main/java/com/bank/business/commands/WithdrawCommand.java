package com.bank.business.commands;

import com.bank.data.DatabaseConfig;
import com.bank.data.models.BankAccount;
import com.bank.data.dao.BankAccountDAO;
import com.bank.data.dao.TransactionDAO;
import com.bank.data.models.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class WithdrawCommand implements Command {
    private BankAccount account;
    private double amount;
    private BankAccountDAO accountDAO;
    private TransactionDAO transactionDAO;

    public WithdrawCommand(BankAccount account, double amount) {
        this.account = account;
        this.amount = amount;
        this.accountDAO = new BankAccountDAO();
        this.transactionDAO = new TransactionDAO();
    }

    @Override
    public void execute() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        if (account.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds! Your balance is $" + account.getBalance());
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Update the balance in the Java Model (Subtract amount)
                account.updateBalance(-amount);

                // 2. Save the updated balance to MySQL
                accountDAO.updateAccountBalance(conn, account);

                // 3. Log the transaction in MySQL
                Transaction tx = new Transaction(account.getAccountNumber(), "WITHDRAW", amount, LocalDateTime.now());
                transactionDAO.saveTransaction(conn, tx);

                conn.commit();
                System.out.println("Successfully withdrew $" + amount + " from account " + account.getAccountNumber());

            } catch (SQLException ex) {
                conn.rollback();
                account.updateBalance(amount);
                System.out.println("Transaction Failed! Rolling back database changes.");
                ex.printStackTrace();
            }

        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }
    }

    @Override
    public void undo() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                account.updateBalance(amount); // reverse withdraw (add back)
                accountDAO.updateAccountBalance(conn, account);
                Transaction tx = new Transaction(account.getAccountNumber(), "UNDO_WITHDRAW", amount,
                        LocalDateTime.now());
                transactionDAO.saveTransaction(conn, tx);
                conn.commit();
                System.out.println("⏪ Undid withdrawal of $" + amount + " for account " + account.getAccountNumber());
            } catch (SQLException ex) {
                conn.rollback();
                account.updateBalance(-amount);
                System.out.println("Undo Failed! Rolling back database changes.");
            }
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }
    }
}
