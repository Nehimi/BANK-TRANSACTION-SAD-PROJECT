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
        System.out.println("WELCOME TO THE CUSTOMER PORTAL");
        System.out.println("=====================================");

        System.out.print("Please enter your Account Number (or 'exit' to quit): ");
        String accountNumber = scanner.nextLine().trim();

        if (accountNumber.equalsIgnoreCase("exit")) {
            return;
        }

        // Validate: Account number must be digits only
        if (!accountNumber.matches("\\d+")) {
            System.out.println("[X] Invalid input! Account Number must contain only numbers.");
            return;
        }

        try {
            BankAccount account = bankService.getAccountDetails(accountNumber);
            if (account == null) {
                System.out.println("[X] Account not found!");
                return;
            }

            // PIN Authentication
            System.out.print("Enter your 4-digit PIN: ");
            String enteredPin = scanner.nextLine().trim();

            if (!bankService.authenticateCustomer(accountNumber, enteredPin)) {
                System.out.println("Incorrect PIN! Access Denied.");
                return;
            }

            boolean active = true;
            while (active) {
                System.out.println("\n-------------------------------------");
                System.out.println("Welcome, " + account.getAccountHolder());
                System.out.println("Current Balance: $" + account.getBalance());
                System.out.println("-------------------------------------");
                System.out.println("1. Deposit Money");
                System.out.println("2. Withdraw Money");
                System.out.println("3. View Transaction History");
                System.out.println("4. Transfer Money");
                System.out.println("5. Change PIN");
                System.out.println("6. Exit System");
                System.out.print("> Choose an option (1-6): ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        try {
                            System.out.print("Enter amount to deposit: $");
                            double depositAmount = Double.parseDouble(scanner.nextLine().trim());
                            bankService.processDeposit(accountNumber, depositAmount);
                            account = bankService.getAccountDetails(accountNumber);
                        } catch (NumberFormatException e) {
                            System.out.println("[!] Invalid input. Please enter a valid number.");
                        } catch (IllegalArgumentException e) {
                            System.out.println("[!] " + e.getMessage());
                        }
                        break;
                    case "2":
                        try {
                            System.out.print("Enter amount to withdraw: $");
                            double withdrawAmount = Double.parseDouble(scanner.nextLine().trim());
                            bankService.processWithdrawal(accountNumber, withdrawAmount);
                            account = bankService.getAccountDetails(accountNumber);
                        } catch (NumberFormatException e) {
                            System.out.println("[!] Invalid input. Please enter a valid number.");
                        } catch (IllegalArgumentException e) {
                            System.out.println("[!] " + e.getMessage());
                        }
                        break;
                    case "3":
                        System.out.println("\n--- Transaction History ---");
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
                        try {
                            System.out.print("Enter Destination Account Number: ");
                            String toAcc = scanner.nextLine().trim();
                            if (!toAcc.matches("\\d+")) {
                                System.out.println("[!] Invalid Account Number! Must contain only numbers.");
                                break;
                            }
                            System.out.print("Enter Amount to Transfer: $");
                            double transferAmount = Double.parseDouble(scanner.nextLine().trim());
                            bankService.processTransfer(accountNumber, toAcc, transferAmount);
                            account = bankService.getAccountDetails(accountNumber);
                        } catch (NumberFormatException e) {
                            System.out.println("[!] Invalid input. Please enter a valid number.");
                        } catch (IllegalArgumentException e) {
                            System.out.println("[!] " + e.getMessage());
                        }
                        break;
                    case "5":
                        try {
                            System.out.print("Enter current PIN: ");
                            String oldPin = scanner.nextLine().trim();
                            System.out.print("Enter new 4-digit PIN: ");
                            String newPin = scanner.nextLine().trim();
                            bankService.changePin(accountNumber, oldPin, newPin);
                            System.out.println("[OK] PIN changed successfully!");
                            account = bankService.getAccountDetails(accountNumber);
                        } catch (IllegalArgumentException e) {
                            System.out.println("[!] " + e.getMessage());
                        } catch (Exception e) {
                            System.out.println("[X] Error: " + e.getMessage());
                        }
                        break;
                    case "6":
                        System.out.println("Logging out of account...");
                        active = false;
                        break;
                    default:
                        System.out.println("[!] Invalid option.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
