package voxlink.server.src.test;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.rmi.RMIServer;
import voxlink.shared.dto.ServerStatsDTO;
import voxlink.shared.dto.WorkspaceDTO;
import voxlink.shared.rmi.RemoteService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * RMITest class to verify RMI server functionality
 */
public class RMITest {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("VoxLink RMI Test Suite");
        System.out.println("=========================================\n");

        // First, start the RMI server
        System.out.println("📋 Starting RMI Server...");
        RMIServer rmiServer = new RMIServer();
        rmiServer.start();
        System.out.println("   RMI Server started\n");

        // Give server a moment to fully initialize
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Test RMI Client Connection
        testRmiConnection();

        // Stop the server
        System.out.println("\n📋 Stopping RMI Server...");
        rmiServer.stop();
        System.out.println("   RMI Server stopped\n");

        System.out.println("=========================================");
        System.out.println("RMI Test Complete!");
        System.out.println("=========================================");
    }

    private static void testRmiConnection() {
        System.out.println("📋 Testing RMI Client Connection");

        try {
            // Look up the remote service from RMI registry
            Registry registry = LocateRegistry.getRegistry("localhost", ServerConfig.RMI_PORT);
            RemoteService remoteService = (RemoteService) registry.lookup(ServerConfig.RMI_SERVICE_NAME);

            System.out.println("   Connected to RMI Service: " + ServerConfig.RMI_SERVICE_NAME);

            // Test getServerStats()
            System.out.println("\n   → Testing getServerStats()...");
            ServerStatsDTO stats = remoteService.getServerStats();
            System.out.println("   Server Stats retrieved:");
            System.out.println("      - Online Users: " + stats.getOnlineUsers());
            System.out.println("      - Total Workspaces: " + stats.getTotalWorkspaces());
            System.out.println("      - Active Connections: " + stats.getActiveConnections());

            // Test getServerUptime()
            System.out.println("\n   → Testing getServerUptime()...");
            long uptime = remoteService.getServerUptime();
            long uptimeSeconds = uptime / 1000;
            System.out.println("   Server uptime: " + uptimeSeconds + " seconds");

            // Test getActiveConnectionCount()
            System.out.println("\n   → Testing getActiveConnectionCount()...");
            int connections = remoteService.getActiveConnections();
            System.out.println("   Active connections: " + connections);

            // Test getPublicWorkspaces()
            System.out.println("\n   → Testing getPublicWorkspaces()...");
            List<WorkspaceDTO> workspaces = remoteService.getPublicWorkspaces();
            System.out.println("   Found " + workspaces.size() + " public workspace(s)");

            // Test getOnlineUserCount()
            System.out.println("\n   → Testing getOnlineUserCount()...");
            int onlineUsers = remoteService.getOnlineUserCount();
            System.out.println("   Online users: " + onlineUsers);

            System.out.println("\n   All RMI tests passed!");

        } catch (Exception e) {
            System.err.println("\n   RMI Test Failed!");
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}