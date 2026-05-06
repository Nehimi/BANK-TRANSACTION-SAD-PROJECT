package com.bank.business.services;

import com.bank.business.commands.Command;
import com.bank.business.commands.DepositCommand;
import com.bank.business.commands.WithdrawCommand;
import com.bank.business.core.TransactionManager;
import com.bank.data.dao.BankAccountDAO;
import com.bank.data.models.BankAccount;

public class BankService {
    private BankAccountDAO accountDAO;
    private com.bank.data.dao.TransactionDAO transactionDAO;
    private com.bank.data.dao.AdminDAO adminDAO;
    private TransactionManager transactionManager;

    public BankService() {
        this.accountDAO = new BankAccountDAO();
        this.transactionDAO = new com.bank.data.dao.TransactionDAO();
        this.adminDAO = new com.bank.data.dao.AdminDAO();
        // Accessing the Singleton instance
        this.transactionManager = TransactionManager.getInstance();
    }

    // Called by the UI to authenticate an admin
    public boolean authenticateAdmin(String username, String password) {
        return adminDAO.authenticateAdmin(username, password);
    }

    // Called by the UI to fetch account details
    public BankAccount getAccountDetails(String accountNumber) {
        return accountDAO.getAccount(accountNumber);
    }

    // Called by the Admin UI to create a new account
    public void createAccount(BankAccount account) {
        // Simple validation
        if (account.getAccountNumber() == null || account.getAccountNumber().length() < 5) {
            throw new IllegalArgumentException("Account Number must be at least 5 characters.");
        }
        if (accountDAO.getAccount(account.getAccountNumber()) != null) {
            throw new IllegalArgumentException("Account already exists!");
        }
        accountDAO.createAccount(account);
    }

    // Called by the UI to fetch transaction history
    public java.util.List<com.bank.data.models.Transaction> getAccountHistory(String accountNumber) {
        return transactionDAO.getTransactionsByAccount(accountNumber);
    }

    // Called by the UI to undo the last transaction
    public void undoLastTransaction() {
        transactionManager.undoLastCommand();
    }

    // Called by the UI to process a deposit
    public void processDeposit(String accountNumber, double amount) {
        BankAccount account = accountDAO.getAccount(accountNumber);
        if (account != null) {
            Command deposit = new DepositCommand(account, amount);
            transactionManager.executeCommand(deposit);
        } else {
            throw new IllegalArgumentException("Account not found!");
        }
    }

    // Called by the UI to process a withdrawal
    public void processWithdrawal(String accountNumber, double amount) {
        BankAccount account = accountDAO.getAccount(accountNumber);
        if (account != null) {
            Command withdraw = new WithdrawCommand(account, amount);
            transactionManager.executeCommand(withdraw);
        } else {
            throw new IllegalArgumentException("Account not found!");
        }
    }
}
