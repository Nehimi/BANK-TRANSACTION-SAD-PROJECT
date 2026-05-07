package com.bank.presentation;

import com.bank.business.services.BankService;
import com.bank.data.models.BankAccount;
import com.bank.data.models.Transaction;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class AdminCLI {
    private BankService bankService;
    private Scanner scanner;

    public AdminCLI(BankService bankService, Scanner scanner) {
        this.bankService = bankService;
        this.scanner = scanner;
    }

    public void start() {
        System.out.println("\n=====================================");
        System.out.println("WELCOME TO THE ADMIN TERMINAL");
        System.out.println("=====================================");

        System.out.print("Enter Admin Username (or 'exit' to quit): ");
        String username = scanner.nextLine().trim();

        if (username.equalsIgnoreCase("exit"))
            return;

        System.out.print("Enter Admin Password: ");
        String password = scanner.nextLine().trim();

        if (!bankService.authenticateAdmin(username, password)) {
            System.out.println("[X] Incorrect Username or Password! Access Denied.");
            return;
        }

        boolean active = true;
        while (active) {
            System.out.println("\n-------------------------------------");
            System.out.println("ADMIN MENU");
            System.out.println("-------------------------------------");
            System.out.println("1. Create a New Bank Account");
            System.out.println("2. Undo Last Transaction (By Account)");
            System.out.println("3. View All System Transactions");
            System.out.println("4. Manage Account Status (Freeze/Unfreeze)");
            System.out.println("5. Logout (Go Back)");
            System.out.print("> Choose an option (1-5): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Enter New Account Number (Min 5 chars): ");
                    String accNo = scanner.nextLine().trim();
                    System.out.print("Enter Full Name of Account Holder: ");
                    String holder = scanner.nextLine().trim();
                    System.out.print("Create a 4-digit PIN: ");
                    String pin = scanner.nextLine().trim();
                    System.out.print("Enter Initial Deposit Amount: $");

                    try {
                        double initialDeposit = Double.parseDouble(scanner.nextLine().trim());
                        BankAccount newAccount = new BankAccount(accNo, holder, pin, initialDeposit, "ACTIVE");
                        bankService.createAccount(newAccount);
                        System.out.println("[OK] Account Created Successfully for " + holder + "!");
                    } catch (NumberFormatException e) {
                        System.out.println("[!] Please enter a valid number for the deposit.");
                    } catch (Exception e) {
                        System.out.println("[X] Failed to create account: " + e.getMessage());
                    }
                    break;

                case "2":
                    System.out.print("Enter Account Number to undo last transaction: ");
                    String undoAccNo = scanner.nextLine().trim();
                    System.out.println("\n[<<] Attempting to Undo Last Transaction for account " + undoAccNo + "...");
                    bankService.undoLastTransaction(undoAccNo);
                    break;

                case "3":
                    System.out.println("\n--- All System Transactions ---");
                    List<Transaction> allHistory = bankService.getAllSystemTransactions();
                    if (allHistory.isEmpty()) {
                        System.out.println("No transactions found in the system.");
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        for (Transaction t : allHistory) {
                            System.out.printf("[%s] ACC: %-15s | %-12s : $%.2f\n",
                                    t.getTimestamp().format(formatter),
                                    t.getAccountNumber(),
                                    t.getType(),
                                    t.getAmount());
                        }
                    }
                    break;

                case "4":
                    System.out.print("Enter Account Number to change status: ");
                    String statusAccNo = scanner.nextLine().trim();
                    System.out.print("Enter New Status (ACTIVE/FROZEN): ");
                    String newStatus = scanner.nextLine().trim().toUpperCase();

                    try {
                        bankService.updateAccountStatus(statusAccNo, newStatus);
                        System.out.println("[OK] Account " + statusAccNo + " is now " + newStatus + ".");
                    } catch (Exception e) {
                        System.out.println("[X] Failed to update status: " + e.getMessage());
                    }
                    break;

                case "5":
                    System.out.println("Logging out of Admin Terminal...");
                    active = false;
                    break;

                default:
                    System.out.println("[!] Invalid option.");
            }
        }
    }
}
