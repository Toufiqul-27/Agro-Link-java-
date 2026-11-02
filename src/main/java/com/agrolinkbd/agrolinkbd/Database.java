package com.agrolinkbd.agrolinkbd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates a fresh connection per operation; callers must close it. */
public final class Database {
    private Database() {}

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }

    public static Connection connect() {
        String url = System.getenv().getOrDefault(
                "AGROLINK_DB_URL", "jdbc:mysql://localhost:3306/agrolink_db");
        String user = requiredEnv("AGROLINK_DB_USER");
        String password = requiredEnv("AGROLINK_DB_PASSWORD");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot connect to MySQL. Check your server and AGROLINK_DB_* settings.", e);
        }
    }
}
