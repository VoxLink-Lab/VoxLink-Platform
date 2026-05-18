package voxlink.server.src.main.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import voxlink.server.src.main.config.ServerConfig;

public class ServerSocketManager {

    private final int port;
    private final ExecutorService threadPool;
    private boolean isRunning;

    public ServerSocketManager(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(ServerConfig.MAX_CONNECTIONS);
    }

    public void start() {
        isRunning = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void stop() {
        isRunning = false;
        threadPool.shutdown();
    }
}

