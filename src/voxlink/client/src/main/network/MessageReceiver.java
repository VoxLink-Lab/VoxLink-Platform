package voxlink.client.src.main.network;

import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.ResponseType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MessageReceiver runs in a background thread and listens for incoming
 * packets from the server. Notifies all registered listeners when a packet arrives.
 */
public class MessageReceiver extends Thread {

    private final ObjectInputStream inputStream;
    private final List<ServerConnection.PacketListener> listeners;
    private final ConcurrentMap<String, Packet> pendingResponses;
    private volatile boolean isRunning;
    private Packet lastReceivedPacket;

    public MessageReceiver(ObjectInputStream inputStream) {
        this.inputStream = inputStream;
        this.listeners = new CopyOnWriteArrayList<>();
        this.pendingResponses = new ConcurrentHashMap<>();
        this.isRunning = true;
        this.setDaemon(true);
        this.setName("MessageReceiver-Thread");
    }

    @Override
    public void run() {
        System.out.println("[MessageReceiver] Started listening for server messages");

        while (isRunning) {
            try {
                // Read packet from server
                Packet packet = (Packet) inputStream.readObject();

                if (packet == null) {
                    continue;
                }

                lastReceivedPacket = packet;

                // Handle the packet
                handlePacket(packet);

            } catch (SocketException e) {
                if (isRunning) {
                    System.err.println("[MessageReceiver] Connection lost: " + e.getMessage());
                }
                break;
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("[MessageReceiver] I/O Error: " + e.getMessage());
                }
                break;
            } catch (ClassNotFoundException e) {
                System.err.println("[MessageReceiver] Unknown packet type: " + e.getMessage());
            }
        }

        System.out.println("[MessageReceiver] Stopped");
    }

    // Handle an incoming packet
    private void handlePacket(Packet packet) {
        // Check if this is a response to a pending request
        String packetId = packet.getPacketId();
        if (packetId != null && pendingResponses.containsKey(packetId)) {
            pendingResponses.put(packetId, packet);
        }

        // Handle different response types
        ResponseType responseType = packet.getResponseType();

        if (responseType != null) {
            switch (responseType) {
                case HEARTBEAT_PONG:
                    // Heartbeat response - ignore
                    break;

                case MESSAGE_BROADCAST:
                    handleNewMessage(packet);
                    break;

                case MESSAGE_TYPING_BROADCAST:
                    handleTypingIndicator(packet);
                    break;

                case USER_PRESENCE_BROADCAST:
                    handleUserPresence(packet);
                    break;

                case WORKSPACE_BROADCAST_UPDATE:
                    handleWorkspaceUpdate(packet);
                    break;

                case CHANNEL_BROADCAST_UPDATE:
                    handleChannelUpdate(packet);
                    break;

                case CONNECTION_CLOSED:
                    handleConnectionClosed(packet);
                    break;

                default:
                    break;
            }
        }

        // Notify all listeners
        notifyListeners(packet);
    }

    // Handle new message broadcast from server
    private void handleNewMessage(Packet packet) {
        System.out.println("[MessageReceiver] New message received");
        // TODO: Update UI with new message via AppState
    }

    // Handle typing indicator broadcast
    private void handleTypingIndicator(Packet packet) {
        String username = packet.get("username").toString();
        int channelId = (Integer) packet.get("channelId");
        boolean isTyping = (Boolean) packet.get("isTyping");

        // TODO: Update UI to show typing indicator
        System.out.println("[MessageReceiver] " + username + " is typing in channel " + channelId);
    }

    // Handle user presence change (online/offline)
    private void handleUserPresence(Packet packet) {
        int userId = (Integer) packet.get("userId");
        String username = packet.get("username").toString();
        String status = packet.get("status").toString();

        // TODO: Update UI user list
        System.out.println("[MessageReceiver] User " + username + " is now " + status);
    }

    // Handle workspace update (new channel, channel deleted, etc.)
    private void handleWorkspaceUpdate(Packet packet) {
        int workspaceId = (Integer) packet.get("workspaceId");
        String action = packet.get("action").toString();

        // TODO: Refresh workspace UI
        System.out.println("[MessageReceiver] Workspace " + workspaceId + " updated: " + action);
    }

    // Handle channel update
    private void handleChannelUpdate(Packet packet) {
        int channelId = (Integer) packet.get("channelId");
        String action = packet.get("action").toString();

        // TODO: Refresh channel UI
        System.out.println("[MessageReceiver] Channel " + channelId + " updated: " + action);
    }

    // Handle connection closed by server
    private void handleConnectionClosed(Packet packet) {
        System.err.println("[MessageReceiver] Server closed the connection");
        isRunning = false;
    }

    // Notify all registered listeners about a packet
    private void notifyListeners(Packet packet) {
        synchronized (listeners) {
            for (ServerConnection.PacketListener listener : listeners) {
                try {
                    listener.onPacketReceived(packet);
                } catch (Exception e) {
                    System.err.println("[MessageReceiver] Listener error: " + e.getMessage());
                }
            }
        }
    }

    // Add a packet listener
    public void addListener(ServerConnection.PacketListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    // Remove a packet listener
    public void removeListener(ServerConnection.PacketListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    // Stop the receiver thread
    public void stopReceiver() {
        isRunning = false;
        this.interrupt();
    }

    // Get the last received packet (for simple request-response)
    public Packet getLastResponse(String requestId) {
        return pendingResponses.remove(requestId);
    }

    // Wait for a specific response type
    public Packet waitForResponse(String requestId, long timeoutMs) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            Packet response = pendingResponses.remove(requestId);
            if (response != null) {
                return response;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return null;
    }

    // Check if receiver is running
    public boolean isRunning() {
        return isRunning;
    }
}