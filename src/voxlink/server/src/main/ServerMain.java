package voxlink.server.src.main;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.database.SchemaInitializer;
import voxlink.server.src.main.network.ServerSocketListener;
import voxlink.shared.util.Constants;

import java.sql.SQLException;

/**
 * Full VoxLink server bootstrap: database schema + TCP socket listener.
 */
public class ServerMain {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Constants.DEFAULT_SERVER_PORT;

        System.out.println("Starting " + ServerConfig.APP_NAME + " v" + ServerConfig.VERSION + "...");

        try {
            System.out.println("Initializing database schema...");
            SchemaInitializer.initialize();
            System.out.println("Database ready.");
        } catch (SQLException e) {
            System.err.println("CRITICAL: Database initialization failed: " + e.getMessage());
            return;
        }

        ServerSocketListener listener = new ServerSocketListener();
        if (!listener.start(port)) {
            System.err.println("Failed to start server on port " + port);
            return;
        }

        System.out.println("VoxLink server listening on port " + port);
        System.out.println("Press Ctrl+C to stop.");

        Runtime.getRuntime().addShutdownHook(new Thread(listener::stop));

        while (listener.isRunning()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
