package voxlink.client.src.main.state;

import voxlink.shared.dto.MessageDTO;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MessageCache provides local caching of messages to reduce network calls.
 */
public class MessageCache {

    private static MessageCache instance;

    // Cache structure: channelId -> List of messages
    private final Map<Integer, List<MessageDTO>> channelMessages;
    private final Map<Integer, Integer> channelUnreadCounts;
    private final Map<Integer, LocalDateTime> channelLastReadTime;

    private int maxMessagesPerChannel;
    private int maxTotalMessages;

    // Private constructor for singleton pattern
    private MessageCache() {
        this.channelMessages = new ConcurrentHashMap<>();
        this.channelUnreadCounts = new ConcurrentHashMap<>();
        this.channelLastReadTime = new ConcurrentHashMap<>();
        this.maxMessagesPerChannel = 200;
        this.maxTotalMessages = 5000;
    }

    // Get singleton instance
    public static synchronized MessageCache getInstance() {
        if (instance == null) {
            instance = new MessageCache();
        }
        return instance;
    }

    // Configure cache limits
    public void configure(int maxPerChannel, int maxTotal) {
        this.maxMessagesPerChannel = maxPerChannel;
        this.maxTotalMessages = maxTotal;
    }

    // Add a message to the cache
    public void addMessage(MessageDTO message) {
        if (message == null || message.isDeleted()) {
            return;
        }

        int channelId = message.getChannelId();

        List<MessageDTO> messages = channelMessages.computeIfAbsent(channelId,
                k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (messages) {
            // Check if message already exists
            boolean exists = messages.stream().anyMatch(m -> m.getId() == message.getId());
            if (exists) {
                return;
            }

            messages.add(message);

            // Sort by timestamp (oldest first)
            messages.sort(Comparator.comparing(MessageDTO::getSentAt));

            // Limit per channel
            while (messages.size() > maxMessagesPerChannel) {
                messages.removeFirst();
            }
        }

        // Check total cache size
        enforceTotalLimit();
    }

    // Add multiple messages to the cache
    public void addMessages(List<MessageDTO> messages, int channelId) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<MessageDTO> channelMsgList = channelMessages.computeIfAbsent(channelId,
                k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (channelMsgList) {
            for (MessageDTO message : messages) {
                if (message.isDeleted()) {
                    continue;
                }

                boolean exists = channelMsgList.stream()
                        .anyMatch(m -> m.getId() == message.getId());
                if (!exists) {
                    channelMsgList.add(message);
                }
            }

            // Sort by timestamp
            channelMsgList.sort(Comparator.comparing(MessageDTO::getSentAt));

            // Limit per channel
            while (channelMsgList.size() > maxMessagesPerChannel) {
                channelMsgList.removeFirst();
            }
        }

        enforceTotalLimit();
    }

    // Get messages for a channel
    public List<MessageDTO> getMessages(int channelId) {
        List<MessageDTO> messages = channelMessages.get(channelId);
        if (messages == null) {
            return new ArrayList<>();
        }

        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    // Get messages for a channel with pagination
    public List<MessageDTO> getMessages(int channelId, int limit, int offset) {
        List<MessageDTO> messages = getMessages(channelId);
        if (messages.isEmpty()) {
            return new ArrayList<>();
        }

        int total = messages.size();
        int start = Math.max(0, total - offset - limit);
        int end = total - offset;

        if (start >= end) {
            return new ArrayList<>();
        }

        return new ArrayList<>(messages.subList(start, Math.min(end, total)));
    }

    // Get recent messages (last N)
    public List<MessageDTO> getRecentMessages(int channelId, int count) {
        List<MessageDTO> messages = getMessages(channelId);
        if (messages.isEmpty()) {
            return new ArrayList<>();
        }

        int size = messages.size();
        int start = Math.max(0, size - count);
        return new ArrayList<>(messages.subList(start, size));
    }

    // Get a single message by ID
    public MessageDTO getMessage(int channelId, int messageId) {
        List<MessageDTO> messages = channelMessages.get(channelId);
        if (messages == null) {
            return null;
        }

        synchronized (messages) {
            return messages.stream()
                    .filter(m -> m.getId() == messageId)
                    .findFirst()
                    .orElse(null);
        }
    }

    // Update a message (after edit)
    public void updateMessage(int messageId, int channelId, String newContent, LocalDateTime editedAt) {
        List<MessageDTO> messages = channelMessages.get(channelId);
        if (messages == null) {
            return;
        }

        synchronized (messages) {
            for (MessageDTO message : messages) {
                if (message.getId() == messageId) {
                    message.setContent(newContent);
                    message.setEditedAt(editedAt);
                    break;
                }
            }
        }
    }

    // Mark a message as deleted
    public void markMessageDeleted(int messageId, int channelId, LocalDateTime deletedAt) {
        List<MessageDTO> messages = channelMessages.get(channelId);
        if (messages == null) {
            return;
        }

        synchronized (messages) {
            for (MessageDTO message : messages) {
                if (message.getId() == messageId) {
                    message.setDeletedAt(deletedAt);
                    message.setStatus(voxlink.shared.dto.MessageStatus.DELETED);
                    break;
                }
            }
        }
    }

    // Clear messages for a specific channel
    public void clearChannel(int channelId) {
        channelMessages.remove(channelId);
        channelUnreadCounts.remove(channelId);
        channelLastReadTime.remove(channelId);
    }

    // Clear all cached messages
    public void clearAll() {
        channelMessages.clear();
        channelUnreadCounts.clear();
        channelLastReadTime.clear();
    }

    // --- UNREAD COUNT MANAGEMENT ---

    // Increment unread count for a channel
    public int incrementUnreadCount(int channelId) {
        return channelUnreadCounts.merge(channelId, 1, Integer::sum);
    }

    // Reset unread count for a channel (user read messages)
    public void resetUnreadCount(int channelId) {
        channelUnreadCounts.put(channelId, 0);
        channelLastReadTime.put(channelId, LocalDateTime.now());
    }

    // Get unread count for a channel
    public int getUnreadCount(int channelId) {
        return channelUnreadCounts.getOrDefault(channelId, 0);
    }

    // Get total unread count across all channels
    public int getTotalUnreadCount() {
        return channelUnreadCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    // Get last read time for a channel
    public LocalDateTime getLastReadTime(int channelId) {
        return channelLastReadTime.get(channelId);
    }

    // Check if a message is unread
    public boolean isMessageUnread(int channelId, LocalDateTime messageSentAt) {
        LocalDateTime lastRead = channelLastReadTime.get(channelId);
        if (lastRead == null) {
            return true;
        }
        return messageSentAt.isAfter(lastRead);
    }

    // --- CACHE MANAGEMENT ---

    // Enforce total cache size limit
    private void enforceTotalLimit() {
        int totalSize = channelMessages.values().stream()
                .mapToInt(List::size)
                .sum();

        if (totalSize <= maxTotalMessages) {
            return;
        }

        // Remove oldest messages from oldest channels
        int toRemove = totalSize - maxTotalMessages;
        int removed = 0;

        // Sort channels by last activity (oldest first)
        List<Map.Entry<Integer, List<MessageDTO>>> entries = new ArrayList<>(channelMessages.entrySet());

        for (Map.Entry<Integer, List<MessageDTO>> entry : entries) {
            List<MessageDTO> messages = entry.getValue();
            synchronized (messages) {
                while (!messages.isEmpty() && removed < toRemove) {
                    messages.removeFirst();
                    removed++;
                }
                if (messages.isEmpty()) {
                    channelMessages.remove(entry.getKey());
                }
            }
            if (removed >= toRemove) {
                break;
            }
        }
    }

    // Get cache statistics
    public String getStats() {
        int totalMessages = channelMessages.values().stream()
                .mapToInt(List::size)
                .sum();

        return String.format("MessageCache - Channels: %d, Total Messages: %d, Total Unread: %d",
                channelMessages.size(), totalMessages, getTotalUnreadCount());
    }

    // Check if channel has cached messages
    public boolean hasChannelCache(int channelId) {
        List<MessageDTO> messages = channelMessages.get(channelId);
        return messages != null && !messages.isEmpty();
    }

    // Get oldest message timestamp in a channel
    public LocalDateTime getOldestMessageTime(int channelId) {
        List<MessageDTO> messages = channelMessages.get(channelId);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        synchronized (messages) {
            return messages.getFirst().getSentAt();
        }
    }

    // Get newest message timestamp in a channel
    public LocalDateTime getNewestMessageTime(int channelId) {
        List<MessageDTO> messages = channelMessages.get(channelId);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        synchronized (messages) {
            return messages.get(messages.size() - 1).getSentAt();
        }
    }
}