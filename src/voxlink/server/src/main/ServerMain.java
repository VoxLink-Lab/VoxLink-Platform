package voxlink.server.src.main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.network.ClientHandler;

public class ServerMain {

    // Thread pool to handle multiple clients efficiently without overwhelming the server
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(ServerConfig.MAX_CONNECTIONS);

    public static void main(String[] args) {
        System.out.println("Starting " + ServerConfig.APP_NAME + " v" + ServerConfig.VERSION + "...");

        try (ServerSocket serverSocket = new ServerSocket(ServerConfig.PORT)) {
            System.out.println("Server is listening on port " + ServerConfig.PORT);

            // Infinite loop to continuously accept incoming client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Create a new ClientHandler for the connected client and pass it to the thread pool
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
