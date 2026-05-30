package voxlink.client.src.main.network;

import voxlink.shared.protocol.Packet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MessageSender runs in a background thread and processes outgoing packets
 * from a queue, sending them to the server asynchronously.
 */
public class MessageSender extends Thread {

    private final ObjectOutputStream outputStream;
    private final BlockingQueue<Packet> packetQueue;
    private final AtomicBoolean isRunning;
    private final AtomicBoolean isPaused;

    public MessageSender(ObjectOutputStream outputStream, BlockingQueue<Packet> packetQueue) {
        this.outputStream = outputStream;
        this.packetQueue = packetQueue;
        this.isRunning = new AtomicBoolean(true);
        this.isPaused = new AtomicBoolean(false);
        this.setDaemon(true);
        this.setName("MessageSender-Thread");
    }

    @Override
    public void run() {
        System.out.println("[MessageSender] Started sending messages");

        while (isRunning.get()) {
            try {
                // Check if paused
                if (isPaused.get()) {
                    Thread.sleep(100);
                    continue;
                }

                // Take next packet from queue (blocks if empty)
                Packet packet = packetQueue.take();

                // Send the packet
                sendPacket(packet);

            } catch (InterruptedException e) {
                if (isRunning.get()) {
                    System.err.println("[MessageSender] Interrupted: " + e.getMessage());
                }
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[MessageSender] Stopped");
    }

    // Send a single packet to the server
    private void sendPacket(Packet packet) {
        try {
            outputStream.writeObject(packet);
            outputStream.flush();

            // Log for debugging (optional)
            if (packet.getRequestType() != null) {
                System.out.println("[MessageSender] Sent: " + packet.getRequestType());
            }

        } catch (IOException e) {
            System.err.println("[MessageSender] Failed to send packet: " + e.getMessage());

            // If connection is broken, stop the sender
            if (e.getMessage().contains("Broken pipe") ||
                    e.getMessage().contains("Connection reset")) {
                stopSender();
            }
        }
    }

    // Add a packet to the send queue (non-blocking)
    public boolean queuePacket(Packet packet) {
        if (!isRunning.get()) {
            System.err.println("[MessageSender] Cannot queue packet - sender is stopped");
            return false;
        }

        return packetQueue.offer(packet);
    }

    // Add a packet to the send queue and wait for it to be sent (blocking)
    public boolean queuePacketAndWait(Packet packet, long timeoutMs) {
        if (!isRunning.get()) {
            return false;
        }

        long startTime = System.currentTimeMillis();

        // Add to queue
        packetQueue.offer(packet);

        // Wait for queue to empty (packet sent)
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (!packetQueue.contains(packet)) {
                return true;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    // Get current queue size
    public int getQueueSize() {
        return packetQueue.size();
    }

    // Clear all pending packets from queue
    public void clearQueue() {
        packetQueue.clear();
    }

    // Pause sending (useful during reconnect)
    public void pause() {
        isPaused.set(true);
        System.out.println("[MessageSender] Paused");
    }

    // Resume sending
    public void resumeSending() {
        isPaused.set(false);
        System.out.println("[MessageSender] Resumed");
    }

    // Stop the sender thread
    public void stopSender() {
        isRunning.set(false);
        this.interrupt();
    }

    // Check if sender is running
    public boolean isRunning() {
        return isRunning.get();
    }

    // Check if sender is paused
    public boolean isPaused() {
        return isPaused.get();
    }

    // Flush the output stream
    public void flush() {
        try {
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("[MessageSender] Failed to flush: " + e.getMessage());
        }
    }
}