package voxlink.client.src.test;

import voxlink.client.src.main.model.UserModel;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.MessageCache;
import voxlink.server.src.main.network.ServerSocketListener;
import voxlink.shared.dto.UserDTO;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ClientNetworkTest verifies client-side network and model functionality.
 * Tests ServerConnection, UserModel, and state management.
 */
public class ClientNetworkTest {

    private static Thread serverThread;
    private static ServerSocketListener server;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("VoxLink Client Network Test Suite");
        System.out.println("=========================================\n");

        // Start server first
        startServer();

        // Give server time to start
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        boolean allTestsPassed = true;

        allTestsPassed &= testServerConnection();
        allTestsPassed &= testUserModelLogin();
        allTestsPassed &= testUserModelRegister();
        allTestsPassed &= testAppState();
        allTestsPassed &= testMessageCache();

        // Shutdown
        stopServer();

        System.out.println("\n=========================================");
        if (allTestsPassed) {
            System.out.println("✅ ALL CLIENT NETWORK TESTS PASSED!");
        } else {
            System.out.println("❌ SOME TESTS FAILED - Check logs above");
        }
        System.out.println("=========================================");

        System.exit(0);
    }

    private static void startServer() {
        System.out.println("📋 Starting test server...");
        serverThread = new Thread(() -> {
            server = new ServerSocketListener();
            server.start(8888);
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Test 1: Server Connection
     */
    private static boolean testServerConnection() {
        System.out.println("\n📋 Test 1: Server Connection");

        ServerConnection connection = ServerConnection.getInstance();
        boolean connected = connection.connect("localhost", 8888);

        if (connected) {
            System.out.println("   ✅ Connected to server");
            return true;
        } else {
            System.out.println("   ❌ Failed to connect");
            return false;
        }
    }

    /**
     * Test 2: UserModel Login
     */
    private static boolean testUserModelLogin() {
        System.out.println("\n📋 Test 2: UserModel Login");

        UserModel userModel = UserModel.getInstance();
        AtomicBoolean success = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        // First create a test user if not exists (using register)
        userModel.register("clienttest", "testpass123", "client@test.com", "Client Tester", result -> {
            if (result.isSuccess()) {
                System.out.println("   → Test user created");
                // Now login
                userModel.login("clienttest", "testpass123", loginResult -> {
                    if (loginResult.isSuccess()) {
                        System.out.println("   ✅ Login successful for: " + loginResult.getUser().getUsername());
                        success.set(true);
                    } else {
                        System.out.println("   ❌ Login failed: " + loginResult.getErrorMessage());
                    }
                    latch.countDown();
                });
            } else {
                // User might already exist, try login directly
                userModel.login("clienttest", "testpass123", loginResult -> {
                    if (loginResult.isSuccess()) {
                        System.out.println("   ✅ Login successful for existing user: " + loginResult.getUser().getUsername());
                        success.set(true);
                    } else {
                        System.out.println("   ❌ Login failed: " + loginResult.getErrorMessage());
                    }
                    latch.countDown();
                });
            }
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("   ❌ Test timeout");
            return false;
        }

        return success.get();
    }

    /**
     * Test 3: UserModel Register
     */
    private static boolean testUserModelRegister() {
        System.out.println("\n📋 Test 3: UserModel Register");

        UserModel userModel = UserModel.getInstance();
        AtomicBoolean success = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        String uniqueName = "newuser_" + System.currentTimeMillis();

        userModel.register(uniqueName, "newpass123", uniqueName + "@test.com", "New Tester", result -> {
            if (result.isSuccess()) {
                System.out.println("   ✅ Registration successful for: " + result.getUser().getUsername());
                success.set(true);
            } else {
                System.out.println("   ❌ Registration failed: " + result.getErrorMessage());
            }
            latch.countDown();
        });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("   ❌ Test timeout");
            return false;
        }

        return success.get();
    }

    /**
     * Test 4: AppState Management
     */
    private static boolean testAppState() {
        System.out.println("\n📋 Test 4: AppState Management");

        AppState appState = AppState.getInstance();

        // Test login state
        appState.login(new UserDTO(1, "testuser", "test@test.com", "Test User"), "test-token");

        if (!appState.isLoggedIn()) {
            System.out.println("   ❌ Login state not set correctly");
            return false;
        }
        System.out.println("   ✅ Login state working");

        // Test current user
        if (appState.getCurrentUser() == null) {
            System.out.println("   ❌ Current user not set");
            return false;
        }
        System.out.println("   ✅ Current user: " + appState.getCurrentUser().getUsername());

        // Test logout
        appState.logout();
        if (appState.isLoggedIn()) {
            System.out.println("   ❌ Logout failed");
            return false;
        }
        System.out.println("   ✅ Logout working");

        return true;
    }

    /**
     * Test 5: MessageCache
     */
    private static boolean testMessageCache() {
        System.out.println("\n📋 Test 5: MessageCache");

        MessageCache cache = MessageCache.getInstance();

        // Test adding messages
        int channelId = 100;
        cache.addMessage(createTestMessage(1, "Hello world", channelId));
        cache.addMessage(createTestMessage(2, "Second message", channelId));

        // Test retrieving messages
        var messages = cache.getMessages(channelId);
        if (messages.size() != 2) {
            System.out.println("   ❌ Expected 2 messages, got " + messages.size());
            return false;
        }
        System.out.println("   ✅ Messages added and retrieved: " + messages.size());

        // Test unread counts
        cache.resetUnreadCount(channelId);
        int unread = cache.getUnreadCount(channelId);
        if (unread != 0) {
            System.out.println("   ❌ Unread count should be 0, got " + unread);
            return false;
        }
        System.out.println("   ✅ Unread count reset working");

        // Test increment unread
        cache.incrementUnreadCount(channelId);
        unread = cache.getUnreadCount(channelId);
        if (unread != 1) {
            System.out.println("   ❌ Unread count should be 1, got " + unread);
            return false;
        }
        System.out.println("   ✅ Unread count increment working");

        // Test clear channel
        cache.clearChannel(channelId);
        var afterClear = cache.getMessages(channelId);
        if (!afterClear.isEmpty()) {
            System.out.println("   ❌ Channel not cleared");
            return false;
        }
        System.out.println("   ✅ Clear channel working");

        System.out.println("   ✅ MessageCache tests passed!");
        return true;
    }

    private static voxlink.shared.dto.MessageDTO createTestMessage(int id, String content, int channelId) {
        voxlink.shared.dto.MessageDTO msg = new voxlink.shared.dto.MessageDTO();
        msg.setId(id);
        msg.setContent(content);
        msg.setChannelId(channelId);
        msg.setSenderId(1);
        msg.setSenderUsername("testuser");
        msg.setType(voxlink.shared.dto.MessageType.TEXT);
        msg.setSentAt(java.time.LocalDateTime.now());
        return msg;
    }
}