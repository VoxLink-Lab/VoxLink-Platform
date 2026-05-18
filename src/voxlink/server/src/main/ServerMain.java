package voxlink.server.src.main;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.network.ServerSocketManager;

public class ServerMain {

    public static void main(String[] args) {
        System.out.println("Starting " + ServerConfig.APP_NAME + " v" + ServerConfig.VERSION + "...");
        
        // Delegate server start-up to the dedicated manager
        ServerSocketManager serverManager = new ServerSocketManager(ServerConfig.PORT);
        serverManager.start();
    }
}

