package voxlink.server.src.main.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import voxlink.server.src.main.config.ServerConfig;

public class DBConnection {

    private static Connection connection;
    public static Connection getConnection() {
        try {

            if(connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(ServerConfig.DB_URL, ServerConfig.DB_USER,
                        ServerConfig.DB_PASSWORD);
                System.out.println("[DB] Connected Successfully");
            }

            return connection;
        } catch (SQLException e) {
            System.out.println("Unable to connect to the database!!");
            throw new RuntimeException(e.getMessage());
        }
    }
}
