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

public class WithdrawCommand implements Command {
    private BankAccount account;
    private double amount;
    private BankAccountDAO accountDAO;
    private TransactionDAO transactionDAO;
    private CommandHistoryDAO commandHistoryDAO;

    public WithdrawCommand(BankAccount account, double amount) {
        this.account = account;
        this.amount = amount;
        this.accountDAO = new BankAccountDAO();
        this.transactionDAO = new TransactionDAO();
        this.commandHistoryDAO = new CommandHistoryDAO();
    }

    @Override
    public void execute() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }

        // Calculate fee: 5 units if amount > 5000
        double fee = (amount > 5000) ? 5.0 : 0.0;
        double totalDeduction = amount + fee;

        if (account.getBalance() < totalDeduction) {
            throw new IllegalArgumentException("Insufficient funds! Your balance is $" + account.getBalance()
                    + (fee > 0 ? ". Amount with fee: $" + totalDeduction : ""));
        }

        // Check if account is ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Transaction Failed! Account " + account.getAccountNumber() + " is FROZEN.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Lock the row to prevent concurrent modifications
                BankAccount lockedAccount = accountDAO.getAccountForUpdate(conn, account.getAccountNumber());
                if (lockedAccount == null) {
                    throw new SQLException("Account not found during transaction!");
                }

                // 2. Re-check balance using the locked (fresh) data
                if (lockedAccount.getBalance() < totalDeduction) {
                    throw new IllegalArgumentException(
                            "Insufficient funds! Your balance is $" + lockedAccount.getBalance());
                }

                // 3. Update the balance
                lockedAccount.updateBalance(-totalDeduction);

                // 4. Save the updated balance to MySQL
                accountDAO.updateAccountBalance(conn, lockedAccount);

                // 5. Log the transactions in MySQL
                Transaction tx = new Transaction(account.getAccountNumber(), "WITHDRAW", amount, LocalDateTime.now());
                transactionDAO.saveTransaction(conn, tx);

                if (fee > 0) {
                    Transaction feeTx = new Transaction(account.getAccountNumber(), "SERVICE_FEE", fee,
                            LocalDateTime.now());
                    transactionDAO.saveTransaction(conn, feeTx);
                }

                // 6. Save command to history for persistent undo
                commandHistoryDAO.saveCommand(conn, account.getAccountNumber(), "WITHDRAW", amount, null,
                        LocalDateTime.now());

                // 7. Sync the in-memory object
                account.setBalance(lockedAccount.getBalance());

                conn.commit();
                System.out.println("Successfully withdrew $" + amount + " from account " + account.getAccountNumber());
                if (fee > 0) {
                    System.out.println("A service fee of $" + fee + " was applied for large withdrawal.");
                }

            } catch (SQLException ex) {
                conn.rollback();
                account.updateBalance(totalDeduction);
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

    @Override
    public String getAccountNumber() {
        return account.getAccountNumber();
    }
}
