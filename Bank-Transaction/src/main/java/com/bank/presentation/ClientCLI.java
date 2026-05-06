package com.bank.presentation;

import com.bank.business.services.BankService;
import com.bank.data.models.BankAccount;
import com.bank.data.models.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class ClientCLI {
    private BankService bankService;
    private Scanner scanner;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ClientCLI(BankService bankService, Scanner scanner) {
        this.bankService = bankService;
        this.scanner = scanner;
    }

    public void start() {
        System.out.println("\n=====================================");
        System.out.println("💳 WELCOME TO THE CUSTOMER PORTAL 💳");
        System.out.println("=====================================");

        System.out.print("🔑 Please enter your Account Number (or 'exit' to quit): ");
        String accountNumber = scanner.nextLine().trim();

        if (accountNumber.equalsIgnoreCase("exit")) {
            return;
        }

        try {
            BankAccount account = bankService.getAccountDetails(accountNumber);
            if (account == null) {
                System.out.println("❌ Account not found!");
                return;
            }

            // PIN Authentication
            System.out.print("🔒 Enter your 4-digit PIN: ");
            String enteredPin = scanner.nextLine().trim();

            if (!enteredPin.equals(account.getPinCode())) {
                System.out.println("❌ Incorrect PIN! Access Denied.");
                return;
            }

            boolean active = true;
            while (active) {
                System.out.println("\n-------------------------------------");
                System.out.println("👤 Welcome, " + account.getAccountHolder());
                System.out.println("💵 Current Balance: $" + account.getBalance());
                System.out.println("-------------------------------------");
                System.out.println("1️⃣ Deposit Money");
                System.out.println("2️⃣ Withdraw Money");
                System.out.println("3️⃣ View Transaction History (Statement)");
                System.out.println("4️⃣ Logout");
                System.out.print("👉 Choose an option (1-4): ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        System.out.print("💰 Enter amount to deposit: $");
                        double depositAmount = Double.parseDouble(scanner.nextLine().trim());
                        bankService.processDeposit(accountNumber, depositAmount);
                        account = bankService.getAccountDetails(accountNumber);
                        break;
                    case "2":
                        System.out.print("💸 Enter amount to withdraw: $");
                        double withdrawAmount = Double.parseDouble(scanner.nextLine().trim());
                        bankService.processWithdrawal(accountNumber, withdrawAmount);
                        account = bankService.getAccountDetails(accountNumber);
                        break;
                    case "3":
                        System.out.println("\n📜 --- Transaction History ---");
                        List<Transaction> history = bankService.getAccountHistory(accountNumber);
                        if (history.isEmpty()) {
                            System.out.println("No transactions found.");
                        } else {
                            for (Transaction t : history) {
                                System.out.printf("[%s] %-15s : $%.2f\n",
                                        t.getTimestamp().format(formatter), t.getType(), t.getAmount());
                            }
                        }
                        break;
                    case "4":
                        System.out.println("🔒 Logging out of account...");
                        active = false;
                        break;
                    default:
                        System.out.println("⚠️ Invalid option.");
                }
            }
        } catch (Exception e) {
            System.out.println("🚨 Error: " + e.getMessage());
        }
    }
}
