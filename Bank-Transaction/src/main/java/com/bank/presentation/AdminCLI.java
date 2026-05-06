package com.bank.presentation;

import com.bank.business.services.BankService;
import com.bank.data.models.BankAccount;

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
        System.out.println("👔 WELCOME TO THE ADMIN TERMINAL 👔");
        System.out.println("=====================================");

        System.out.print("👤 Enter Admin Username (or 'exit' to quit): ");
        String username = scanner.nextLine().trim();
        
        if (username.equalsIgnoreCase("exit"))
            return;

        System.out.print("🔒 Enter Admin Password: ");
        String password = scanner.nextLine().trim();

        if (!bankService.authenticateAdmin(username, password)) {
            System.out.println("❌ Incorrect Username or Password! Access Denied.");
            return;
        }

        boolean active = true;
        while (active) {
            System.out.println("\n-------------------------------------");
            System.out.println("👔 ADMIN MENU");
            System.out.println("-------------------------------------");
            System.out.println("1️⃣ Create a New Bank Account");
            System.out.println("2️⃣ Undo Last Global Transaction ⏪");
            System.out.println("3️⃣ Logout (Go Back)");
            System.out.print("👉 Choose an option (1-3): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("📝 Enter New Account Number (Min 5 chars): ");
                    String accNo = scanner.nextLine().trim();
                    System.out.print("👤 Enter Full Name of Account Holder: ");
                    String holder = scanner.nextLine().trim();
                    System.out.print("🔒 Create a 4-digit PIN: ");
                    String pin = scanner.nextLine().trim();
                    System.out.print("💵 Enter Initial Deposit Amount: $");

                    try {
                        double initialDeposit = Double.parseDouble(scanner.nextLine().trim());
                        BankAccount newAccount = new BankAccount(accNo, holder, pin, initialDeposit);
                        bankService.createAccount(newAccount);
                        System.out.println("✅ Account Created Successfully for " + holder + "!");
                    } catch (NumberFormatException e) {
                        System.out.println("⚠️ Please enter a valid number for the deposit.");
                    } catch (Exception e) {
                        System.out.println("❌ Failed to create account: " + e.getMessage());
                    }
                    break;

                case "2":
                    System.out.println("\n⏪ Attempting to Undo Last System Transaction...");
                    bankService.undoLastTransaction();
                    break;

                case "3":
                    System.out.println("🔒 Logging out of Admin Terminal...");
                    active = false;
                    break;

                default:
                    System.out.println("⚠️ Invalid option.");
            }
        }
    }
}
