package voxlink.client.src.main.network.voice;

import voxlink.client.src.main.media.AudioManager;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.UserStore;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Handles sending and receiving UDP packets for voice channels.
 */
public class VoiceClient implements Runnable {

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private boolean isRunning;

    private int currentChannelId;
    private String voiceToken;
    private final UserStore userStore;
    private final AudioManager audioManager;

    public VoiceClient() {
        this.userStore = UserStore.getInstance();
        this.audioManager = AudioManager.getInstance();
    }

    public void connect(String serverIp, int port, int channelId, String voiceToken) {
        this.serverPort = port;
        this.currentChannelId = channelId;
        this.voiceToken = voiceToken;

        try {
            this.serverAddress = InetAddress.getByName(serverIp);
            this.socket = new DatagramSocket(); // Bind to any available local port
            this.isRunning = true;

            // Start listening thread
            new Thread(this, "VoiceClient-Listener").start();

            // Send authentication packet
            sendAuthPacket();

            // Wire audio manager to send packets when microphone captures data
            audioManager.setAudioDataListener(this::sendAudioPacket);
            audioManager.startCapture();
            audioManager.startPlayback();

            System.out.println("[VoiceClient] Connected to Voice Server at " + serverIp + ":" + port);

        } catch (Exception e) {
            System.err.println("[VoiceClient] Failed to connect: " + e.getMessage());
        }
    }

    public void disconnect() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        audioManager.stopCapture();
        audioManager.stopPlayback();
        audioManager.setAudioDataListener(null);
        System.out.println("[VoiceClient] Disconnected from Voice Server");
    }

    private void sendAuthPacket() {
        try {
            byte[] tokenBytes = voiceToken.getBytes();
            byte[] payload = new byte[9 + tokenBytes.length];

            // Sender ID
            intToBytes(userStore.getUserId(), payload, 0);
            // Channel ID
            intToBytes(currentChannelId, payload, 4);
            // Type (1 = Auth)
            payload[8] = 1;
            // Token
            System.arraycopy(tokenBytes, 0, payload, 9, tokenBytes.length);

            DatagramPacket packet = new DatagramPacket(payload, payload.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("[VoiceClient] Failed to send auth packet: " + e.getMessage());
        }
    }

    private void sendAudioPacket(byte[] audioData) {
        if (!isRunning || socket == null || socket.isClosed()) return;

        try {
            byte[] payload = new byte[9 + audioData.length];

            // Sender ID
            intToBytes(userStore.getUserId(), payload, 0);
            // Channel ID
            intToBytes(currentChannelId, payload, 4);
            // Type (0 = Audio)
            payload[8] = 0;
            // Audio data
            System.arraycopy(audioData, 0, payload, 9, audioData.length);

            DatagramPacket packet = new DatagramPacket(payload, payload.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            // Ignore standard UDP drop errors
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[2048];

        while (isRunning && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                handleIncomingPacket(packet);
            } catch (SocketException e) {
                // Socket closed, thread will exit
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("[VoiceClient] Error receiving packet: " + e.getMessage());
                }
            }
        }
    }

    private void handleIncomingPacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        int length = packet.getLength();

        if (length < 9) return;

        // int senderId = bytesToInt(data, 0);
        // int channelId = bytesToInt(data, 4);
        byte type = data[8];

        if (type == 0) {
            // It's an audio packet! Play it.
            byte[] audioData = new byte[length - 9];
            System.arraycopy(data, 9, audioData, 0, audioData.length);
            audioManager.playAudio(audioData);
        }
    }

    // --- Helpers ---

    private void intToBytes(int value, byte[] bytes, int offset) {
        bytes[offset] = (byte) (value >> 24);
        bytes[offset + 1] = (byte) (value >> 16);
        bytes[offset + 2] = (byte) (value >> 8);
        bytes[offset + 3] = (byte) value;
    }

    private int bytesToInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
                ((bytes[offset + 1] & 0xFF) << 16) |
                ((bytes[offset + 2] & 0xFF) << 8) |
                (bytes[offset + 3] & 0xFF);
    }
}
