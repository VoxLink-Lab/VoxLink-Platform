package voxlink.server.src.main.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String url = "jdbc:mysql://localhost:3306/voxlink_db";
    private static final String password = "";
    private static final String user = "root";

    public static Connection connect() {
        Connection connection;
        try{
            connection = DriverManager.getConnection(DBConnection.url, DBConnection.user, DBConnection.password);

        } catch(SQLException e) {
            System.out.println("Unable to connect to the database!!");
            throw new RuntimeException(e.getMessage());
        }

        return connection;
    }
}
