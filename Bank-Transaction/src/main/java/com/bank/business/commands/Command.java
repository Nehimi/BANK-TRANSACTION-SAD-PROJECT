package com.bank.business.commands;

public interface Command {
    void execute();

    void undo();
}
