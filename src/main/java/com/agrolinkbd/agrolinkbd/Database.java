package com.agrolinkbd.agrolinkbd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static Database instance;
    private Connection connection;

    private static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return value;
    }

    private Database() {
        String dbUrl = System.getenv().getOrDefault("AGROLINK_DB_URL", "jdbc:mysql://localhost:3306/agrolink_db");
        String dbUser = requiredEnv("AGROLINK_DB_USER");
        String dbPassword = requiredEnv("AGROLINK_DB_PASSWORD");

        try {
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed. Check MySQL and AGROLINK_DB_* environment variables.", e);
        }
    }

    public static synchronized Connection connect() {
        try {
            if (instance == null || instance.connection.isClosed()) {
                instance = new Database();
            }
            return instance.connection;
        } catch (SQLException e) {
            instance = new Database();
            return instance.connection;
        }
    }
}
