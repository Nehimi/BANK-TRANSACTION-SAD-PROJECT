package com.bank.presentation.controllers;

import com.bank.business.services.BankService;
import com.bank.data.models.BankAccount;
import com.bank.data.models.Transaction;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebController {
    private BankService bankService;
    private Javalin app;

    public WebController(BankService bankService) {
        this.bankService = bankService;
    }

    public void start(int port) {
        String staticPath = "frontend";
        if (!new java.io.File(staticPath).exists()) {
            staticPath = "Bank-Transaction/frontend";
        }

        final String finalPath = staticPath;
        app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = finalPath;
                staticFiles.location = Location.EXTERNAL;
            });
            // Fix for trailing slash issues in some browsers
            config.routing.ignoreTrailingSlashes = true;

            // Handle LocalDateTime serialization
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            config.jsonMapper(new io.javalin.json.JavalinJackson(objectMapper));
        }).start(port);

        // --- API Endpoints ---

        // 1. Authentication
        app.post("/api/login", this::handleLogin);

        // 2. Account Information
        app.get("/api/account/{accountNumber}", this::handleGetAccount);
        app.get("/api/history/{accountNumber}", this::handleGetHistory);

        // 3. Transactions
        app.post("/api/deposit", this::handleDeposit);
        app.post("/api/withdraw", this::handleWithdraw);
        app.post("/api/transfer", this::handleTransfer);
        app.post("/api/change-pin", this::handleChangePin);

        // 4. Admin Operations
        app.get("/api/admin/accounts", this::handleAdminGetAllAccounts);
        app.get("/api/admin/transactions", this::handleAdminGetAllTransactions);
        app.post("/api/admin/create-account", this::handleAdminCreateAccount);
        app.post("/api/admin/update-status", this::handleAdminUpdateStatus);
        app.post("/api/admin/undo", this::handleAdminUndo);
        app.post("/api/admin/reset-pin", this::handleAdminResetPin);
        app.delete("/api/admin/delete-account/{accountNumber}", this::handleAdminDeleteAccount);

        System.out.println("[Web Server] Started on http://localhost:" + port);
    }

    private void handleLogin(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String role = body.get("role");
        String identifier = body.get("identifier"); // accNo or username
        String pin = body.get("pin"); // pin or password

        boolean success = false;
        if ("admin".equalsIgnoreCase(role)) {
            success = bankService.authenticateAdmin(identifier, pin);
        } else {
            success = bankService.authenticateCustomer(identifier, pin);
        }

        if (success) {
            ctx.json(Map.of("status", "success", "message", "Login successful"));
        } else {
            ctx.status(401).json(Map.of("status", "error", "message", "Invalid credentials"));
        }
    }

    private void handleGetAccount(Context ctx) {
        String accNo = ctx.pathParam("accountNumber");
        BankAccount account = bankService.getAccountDetails(accNo);
        if (account != null) {
            // Don't send PIN to frontend
            Map<String, Object> response = new HashMap<>();
            response.put("accountNumber", account.getAccountNumber());
            response.put("accountHolder", account.getAccountHolder());
            response.put("balance", account.getBalance());
            response.put("status", account.getStatus());
            ctx.json(response);
        } else {
            ctx.status(404).json(Map.of("status", "error", "message", "Account not found"));
        }
    }

    private void handleGetHistory(Context ctx) {
        String accNo = ctx.pathParam("accountNumber");
        List<Transaction> history = bankService.getAccountHistory(accNo);
        ctx.json(history);
    }

    private void handleDeposit(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String accNo = (String) body.get("accountNumber");
        double amount = Double.parseDouble(body.get("amount").toString());

        try {
            bankService.processDeposit(accNo, amount);
            ctx.json(Map.of("status", "success", "message", "Deposit successful"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleWithdraw(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String accNo = (String) body.get("accountNumber");
        double amount = Double.parseDouble(body.get("amount").toString());

        try {
            bankService.processWithdrawal(accNo, amount);
            ctx.json(Map.of("status", "success", "message", "Withdrawal successful"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleTransfer(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String from = (String) body.get("fromAccount");
        String to = (String) body.get("toAccount");
        double amount = Double.parseDouble(body.get("amount").toString());

        try {
            bankService.processTransfer(from, to, amount);
            ctx.json(Map.of("status", "success", "message", "Transfer successful"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleChangePin(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String accNo = body.get("accountNumber");
        String oldPin = body.get("oldPin");
        String newPin = body.get("newPin");

        try {
            bankService.changePin(accNo, oldPin, newPin);
            ctx.json(Map.of("status", "success", "message", "PIN updated successfully"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleAdminGetAllAccounts(Context ctx) {
        ctx.json(bankService.getAllAccounts());
    }

    private void handleAdminGetAllTransactions(Context ctx) {
        ctx.json(bankService.getAllSystemTransactions());
    }

    private void handleAdminCreateAccount(Context ctx) {
        BankAccount account = ctx.bodyAsClass(BankAccount.class);
        try {
            bankService.createAccount(account);
            ctx.status(201).json(Map.of("status", "success", "message", "Account created successfully"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleAdminUpdateStatus(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String accNo = body.get("accountNumber");
        String status = body.get("status");

        try {
            bankService.updateAccountStatus(accNo, status);
            ctx.json(Map.of("status", "success", "message", "Account status updated to " + status));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleAdminUndo(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String accNo = body.get("accountNumber");

        try {
            bankService.undoLastTransaction(accNo);
            ctx.json(Map.of("status", "success", "message", "Last transaction undone successfully"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleAdminResetPin(Context ctx) {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String accNo = body.get("accountNumber");
        String newPin = body.get("newPin");

        try {
            // Reusing accountDAO via bankService if available, or just update directly
            bankService.adminResetPin(accNo, newPin);
            ctx.json(Map.of("status", "success", "message", "Customer PIN has been reset successfully"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleAdminDeleteAccount(Context ctx) {
        String accNo = ctx.pathParam("accountNumber");
        try {
            bankService.deleteAccount(accNo);
            ctx.json(Map.of("status", "success", "message", "Account deleted successfully"));
        } catch (Exception e) {
            ctx.status(400).json(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
