package com.bank.data;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static Dotenv dotenv;

    static {
        try {
            dotenv = Dotenv.load();
        } catch (Exception e) {
            // Fallback for IDEs that run the project from the parent d:\sadbProj folder
            dotenv = Dotenv.configure().directory("./Bank-Transaction").load();
        }
    }

    private static final String URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
