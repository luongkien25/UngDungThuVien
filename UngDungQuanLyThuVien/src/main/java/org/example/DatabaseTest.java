package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/library_db";
        String username = "root";
        String password = "Duckien@25";

        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("✅ Connected successfully!");
            connection.close();
        } catch (SQLException e) {
            System.out.println("❌ Connection failed!");
            e.printStackTrace();
        }
    }
}
