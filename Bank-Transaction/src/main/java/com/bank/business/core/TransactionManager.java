package com.bank.business.core;

import com.bank.business.commands.Command;
import java.util.ArrayList;
import java.util.List;

public class TransactionManager {
    
    // 1. Static variable holding the single instance (Singleton Pattern)
    private static TransactionManager instance;
    
    // Keeps a history of executed commands in memory
    private List<Command> commandHistory;

    // 2. Private constructor prevents other classes from creating new objects
    private TransactionManager() {
        commandHistory = new ArrayList<>();
    }

    // 3. Public static method to get the single instance globally
    public static TransactionManager getInstance() {
        if (instance == null) {
            instance = new TransactionManager();
        }
        return instance;
    }

    // Method to execute commands centrally
    public void executeCommand(Command command) {
        command.execute(); // Executes the deposit or withdraw logic
        commandHistory.add(command); // Logs the command in memory
    }
    
    // Returns the history of executed commands
    public List<Command> getCommandHistory() {
        return commandHistory;
    }
}
