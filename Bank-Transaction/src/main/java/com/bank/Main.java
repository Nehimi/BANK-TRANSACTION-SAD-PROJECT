package com.bank;

import com.bank.business.services.BankService;
import com.bank.presentation.AdminCLI;
import com.bank.presentation.ClientCLI;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BankService bankService = new BankService();

        System.out.println("=========================================");
        System.out.println("🌟WELCOME TO THE BANKING SYSTEM LAUNCHER 🌟");
        System.out.println("=========================================");

        while (true) {
            System.out.println("\nSelect Login Type:");
            System.out.println("1️⃣ Customer Login");
            System.out.println("2️⃣ Admin Login");
            System.out.println("3️⃣ Exit System");
            System.out.print("👉 Choose (1-3): ");

            String role = scanner.nextLine().trim();

            if (role.equals("1")) {
                ClientCLI client = new ClientCLI(bankService, scanner);
                client.start();
            } else if (role.equals("2")) {
                AdminCLI admin = new AdminCLI(bankService, scanner);
                admin.start();
            } else if (role.equals("3") || role.equalsIgnoreCase("exit")) {
                System.out.println("👋 Shutting down... Goodbye!");
                break;
            } else {
                System.out.println("⚠️ Invalid option.");
            }
        }

        scanner.close();
    }
}
