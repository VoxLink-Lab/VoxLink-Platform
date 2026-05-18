package voxlink.server.src.main;

import java.sql.SQLException;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.database.SchemaInitializer;
import voxlink.server.src.main.network.ServerSocketManager;

public class ServerMain {

    public static void main(String[] args) {
        System.out.println("Starting " + ServerConfig.APP_NAME + " v" + ServerConfig.VERSION + "...");
        
        // Initialize the database tables before starting the network
        try {
            System.out.println("Initializing Database Schema...");
            SchemaInitializer.initialize();
            System.out.println("Database tables are ready!");
        } catch (SQLException e) {
            System.err.println("CRITICAL ERROR: Failed to initialize database schema: " + e.getMessage());
            return; // Stop the server if the database fails to initialize
        }
        
        // Delegate server start-up to the dedicated manager
        ServerSocketManager serverManager = new ServerSocketManager(ServerConfig.PORT);
        serverManager.start();
    }
}

