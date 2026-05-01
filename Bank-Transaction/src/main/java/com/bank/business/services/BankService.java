package com.bank.business.services;

import com.bank.business.commands.Command;
import com.bank.business.commands.DepositCommand;
import com.bank.business.commands.WithdrawCommand;
import com.bank.business.core.TransactionManager;
import com.bank.data.dao.BankAccountDAO;
import com.bank.data.models.BankAccount;

public class BankService {
    private BankAccountDAO accountDAO;
    private TransactionManager transactionManager;

    public BankService() {
        this.accountDAO = new BankAccountDAO();
        // Accessing the Singleton instance
        this.transactionManager = TransactionManager.getInstance(); 
    }

    // Called by the UI to fetch account details
    public BankAccount getAccountDetails(String accountNumber) {
        return accountDAO.getAccount(accountNumber);
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
