package com.agrolinkbd.agrolinkbd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static Database instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/agrolink_db";
    private static final String USER = "root";
    private static final String PASSWORD = "ovi@2327";

    private Database() {
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed!", e);
        }
    }

    public static synchronized Connection connect() {
        if (instance == null) instance = new Database();
        try {
            if (instance.connection.isClosed()) instance = new Database();
        } catch (SQLException e) { instance = new Database(); }
        return instance.connection;
    }
}