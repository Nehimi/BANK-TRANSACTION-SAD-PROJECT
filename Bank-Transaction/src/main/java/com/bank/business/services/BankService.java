package com.bank.business.services;

import com.bank.business.commands.Command;
import com.bank.business.commands.DepositCommand;
import com.bank.business.commands.WithdrawCommand;
import com.bank.business.commands.TransferCommand;
import com.bank.business.core.TransactionManager;
import org.mindrot.jbcrypt.BCrypt;
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

    // Called by the UI to authenticate a customer
    public boolean authenticateCustomer(String accountNumber, String pin) {
        BankAccount account = accountDAO.getAccount(accountNumber);
        if (account != null) {
            String storedPin = account.getPinCode();
            try {
                if (storedPin.startsWith("$2")) {
                    return BCrypt.checkpw(pin, storedPin);
                }
            } catch (IllegalArgumentException e) {
                // Not a hash
            }
            return pin.equals(storedPin);
        }
        return false;
    }

    // Called by the Admin UI to create a new account
    public void createAccount(BankAccount account) throws java.sql.SQLException {
        // 1. Account Number Validation
        if (account.getAccountNumber() == null || !account.getAccountNumber().matches("\\d{5,20}")) {
            throw new IllegalArgumentException("Account Number must be 5-20 digits.");
        }

        // 2. Account Holder Name Validation (Only letters and spaces)
        if (account.getAccountHolder() == null || !account.getAccountHolder().matches("^[a-zA-Z\\s]{3,100}$")) {
            throw new IllegalArgumentException(
                    "Account Holder Name must be 3-100 characters and contain only letters.");
        }

        // 3. PIN Validation (Must be 4 digits)
        if (account.getPinCode() == null || !account.getPinCode().matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }

        // 4. Initial Balance Validation
        if (account.getBalance() < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
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

    // Called by the Admin UI to fetch all system transactions
    public java.util.List<com.bank.data.models.Transaction> getAllSystemTransactions() {
        return transactionDAO.getAllTransactions();
    }

    // Called by the UI to undo the last transaction for a specific account
    public void undoLastTransaction(String accountNumber) {
        transactionManager.undoLastCommand(accountNumber);
    }

    // Called by the UI to process a deposit
    public void processDeposit(String accountNumber, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }
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
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }
        BankAccount account = accountDAO.getAccount(accountNumber);
        if (account != null) {
            if (account.getBalance() < amount) {
                throw new IllegalArgumentException("Insufficient funds!");
            }
            Command withdraw = new WithdrawCommand(account, amount);
            transactionManager.executeCommand(withdraw);
        } else {
            throw new IllegalArgumentException("Account not found!");
        }
    }

    // Called by the UI to process a transfer
    public void processTransfer(String fromAccountNumber, String toAccountNumber, double amount) {
        BankAccount fromAccount = accountDAO.getAccount(fromAccountNumber);
        BankAccount toAccount = accountDAO.getAccount(toAccountNumber);

        if (fromAccount == null) {
            throw new IllegalArgumentException("Source account not found!");
        }
        if (toAccount == null) {
            throw new IllegalArgumentException("Destination account not found!");
        }
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account!");
        }

        Command transfer = new TransferCommand(fromAccount, toAccount, amount);
        transactionManager.executeCommand(transfer);
    }

    public void changePin(String accountNumber, String oldPin, String newPin) throws java.sql.SQLException {
        BankAccount account = accountDAO.getAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found!");
        }

        // Verify current PIN using BCrypt
        if (!BCrypt.checkpw(oldPin, account.getPinCode())) {
            throw new IllegalArgumentException("Incorrect current PIN!");
        }

        if (newPin.length() != 4) {
            throw new IllegalArgumentException("New PIN must be exactly 4 digits!");
        }
        accountDAO.updatePin(accountNumber, newPin);
    }
}
