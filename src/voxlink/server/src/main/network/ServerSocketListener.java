package voxlink.server.src.main.network;

import voxlink.server.src.main.config.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * ServerSocketListener accepts incoming client connections and spawns
 */
public class ServerSocketListener {

    private static final Logger LOGGER = Logger.getLogger(ServerSocketListener.class.getName());

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean isRunning;
    private final AtomicInteger activeConnections;
    private final AtomicInteger totalConnections;

   // Constructor - initializes connection counters
    public ServerSocketListener() {
        this.activeConnections = new AtomicInteger(0);
        this.totalConnections = new AtomicInteger(0);
    }

    // Start the server socket listener
    public boolean start(int port) {
        try {
            // Create server socket
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(ServerConfig.SOCKET_TIMEOUT_MS);
            isRunning = true;

            // Initialize thread pool for client handlers
            threadPool = Executors.newFixedThreadPool(ServerConfig.MAX_CONNECTIONS);

            System.out.println("=========================================");
            System.out.println("VoxLink Server Started!");
            System.out.println("Listening on port: " + port);
            System.out.println("Max client threads: " + ServerConfig.MAX_CONNECTIONS);
            System.out.println("=========================================");
            LOGGER.info("Server started on port " + port);

            // Start accepting connections
            acceptConnections();

            return true;

        } catch (IOException e) {
            System.err.println("[Server] Failed to start on port " + port + ": " + e.getMessage());
            LOGGER.severe("Failed to start server: " + e.getMessage());
            return false;
        }
    }

    // Main loop for accepting client connections
    private void acceptConnections() {
        while (isRunning) {
            try {
                // Wait for client connection
                Socket clientSocket = serverSocket.accept();

                // Set socket options
                clientSocket.setKeepAlive(true);
                clientSocket.setTcpNoDelay(true);

                // Increment counters
                int currentActive = activeConnections.incrementAndGet();
                int currentTotal = totalConnections.incrementAndGet();

                System.out.println("[Server] New client connected from: " +
                        clientSocket.getInetAddress().getHostAddress() +
                        " (Active: " + currentActive + ", Total: " + currentTotal + ")");

                // Create and submit client handler to thread pool
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.submit(clientHandler);

            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("[Server] Error accepting connection: " + e.getMessage());
                    LOGGER.warning("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    // Stop the server and close all connections
    public void stop() {
        System.out.println("\n[Server] Shutting down...");
        isRunning = false;

        // Close server socket to stop accepting new connections
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error closing server socket: " + e.getMessage());
        }

        // Shutdown thread pool
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();

            // Wait for existing clients to finish
            try {
                if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    System.err.println("[Server] Forcing thread pool shutdown...");
                    threadPool.shutdownNow();

                    if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.err.println("[Server] Thread pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[Server] Shutdown complete");
        LOGGER.info("Server shutdown complete");
    }

    // Get number of active client connections
    public int getActiveConnectionCount() {
        return activeConnections.get();
    }

    // Get total connections received since server start
    public int getTotalConnectionCount() {
        return totalConnections.get();
    }

    // Decrement active connection count (called when client disconnects)
    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    // Check if server is running
    public boolean isRunning() {
        return isRunning;
    }

    // Get thread pool statistics
    public String getThreadPoolStats() {
        if (threadPool == null) {
            return "Thread pool not initialized";
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) threadPool;
        return String.format("Pool Size: %d, Active: %d, Completed: %d, Queue: %d",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getCompletedTaskCount(),
                executor.getQueue().size());
    }

    public static void main(String[] args) {
        ServerSocketListener server = new ServerSocketListener();

        int port = ServerConfig.SERVER_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + port);
            }
        }

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Server] Shutdown signal received...");
            server.stop();
        }));

        // Start the server
        if (server.start(port)) {
            // Keep main thread alive
            while (server.isRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}