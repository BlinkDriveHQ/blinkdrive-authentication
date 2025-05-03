package edu.co.upb.blinkdrive.auth.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final Properties properties = new Properties();

    static {
        try {
            // Load configuration from classpath resource
            try (InputStream inputStream = DatabaseManager.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {

                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    throw new RuntimeException("Could not find application.properties on classpath");
                }
            }

            // Load the JDBC driver
            Class.forName(properties.getProperty("db.driver"));
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error initializing DatabaseManager: " + e.getMessage());
            throw new RuntimeException("Failed to initialize DatabaseManager", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            properties.getProperty("db.url"),
            properties.getProperty("db.user"),
            properties.getProperty("db.password")
        );
    }
}