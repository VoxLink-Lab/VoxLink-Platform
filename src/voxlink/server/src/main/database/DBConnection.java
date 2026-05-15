package voxlink.server.src.main.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import voxlink.server.src.main.config.ServerConfig;

public class DBConnection {

    public static Connection connect() {
        Connection connection;
        try {
            connection = DriverManager.getConnection(ServerConfig.DB_URL, ServerConfig.DB_USER,
                    ServerConfig.DB_PASSWORD);

        } catch (SQLException e) {
            System.out.println("Unable to connect to the database!!");
            throw new RuntimeException(e.getMessage());
        }

        return connection;
    }
}
