package voxlink.server.src.main.rmi;

import voxlink.server.src.main.config.ServerConfig;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

/**
 * RMIServer initializes and starts the RMI registry for VoxLink.
 */
public class RMIServer {

    private static final Logger LOGGER = Logger.getLogger(RMIServer.class.getName());

    private Registry registry;
    private RemoteServiceImpl remoteService;

    // Start the RMI server with default configuration
    public void start() {
        start(ServerConfig.RMI_PORT);
    }

    // Start the RMI server on the specified port
    public void start(int port) {
        try {
            // Create RMI registry on the specified port
            registry = LocateRegistry.createRegistry(port);
            LOGGER.info("RMI Registry started on port " + port);

            // Create remote service implementation
            remoteService = new RemoteServiceImpl();
            LOGGER.info("RemoteServiceImpl created");

            // Bind the remote service to the registry
            registry.bind(ServerConfig.RMI_SERVICE_NAME, remoteService);
            LOGGER.info("Remote service bound as: " + ServerConfig.RMI_SERVICE_NAME);

            System.out.println("=========================================");
            System.out.println("RMI Server is running!");
            System.out.println("Registry Port: " + port);
            System.out.println("Service Name: " + ServerConfig.RMI_SERVICE_NAME);
            System.out.println("RMI URL: rmi://localhost:" + port + "/" + ServerConfig.RMI_SERVICE_NAME);
            System.out.println("=========================================");

        } catch (RemoteException e) {
            LOGGER.severe("Failed to create RMI registry: " + e.getMessage());
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            LOGGER.severe("Service name already bound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Stop the RMI server and unbind the remote service
    public void stop() {
        try {
            if (registry != null) {
                registry.unbind(ServerConfig.RMI_SERVICE_NAME);
                LOGGER.info("Remote service unbound");
            }

            if (remoteService != null) {
                UnicastRemoteObject.unexportObject(remoteService, true);
                LOGGER.info("Remote service unexported");
            }

            System.out.println("RMI Server stopped");

        } catch (Exception e) {
            LOGGER.severe("Error stopping RMI server: " + e.getMessage());
        }
    }

    // Get the remote service instance
    public RemoteServiceImpl getRemoteService() {
        return remoteService;
    }

    // Check if RMI server is running
    public boolean isRunning() {
        return registry != null;
    }

    public static void main(String[] args) {
        RMIServer rmiServer = new RMIServer();

        int port = ServerConfig.RMI_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + port);
            }
        }

        // Add shutdown hook to clean up
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down RMI Server...");
            rmiServer.stop();
        }));

        rmiServer.start(port);
    }
}