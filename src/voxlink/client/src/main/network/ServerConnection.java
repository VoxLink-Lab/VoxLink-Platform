package voxlink.client.src.main.network;

 import voxlink.client.src.main.state.AppState;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ServerConnection manages the TCP socket connection to the VoxLink server.
 */
public class ServerConnection {

    private static ServerConnection instance;

    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    private MessageReceiver messageReceiver;
    private MessageSender messageSender;

    private final AtomicBoolean isConnected;
    private final BlockingQueue<Packet> outgoingQueue;

    private String serverHost;
    private int serverPort;

    // Private constructor for singleton pattern
    private ServerConnection() {
        this.isConnected = new AtomicBoolean(false);
        this.outgoingQueue = new LinkedBlockingQueue<>();
    }

    // Get singleton instance
    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    // Connect to the server
    public boolean connect(String host, int port) {
        if (isConnected.get()) {
            System.out.println("[Network] Already connected to server");
            return true;
        }

        try {
            this.serverHost = host;
            this.serverPort = port;

            // Create socket and streams
            socket = new Socket(host, port);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);

            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());

            // Start sender and receiver threads
            messageSender = new MessageSender(outputStream, outgoingQueue);
            messageReceiver = new MessageReceiver(inputStream);

            messageSender.start();
            messageReceiver.start();

            isConnected.set(true);

            System.out.println("[Network] Connected to server at " + host + ":" + port);
            return true;

        } catch (IOException e) {
            System.err.println("[Network] Failed to connect: " + e.getMessage());
            return false;
        }
    }

    // Disconnect from the server
    public void disconnect() {
        if (!isConnected.get()) {
            return;
        }

        isConnected.set(false);

        // Stop sender and receiver threads
        if (messageSender != null) {
            messageSender.stop();
        }
        if (messageReceiver != null) {
            messageReceiver.stop();
        }

        // Send logout packet if authenticated
        if (AppState.getInstance().isLoggedIn()) {
            Packet logoutPacket = new Packet(RequestType.AUTH_LOGOUT);
            sendPacket(logoutPacket);
        }

        // Close streams and socket
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[Network] Error closing connection: " + e.getMessage());
        }

        System.out.println("[Network] Disconnected from server");
    }

    // Send a packet to the server (adds to queue, non-blocking)
    public void sendPacket(Packet packet) {
        if (!isConnected.get()) {
            System.err.println("[Network] Not connected to server");
            return;
        }

        try {
            outgoingQueue.put(packet);
        } catch (InterruptedException e) {
            System.err.println("[Network] Failed to queue packet: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    // Send a packet and wait for response (blocking)
    public Packet sendAndWait(Packet packet) {
        if (!isConnected.get()) {
            System.err.println("[Network] Not connected to server");
            return null;
        }

        // Add request ID tracking
        String requestId = packet.getPacketId();

        // Send packet
        sendPacket(packet);

        // Wait for response
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Return last received response (simplified)
        return messageReceiver.getLastResponse(requestId);
    }

    // Check if connected to the server
    public boolean isConnected() {
        return isConnected.get() && socket != null && !socket.isClosed();
    }

    // Get server host
    public String getServerHost() {
        return serverHost;
    }

    // Get server port
    public int getServerPort() {
        return serverPort;
    }

    // Register a packet listener for incoming packets
    public void addPacketListener(PacketListener listener) {
        if (messageReceiver != null) {
            messageReceiver.addListener(listener);
        }
    }

    // Remove a packet listener
    public void removePacketListener(PacketListener listener) {
        if (messageReceiver != null) {
            messageReceiver.removeListener(listener);
        }
    }

   // Packet listener interface for incoming packet callbacks
    public interface PacketListener {
        void onPacketReceived(Packet packet);
    }
}