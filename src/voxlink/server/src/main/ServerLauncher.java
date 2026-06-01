package voxlink.server.src.main;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.network.ServerSocketListener;

public class ServerLauncher {
    public static void main(String[] args) {
        System.out.println("Starting VoxLink Server (Login Only)...");

        try {
            System.out.println("Initializing database schema...");
            voxlink.server.src.main.database.SchemaInitializer.initialize();
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }

        ServerSocketListener server = new ServerSocketListener();
        server.start(8888);

        System.out.println("Server running on port " + 8888);
        System.out.println("Press Ctrl+C to stop");

        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
    }
}
