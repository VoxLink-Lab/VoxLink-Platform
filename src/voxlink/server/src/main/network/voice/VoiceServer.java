package voxlink.server.src.main.network.voice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Set;

/**
 * Handles incoming UDP audio packets and broadcasts them to other users in the same channel.
 */
public class VoiceServer implements Runnable {

    public static final int VOICE_PORT = 8081;
    private static final int BUFFER_SIZE = 1024;

    private DatagramSocket socket;
    private boolean isRunning;
    private final VoiceSessionManager sessionManager;

    public VoiceServer() {
        this.sessionManager = VoiceSessionManager.getInstance();
    }

    public void start() {
        try {
            socket = new DatagramSocket(VOICE_PORT);
            isRunning = true;
            System.out.println("[VoiceServer] UDP Voice Server started on port " + VOICE_PORT);
            new Thread(this, "VoiceServer-Thread").start();
        } catch (IOException e) {
            System.err.println("[VoiceServer] Failed to start UDP server: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (isRunning && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                handleIncomingPacket(packet);
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("[VoiceServer] Error receiving UDP packet: " + e.getMessage());
                }
            }
        }
    }

    private void handleIncomingPacket(DatagramPacket packet) {
        // Simple packet structure:
        // [0-3] : senderId (int)
        // [4-7] : channelId (int)
        // [8]   : type (0 = Audio, 1 = Auth)
        // [9+]  : payload (audio bytes or auth token)

        byte[] data = packet.getData();
        int length = packet.getLength();

        if (length < 9) return;

        int senderId = bytesToInt(data, 0);
        int channelId = bytesToInt(data, 4);
        byte type = data[8];

        InetSocketAddress senderAddress = (InetSocketAddress) packet.getSocketAddress();

        if (type == 1) {
            // Auth Packet
            String token = new String(data, 9, length - 9).trim();
            if (sessionManager.authenticateEndpoint(token, senderAddress)) {
                System.out.println("[VoiceServer] Authenticated UDP endpoint for user " + senderId);
            } else {
                System.out.println("[VoiceServer] Failed to authenticate UDP endpoint for user " + senderId);
            }
            return;
        }

        if (type == 0) {
            // Audio Packet
            // We only broadcast if the sender is authenticated and the endpoint matches
            InetSocketAddress expectedAddress = sessionManager.getUserEndpoint(senderId);
            if (expectedAddress != null && expectedAddress.equals(senderAddress)) {
                broadcastAudio(senderId, channelId, packet);
            }
        }
    }

    private void broadcastAudio(int senderId, int channelId, DatagramPacket packet) {
        Set<Integer> members = sessionManager.getChannelMembers(channelId);
        if (members == null) return;

        for (int memberId : members) {
            if (memberId != senderId) {
                InetSocketAddress memberEndpoint = sessionManager.getUserEndpoint(memberId);
                if (memberEndpoint != null) {
                    try {
                        DatagramPacket outboundPacket = new DatagramPacket(
                                packet.getData(),
                                packet.getOffset(),
                                packet.getLength(),
                                memberEndpoint.getAddress(),
                                memberEndpoint.getPort()
                        );
                        socket.send(outboundPacket);
                    } catch (IOException e) {
                        // Ignore standard UDP drop errors
                    }
                }
            }
        }
    }

    // Helper methods to read/write ints to bytes
    private int bytesToInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
                ((bytes[offset + 1] & 0xFF) << 16) |
                ((bytes[offset + 2] & 0xFF) << 8) |
                (bytes[offset + 3] & 0xFF);
    }
}
