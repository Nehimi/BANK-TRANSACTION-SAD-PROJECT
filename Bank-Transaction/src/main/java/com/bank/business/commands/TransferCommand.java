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

public class TransferCommand implements Command {
    private BankAccount sourceAccount;
    private BankAccount destinationAccount;
    private double amount;
    private BankAccountDAO accountDAO;
    private TransactionDAO transactionDAO;
    private CommandHistoryDAO commandHistoryDAO;

    public TransferCommand(BankAccount sourceAccount, BankAccount destinationAccount, double amount) {
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.accountDAO = new BankAccountDAO();
        this.transactionDAO = new TransactionDAO();
        this.commandHistoryDAO = new CommandHistoryDAO();
    }

    @Override
    public void execute() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }
        if (sourceAccount.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds in source account!");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Lock both rows to prevent concurrent modifications
                BankAccount lockedSource = accountDAO.getAccountForUpdate(conn, sourceAccount.getAccountNumber());
                BankAccount lockedDest = accountDAO.getAccountForUpdate(conn, destinationAccount.getAccountNumber());

                if (lockedSource == null || lockedDest == null) {
                    throw new SQLException("Account not found during transaction!");
                }

                // 2. Re-check balance using the locked (fresh) data
                if (lockedSource.getBalance() < amount) {
                    throw new IllegalArgumentException("Insufficient funds in source account!");
                }

                // 3. Update source balance
                lockedSource.updateBalance(-amount);
                accountDAO.updateAccountBalance(conn, lockedSource);

                // 4. Update destination balance
                lockedDest.updateBalance(amount);
                accountDAO.updateAccountBalance(conn, lockedDest);

                // 5. Log transactions
                Transaction outTx = new Transaction(sourceAccount.getAccountNumber(), "TRANSFER_OUT", amount,
                        LocalDateTime.now());
                Transaction inTx = new Transaction(destinationAccount.getAccountNumber(), "TRANSFER_IN", amount,
                        LocalDateTime.now());

                transactionDAO.saveTransaction(conn, outTx);
                transactionDAO.saveTransaction(conn, inTx);

                // 6. Save command to history for persistent undo
                commandHistoryDAO.saveCommand(conn, sourceAccount.getAccountNumber(), "TRANSFER", amount, destinationAccount.getAccountNumber(), LocalDateTime.now());

                // 7. Sync the in-memory objects
                sourceAccount.setBalance(lockedSource.getBalance());
                destinationAccount.setBalance(lockedDest.getBalance());

                conn.commit();
                System.out.println(
                        "Successfully transferred $" + amount + " to account " + destinationAccount.getAccountNumber());
            } catch (SQLException ex) {
                conn.rollback();
                sourceAccount.updateBalance(amount);
                destinationAccount.updateBalance(-amount);
                System.out.println("Transfer Failed! Rolling back database changes.");
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
                // Reverse the transfer
                sourceAccount.updateBalance(amount);
                accountDAO.updateAccountBalance(conn, sourceAccount);

                destinationAccount.updateBalance(-amount);
                accountDAO.updateAccountBalance(conn, destinationAccount);

                // Log undo transactions
                Transaction outUndo = new Transaction(sourceAccount.getAccountNumber(), "UNDO_TRANSFER_OUT", amount,
                        LocalDateTime.now());
                Transaction inUndo = new Transaction(destinationAccount.getAccountNumber(), "UNDO_TRANSFER_IN", amount,
                        LocalDateTime.now());

                transactionDAO.saveTransaction(conn, outUndo);
                transactionDAO.saveTransaction(conn, inUndo);

                conn.commit();
                System.out.println(
                        "⏪ Undid transfer of $" + amount + " to account " + destinationAccount.getAccountNumber());
            } catch (SQLException ex) {
                conn.rollback();
                sourceAccount.updateBalance(-amount);
                destinationAccount.updateBalance(amount);
                System.out.println("Undo Failed! Rolling back database changes.");
            }
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }
    }

    @Override
    public String getAccountNumber() {
        return sourceAccount.getAccountNumber();
    }
}
