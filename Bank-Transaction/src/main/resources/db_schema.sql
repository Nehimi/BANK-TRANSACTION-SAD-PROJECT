-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS bank_db;
USE bank_db;

-- Table for storing Bank Accounts
CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(20) PRIMARY KEY,
    account_holder VARCHAR(100) NOT NULL,
    pin_code VARCHAR(255) NOT NULL DEFAULT '1234',
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' -- 'ACTIVE' or 'FROZEN'
);

-- Table for storing Transactions History
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'DEPOSIT', 'WITHDRAW', 'UNDO'
    amount DECIMAL(15, 2) NOT NULL,
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
);

-- Insert a dummy account for testing purposes
INSERT INTO accounts (account_number, account_holder, pin_code, balance) 
VALUES ('10002489897582', 'Abebe Kebede', '1234', 1000.00)
ON DUPLICATE KEY UPDATE account_holder='Abebe Kebede', pin_code='1234';

-- Table for storing Admin Credentials
CREATE TABLE IF NOT EXISTS admins (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL
);

-- Table for Persistent Undo (Command History)
CREATE TABLE IF NOT EXISTS command_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    command_type VARCHAR(30) NOT NULL,  -- 'DEPOSIT', 'WITHDRAW', 'TRANSFER'
    amount DECIMAL(15, 2) NOT NULL,
    destination_account VARCHAR(20) DEFAULT NULL,  -- Only for TRANSFER commands
    is_undone BOOLEAN DEFAULT FALSE,
    executed_at DATETIME NOT NULL,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE CASCADE
);

-- Insert a default admin account
INSERT INTO admins (username, password) 
VALUES ('admin', 'admin123')
ON DUPLICATE KEY UPDATE password='admin123';
