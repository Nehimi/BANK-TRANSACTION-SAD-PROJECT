package com.bank.business.commands;

import com.bank.data.DatabaseConfig;
import com.bank.data.models.BankAccount;
import com.bank.data.dao.BankAccountDAO;
import com.bank.data.dao.CommandHistoryDAO;
import com.bank.data.dao.TransactionDAO;
import com.bank.data.models.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class DepositCommand implements Command {
    private BankAccount account;
    private double amount;
    private BankAccountDAO accountDAO;
    private TransactionDAO transactionDAO;
    private CommandHistoryDAO commandHistoryDAO;

    public DepositCommand(BankAccount account, double amount) {
        this.account = account;
        this.amount = amount;
        this.accountDAO = new BankAccountDAO();
        this.transactionDAO = new TransactionDAO();
        this.commandHistoryDAO = new CommandHistoryDAO();
    }

    @Override
    public void execute() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Lock the row to prevent concurrent modifications
                BankAccount lockedAccount = accountDAO.getAccountForUpdate(conn, account.getAccountNumber());
                if (lockedAccount == null) {
                    throw new SQLException("Account not found during transaction!");
                }

                // 2. Update the balance using the locked (fresh) data
                lockedAccount.updateBalance(amount);

                // 3. Save the updated balance to MySQL
                accountDAO.updateAccountBalance(conn, lockedAccount);

                // 4. Log the transaction in MySQL
                Transaction tx = new Transaction(account.getAccountNumber(), "DEPOSIT", amount, LocalDateTime.now());
                transactionDAO.saveTransaction(conn, tx);

                // 5. Save command to history for persistent undo
                commandHistoryDAO.saveCommand(conn, account.getAccountNumber(), "DEPOSIT", amount, null, LocalDateTime.now());

                // 6. Sync the in-memory object
                account.setBalance(lockedAccount.getBalance());

                conn.commit();
                System.out.println("Successfully deposited $" + amount + " to account " + account.getAccountNumber());

            } catch (SQLException ex) {
                conn.rollback();
                account.updateBalance(-amount);
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
                account.updateBalance(-amount); // reverse deposit
                accountDAO.updateAccountBalance(conn, account);
                Transaction tx = new Transaction(account.getAccountNumber(), "UNDO_DEPOSIT", amount,
                        LocalDateTime.now());
                transactionDAO.saveTransaction(conn, tx);
                conn.commit();
                System.out.println("⏪ Undid deposit of $" + amount + " from account " + account.getAccountNumber());
            } catch (SQLException ex) {
                conn.rollback();
                account.updateBalance(amount);
                System.out.println("Undo Failed! Rolling back database changes.");
            }
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }
    }

    @Override
    public String getAccountNumber() {
        return account.getAccountNumber();
    }
}
