package com.bank.data.models;

public class BankAccount {
    private String accountNumber;
    private String accountHolder;
    private String pinCode;
    private double balance;

    public BankAccount(String accountNumber, String accountHolder, String pinCode, double balance) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.pinCode = pinCode;
        this.balance = balance;
    }

    // Getters
    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public String getPinCode() {
        return pinCode;
    }

    public double getBalance() {
        return balance;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    // Used by commands to update balance (supports positive or negative amounts)
    public void updateBalance(double amount) {
        this.balance += amount;
    }
}
