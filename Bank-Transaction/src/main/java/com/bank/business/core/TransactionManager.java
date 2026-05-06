package com.bank.business.core;

import com.bank.business.commands.Command;
import java.util.ArrayList;
import java.util.List;

public class TransactionManager {

    private static TransactionManager instance;
    private List<Command> commandHistory;

    private TransactionManager() {
        commandHistory = new ArrayList<>();
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
        commandHistory.add(command); // Logs the command in memory
    }

    public List<Command> getCommandHistory() {
        return commandHistory;
    }
}
