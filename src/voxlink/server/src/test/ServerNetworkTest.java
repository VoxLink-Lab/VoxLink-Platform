package voxlink.server.src.test;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.network.ServerSocketListener;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * ServerNetworkTest verifies the TCP socket server functionality.
 * Tests connection, authentication, and basic packet exchange.
 */
public class ServerNetworkTest {

    private static Socket clientSocket;
    private static ObjectOutputStream outputStream;
    private static ObjectInputStream inputStream;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("VoxLink Server Network Test");
        System.out.println("=========================================\n");

        // Start the server in a separate thread
        System.out.println("📋 Starting Server...");
        Thread serverThread = new Thread(() -> {
            ServerSocketListener server = new ServerSocketListener();
            server.start(ServerConfig.PORT);
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Give server time to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Run tests
        boolean allTestsPassed = true;

        allTestsPassed &= testConnection();
        allTestsPassed &= testRegisterUser();
        allTestsPassed &= testLoginUser();
        allTestsPassed &= testInvalidLogin();
        allTestsPassed &= testGetWorkspaces();
        allTestsPassed &= testHeartbeat();

        // Print summary
        System.out.println("\n=========================================");
        if (allTestsPassed) {
            System.out.println("✅ ALL SERVER NETWORK TESTS PASSED!");
        } else {
            System.out.println("❌ SOME TESTS FAILED - Check logs above");
        }
        System.out.println("=========================================");

        // Clean up
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null) clientSocket.close();
        } catch (Exception e) {
            // Ignore
        }

        System.exit(0);
    }

    /**
     * Test 1: Connect to server
     */
    private static boolean testConnection() {
        System.out.println("📋 Test 1: Connect to Server");

        try {
            clientSocket = new Socket("localhost", ServerConfig.PORT);
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(clientSocket.getInputStream());

            System.out.println("   ✅ Connected to server on port " + ServerConfig.PORT);
            return true;

        } catch (Exception e) {
            System.out.println("   ❌ Failed to connect: " + e.getMessage());
            return false;
        }
    }

    /**
     * Test 2: Register a new user
     */
    private static boolean testRegisterUser() {
        System.out.println("\n📋 Test 2: Register New User");

        try {
            // Create register packet
            Packet registerPacket = new Packet(RequestType.AUTH_REGISTER);
            registerPacket.put("username", "testclient");
            registerPacket.put("password", "clientpass123");
            registerPacket.put("email", "testclient@example.com");
            registerPacket.put("displayName", "Test Client User");

            // Send packet
            outputStream.writeObject(registerPacket);
            outputStream.flush();

            // Receive response
            Packet response = (Packet) inputStream.readObject();

            if (response.getResponseType() == ResponseType.AUTH_REGISTER_SUCCESS) {
                UserDTO user = (UserDTO) response.get("user");
                System.out.println("   ✅ User registered successfully!");
                System.out.println("      - User ID: " + user.getId());
                System.out.println("      - Username: " + user.getUsername());
                return true;
            } else {
                System.out.println("   ❌ Registration failed: " + response.getErrorMessage());
                return false;
            }

        } catch (Exception e) {
            System.out.println("   ❌ Registration error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Test 3: Login with the registered user
     */
    private static boolean testLoginUser() {
        System.out.println("\n📋 Test 3: Login User");

        try {
            // Create login packet
            Packet loginPacket = new Packet(RequestType.AUTH_LOGIN);
            loginPacket.put("username", "testclient");
            loginPacket.put("password", "clientpass123");

            // Send packet
            outputStream.writeObject(loginPacket);
            outputStream.flush();

            // Receive response
            Packet response = (Packet) inputStream.readObject();

            if (response.getResponseType() == ResponseType.AUTH_LOGIN_SUCCESS) {
                UserDTO user = (UserDTO) response.get("user");
                String authToken = response.get("authToken").toString();

                System.out.println("   ✅ User logged in successfully!");
                System.out.println("      - User ID: " + user.getId());
                System.out.println("      - Username: " + user.getUsername());
                System.out.println("      - Auth Token: " + authToken);
                return true;
            } else {
                System.out.println("   ❌ Login failed: " + response.getErrorMessage());
                return false;
            }

        } catch (Exception e) {
            System.out.println("   ❌ Login error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Test 4: Invalid login (should fail)
     */
    private static boolean testInvalidLogin() {
        System.out.println("\n📋 Test 4: Invalid Login (Expected Failure)");

        try {
            // Create invalid login packet
            Packet loginPacket = new Packet(RequestType.AUTH_LOGIN);
            loginPacket.put("username", "nonexistent");
            loginPacket.put("password", "wrongpassword");

            // Send packet
            outputStream.writeObject(loginPacket);
            outputStream.flush();

            // Receive response
            Packet response = (Packet) inputStream.readObject();

            if (response.getResponseType() == ResponseType.AUTH_LOGIN_FAILURE) {
                System.out.println("   ✅ Invalid login correctly rejected");
                System.out.println("      - Error: " + response.getErrorMessage());
                return true;
            } else {
                System.out.println("   ❌ Invalid login was accepted (should have failed)");
                return false;
            }

        } catch (Exception e) {
            System.out.println("   ❌ Test error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Test 5: Get workspaces (requires authentication)
     */
    private static boolean testGetWorkspaces() {
        System.out.println("\n📋 Test 5: Get Workspaces (Authenticated)");

        try {
            // First login to get auth token
            Packet loginPacket = new Packet(RequestType.AUTH_LOGIN);
            loginPacket.put("username", "testclient");
            loginPacket.put("password", "clientpass123");

            outputStream.writeObject(loginPacket);
            outputStream.flush();
            Packet loginResponse = (Packet) inputStream.readObject();

            if (loginResponse.getResponseType() != ResponseType.AUTH_LOGIN_SUCCESS) {
                System.out.println("   ❌ Could not authenticate for this test");
                return false;
            }

            // Request workspaces
            Packet workspacePacket = new Packet(RequestType.WORKSPACE_LIST);
            outputStream.writeObject(workspacePacket);
            outputStream.flush();

            Packet response = (Packet) inputStream.readObject();

            if (response.getResponseType() == ResponseType.WORKSPACE_LIST_DATA) {
                System.out.println("   ✅ Retrieved workspaces successfully");
                return true;
            } else {
                System.out.println("   ❌ Failed to get workspaces: " + response.getErrorMessage());
                return false;
            }

        } catch (Exception e) {
            System.out.println("   ❌ Test error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Test 6: Heartbeat/Ping
     */
    private static boolean testHeartbeat() {
        System.out.println("\n📋 Test 6: Heartbeat/Ping");

        try {
            // Send heartbeat ping
            Packet heartbeatPacket = new Packet(RequestType.HEARTBEAT_PING);
            outputStream.writeObject(heartbeatPacket);
            outputStream.flush();

            // Receive response
            Packet response = (Packet) inputStream.readObject();

            if (response.getResponseType() == ResponseType.HEARTBEAT_PONG) {
                System.out.println("   ✅ Heartbeat response received");
                return true;
            } else {
                System.out.println("   ❌ Invalid heartbeat response");
                return false;
            }

        } catch (Exception e) {
            System.out.println("   ❌ Heartbeat error: " + e.getMessage());
            return false;
        }
    }
}