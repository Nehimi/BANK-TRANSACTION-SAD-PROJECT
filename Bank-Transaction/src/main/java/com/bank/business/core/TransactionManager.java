package com.bank.business.core;

import com.bank.business.commands.Command;
import com.bank.data.DatabaseConfig;
import com.bank.data.dao.BankAccountDAO;
import com.bank.data.dao.CommandHistoryDAO;
import com.bank.data.dao.CommandHistoryDAO.CommandRecord;
import com.bank.data.dao.TransactionDAO;
import com.bank.data.models.BankAccount;
import com.bank.data.models.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TransactionManager {

    private static TransactionManager instance;
    private Map<String, Stack<Command>> accountHistory;
    private CommandHistoryDAO commandHistoryDAO;
    private BankAccountDAO accountDAO;
    private TransactionDAO transactionDAO;

    private TransactionManager() {
        accountHistory = new HashMap<>();
        commandHistoryDAO = new CommandHistoryDAO();
        accountDAO = new BankAccountDAO();
        transactionDAO = new TransactionDAO();
    }

    public static TransactionManager getInstance() {
        if (instance == null) {
            instance = new TransactionManager();
        }
        return instance;
    }

    public void executeCommand(Command command) {
        command.execute();
        String accNo = command.getAccountNumber();
        accountHistory.putIfAbsent(accNo, new Stack<>());
        accountHistory.get(accNo).push(command);
    }

    /**
     * Undo the last transaction for an account.
     * First tries in-memory history, then falls back to persistent (database)
     * history.
     */
    public void undoLastCommand(String accountNumber) {
        // Try in-memory first (current session)
        Stack<Command> history = accountHistory.get(accountNumber);
        if (history != null && !history.isEmpty()) {
            Command lastCommand = history.pop();
            lastCommand.undo();
            return;
        }

        // Fallback: Use persistent history from database
        undoFromDatabase(accountNumber);
    }

    /**
     * Performs undo using the persistent command history stored in the database.
     * This works even after the application has been restarted.
     */
    private void undoFromDatabase(String accountNumber) {
        CommandRecord record = commandHistoryDAO.getLastUndoableCommand(accountNumber);

        if (record == null) {
            System.out.println("[!] No transactions to undo for account: " + accountNumber);
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Lock the account row
                BankAccount account = accountDAO.getAccountForUpdate(conn, accountNumber);
                if (account == null) {
                    throw new SQLException("Account not found!");
                }

                switch (record.getCommandType()) {
                    case "DEPOSIT":
                        // Undo deposit = subtract the amount
                        account.updateBalance(-record.getAmount());
                        accountDAO.updateAccountBalance(conn, account);

                        Transaction undoDepositTx = new Transaction(accountNumber, "UNDO_DEPOSIT", record.getAmount(),
                                LocalDateTime.now());
                        transactionDAO.saveTransaction(conn, undoDepositTx);
                        break;

                    case "WITHDRAW":
                        // Undo withdraw = add the amount back
                        account.updateBalance(record.getAmount());
                        accountDAO.updateAccountBalance(conn, account);

                        Transaction undoWithdrawTx = new Transaction(accountNumber, "UNDO_WITHDRAW", record.getAmount(),
                                LocalDateTime.now());
                        transactionDAO.saveTransaction(conn, undoWithdrawTx);
                        break;

                    case "TRANSFER":
                        // Undo transfer = return money to source, take from destination
                        String destAccNo = record.getDestinationAccount();
                        BankAccount destAccount = accountDAO.getAccountForUpdate(conn, destAccNo);

                        if (destAccount == null) {
                            throw new SQLException("Destination account not found for undo!");
                        }

                        // Give money back to source
                        account.updateBalance(record.getAmount());
                        accountDAO.updateAccountBalance(conn, account);

                        // Take money from destination
                        destAccount.updateBalance(-record.getAmount());
                        accountDAO.updateAccountBalance(conn, destAccount);

                        // Log undo transactions
                        Transaction undoOutTx = new Transaction(accountNumber, "UNDO_TRANSFER_OUT", record.getAmount(),
                                LocalDateTime.now());
                        Transaction undoInTx = new Transaction(destAccNo, "UNDO_TRANSFER_IN", record.getAmount(),
                                LocalDateTime.now());
                        transactionDAO.saveTransaction(conn, undoOutTx);
                        transactionDAO.saveTransaction(conn, undoInTx);
                        break;

                    default:
                        System.out.println("[!] Unknown command type: " + record.getCommandType());
                        conn.rollback();
                        return;
                }

                // Mark the command as undone in the database
                commandHistoryDAO.markAsUndone(conn, record.getId());

                conn.commit();
                System.out.println(" Successfully undid " + record.getCommandType() + " of $" + record.getAmount()
                        + " for account " + accountNumber);

            } catch (SQLException ex) {
                conn.rollback();
                System.out.println("[X] Undo Failed! Rolling back. Error: " + ex.getMessage());
            }
        } catch (SQLException e) {
            System.out.println("[X] Database connection error: " + e.getMessage());
        }
    }

    // For legacy support or global views if needed
    public List<Command> getGlobalHistory() {
        List<Command> global = new ArrayList<>();
        accountHistory.values().forEach(global::addAll);
        return global;
    }
}
