package com.bank.data.models;

public class BankAccount {
    private String accountNumber;
    private String accountHolder;
    private String pinCode;
    private double balance;
    private String status;

    public BankAccount(String accountNumber, String accountHolder, String pinCode, double balance, String status) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.pinCode = pinCode;
        this.balance = balance;
        this.status = status;
    }

    public BankAccount() {
    }

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

    public String getStatus() {
        return status;
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void updateBalance(double amount) {
        this.balance += amount;
    }
}
