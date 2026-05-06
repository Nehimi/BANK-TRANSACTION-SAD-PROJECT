package com.bank.business.core;

import com.bank.business.commands.Command;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TransactionManager {

    private static TransactionManager instance;
    private Map<String, Stack<Command>> accountHistory;

    private TransactionManager() {
        accountHistory = new HashMap<>();
    }

    public static TransactionManager getInstance() {
        if (instance == null) {
            instance = new TransactionManager();
        }
        return instance;
    }

    // Method to execute commands centrally
    public void executeCommand(Command command) {
        command.execute();
        String accNo = command.getAccountNumber();
        accountHistory.putIfAbsent(accNo, new Stack<>());
        accountHistory.get(accNo).push(command);
    }

    public void undoLastCommand(String accountNumber) {
        Stack<Command> history = accountHistory.get(accountNumber);
        if (history != null && !history.isEmpty()) {
            Command lastCommand = history.pop();
            lastCommand.undo();
        } else {
            System.out.println("[!] No transactions to undo for account: " + accountNumber);
        }
    }
    
    // For legacy support or global views if needed
    public List<Command> getGlobalHistory() {
        List<Command> global = new ArrayList<>();
        accountHistory.values().forEach(global::addAll);
        return global;
    }
}
