package voxlink.client.src.main.model;

import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.MessageCache;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.ChannelDTO;
import voxlink.shared.dto.MessageDTO;
import voxlink.shared.dto.MessageType;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * MessageModel handles client-side business logic for message operations.
 */
public class MessageModel {

    private static MessageModel instance;
    private final ServerConnection connection;
    private final UserStore userStore;
    private final AppState appState;
    private final MessageCache messageCache;

    // Typing indicator management
    private final ConcurrentHashMap<Integer, Timer> typingTimers;
    private static final int TYPING_DEBOUNCE_MS = 2000;
    private static final int TYPING_BROADCAST_INTERVAL_MS = 3000;

    private int currentTypingChannel = -1;
    private Timer typingBroadcastTimer;
    private boolean isTyping = false;

    // Private constructor for singleton pattern
    private MessageModel() {
        this.connection = ServerConnection.getInstance();
        this.userStore = UserStore.getInstance();
        this.appState = AppState.getInstance();
        this.messageCache = MessageCache.getInstance();
        this.typingTimers = new ConcurrentHashMap<>();
    }

    // Get singleton instance
    public static synchronized MessageModel getInstance() {
        if (instance == null) {
            instance = new MessageModel();
        }
        return instance;
    }

    // Send a message to the current channel
    public void sendMessage(String content, Consumer<SendMessageResult> callback) {
        ChannelModel channelModel = ChannelModel.getInstance();
        int currentChannelId = channelModel.getCurrentChannel() != null ?
                channelModel.getCurrentChannel().getId() : -1;

        sendMessage(content, currentChannelId, null, callback);
    }

    // Send a message to a specific channel
    public void sendMessage(String content, int channelId, Integer replyToMessageId,
                            Consumer<SendMessageResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new SendMessageResult(false, null, "Not authenticated"));
            }
            return;
        }

        if (content == null || content.trim().isEmpty()) {
            if (callback != null) {
                callback.accept(new SendMessageResult(false, null, "Message cannot be empty"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.MESSAGE_SEND);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("channelId", channelId);
        packet.put("content", content);
        if (replyToMessageId != null) {
            packet.put("replyToMessageId", replyToMessageId);
        }

        // Create temporary message for optimistic UI update
        MessageDTO tempMessage = createTempMessage(content, channelId);
        messageCache.addMessage(tempMessage);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.MESSAGE_SEND_SUCCESS) {
                    MessageDTO message = (MessageDTO) response.get("message");

                    if (message != null) {
                        // Replace temp message with real one
                        messageCache.addMessage(message);
                        if (callback != null) {
                            callback.accept(new SendMessageResult(true, message, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new SendMessageResult(false, null, "Invalid response"));
                        }
                    }
                } else {
                    // Remove temp message on failure
                    messageCache.clearChannel(channelId);
                    if (callback != null) {
                        callback.accept(new SendMessageResult(false, null, response.getErrorMessage()));
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Edit a message
    public void editMessage(int messageId, int channelId, String newContent,
                            Consumer<Boolean> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) callback.accept(false);
            return;
        }

        Packet packet = new Packet(RequestType.MESSAGE_EDIT);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("messageId", messageId);
        packet.put("content", newContent);

        // Optimistic update
        messageCache.updateMessage(messageId, channelId, newContent, LocalDateTime.now());

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                boolean success = response.getResponseType() == ResponseType.MESSAGE_EDIT_SUCCESS;
                if (!success) {
                    // Revert on failure - would need to fetch original message
                }
                if (callback != null) callback.accept(success);
                connection.removePacketListener(this);
            }
        });
    }

    // Delete a message
    public void deleteMessage(int messageId, int channelId, Consumer<Boolean> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) callback.accept(false);
            return;
        }

        Packet packet = new Packet(RequestType.MESSAGE_DELETE);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("messageId", messageId);

        // Optimistic update
        messageCache.markMessageDeleted(messageId, channelId, LocalDateTime.now());

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                boolean success = response.getResponseType() == ResponseType.MESSAGE_DELETE_SUCCESS;
                if (!success) {
                    // Revert on failure - would need to restore message
                }
                if (callback != null) callback.accept(success);
                connection.removePacketListener(this);
            }
        });
    }

    // Send typing indicator
    public void sendTypingIndicator(int channelId, boolean isTyping) {
        if (!userStore.isAuthenticated()) {
            return;
        }

        Packet packet = new Packet(RequestType.MESSAGE_TYPING);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("channelId", channelId);
        packet.put("isTyping", isTyping);

        connection.sendPacket(packet);
    }

    // Start typing in channel
    public void startTyping(int channelId) {
        // Cancel previous timer for this channel
        Timer existingTimer = typingTimers.remove(channelId);
        if (existingTimer != null) {
            existingTimer.cancel();
        }

        // Send typing started if not already typing in this channel
        if (currentTypingChannel != channelId || !isTyping) {
            currentTypingChannel = channelId;
            isTyping = true;
            sendTypingIndicator(channelId, true);
        }

        // Set timer to stop typing after debounce period
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopTyping(channelId);
                typingTimers.remove(channelId);
            }
        }, TYPING_DEBOUNCE_MS);
        typingTimers.put(channelId, timer);
    }

    // Stop typing in channel
    public void stopTyping(int channelId) {
        if (currentTypingChannel == channelId && isTyping) {
            isTyping = false;
            sendTypingIndicator(channelId, false);
            currentTypingChannel = -1;
        }
    }

    // Get message for a channel
    public List<MessageDTO> getMessages(int channelId) {
        return messageCache.getMessages(channelId);
    }

    // Get recent message for a channel
    public List<MessageDTO> getRecentMessages(int channelId, int count) {
        return messageCache.getRecentMessages(channelId, count);
    }

    // Get a single message
    public MessageDTO getMessage(int channelId, int messageId) {
        return messageCache.getMessage(channelId, messageId);
    }

    // Create a temporary message for optimistic UI update
    private MessageDTO createTempMessage(String content, int channelId) {
        MessageDTO message = new MessageDTO();
        message.setId((int) -System.currentTimeMillis());
        message.setContent(content);
        message.setChannelId(channelId);
        message.setSenderId(userStore.getUserId());
        message.setSenderUsername(userStore.getUsername());
        message.setSenderDisplayName(userStore.getDisplayName());
        message.setType(MessageType.TEXT);
        message.setSentAt(LocalDateTime.now());
        return message;
    }

    // Handle new message broadcast from server
    public void handleNewMessageBroadcast(Packet packet) {
        MessageDTO message = (MessageDTO) packet.get("message");
        if (message != null) {
            // Check if message is from current user (already added via optimistic update)
            if (message.getSenderId() != userStore.getUserId()) {
                messageCache.addMessage(message);

                // Increment unread count if not in current channel
                ChannelDTO currentChannel = ChannelModel.getInstance().getCurrentChannel();
                if (currentChannel == null || currentChannel.getId() != message.getChannelId()) {
                    messageCache.incrementUnreadCount(message.getChannelId());
                }
            }
        }
    }

    // Handle typing indicator broadcast from server
    public void handleTypingBroadcast(Packet packet) {
        int userId = (Integer) packet.get("userId");
        String username = packet.get("username").toString();
        int channelId = (Integer) packet.get("channelId");
        boolean isTyping = (Boolean) packet.get("isTyping");

        // Don't show own typing indicator
        if (userId == userStore.getUserId()) {
            return;
        }

        // Check if in the same channel
        ChannelDTO currentChannel = ChannelModel.getInstance().getCurrentChannel();
        if (currentChannel != null && currentChannel.getId() == channelId) {
            // Notify UI through AppState or callback
            System.out.println("[MessageModel] " + username + " is " +
                    (isTyping ? "typing..." : "stopped typing"));
        }
    }

    // Clear message for a channel
    public void clearChannelMessages(int channelId) {
        messageCache.clearChannel(channelId);
    }

    // Clear all cached messages
    public void clearAllMessages() {
        messageCache.clearAll();
    }

    // Get cache statistics
    public String getCacheStats() {
        return messageCache.getStats();
    }

    // --- RESULT CLASS ---

    public static class SendMessageResult {
        private final boolean success;
        private final MessageDTO message;
        private final String errorMessage;

        public SendMessageResult(boolean success, MessageDTO message, String errorMessage) {
            this.success = success;
            this.message = message;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public MessageDTO getMessage() { return message; }
        public String getErrorMessage() { return errorMessage; }
    }
}