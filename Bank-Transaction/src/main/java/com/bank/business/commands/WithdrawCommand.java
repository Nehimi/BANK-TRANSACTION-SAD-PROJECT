package com.bank.business.commands;

import com.bank.data.models.BankAccount;
import com.bank.data.dao.BankAccountDAO;
import com.bank.data.dao.TransactionDAO;
import com.bank.data.models.Transaction;

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
        
        // 1. Update the balance in the Java Model (Subtract amount)
        account.updateBalance(-amount);
        
        // 2. Save the updated balance to MySQL
        accountDAO.updateAccountBalance(account);
        
        // 3. Log the transaction in MySQL
        Transaction tx = new Transaction(account.getAccountNumber(), "WITHDRAW", amount, LocalDateTime.now());
        transactionDAO.saveTransaction(tx);
        
        System.out.println("Successfully withdrew $" + amount + " from account " + account.getAccountNumber());
    }
}
