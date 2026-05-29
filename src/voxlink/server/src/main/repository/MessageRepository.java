package voxlink.server.src.main.repository;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.MessageDTO;
import voxlink.shared.dto.MessageStatus;
import voxlink.shared.dto.MessageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * MessageRepository handles all database operations for Message entities.
 */
public class MessageRepository {

    // Send a new message to a channel
    public MessageDTO sendMessage(String content, int channelId, int senderId,
                                  MessageType type, Integer replyToMessageId) {
        String sql = "INSERT INTO messages (content, channel_id, sender_id, type, reply_to_message_id, sent_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, content);
            stmt.setInt(2, channelId);
            stmt.setInt(3, senderId);
            stmt.setString(4, type.name());
            if (replyToMessageId != null) {
                stmt.setInt(5, replyToMessageId);
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }
            stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int messageId = generatedKeys.getInt(1);
                        System.out.println("[Database] Message sent: " + messageId + " in channel " + channelId);
                        return getMessageById(messageId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to send message: " + e.getMessage());
        }

        return null;
    }

    // Get message by ID
    public MessageDTO getMessageById(int messageId) {
        String sql = "SELECT m.id, m.content, m.channel_id, m.sender_id, m.type, " +
                "m.reply_to_message_id, m.sent_at, m.edited_at, m.deleted_at, m.is_deleted, " +
                "u.username as sender_username, u.display_name as sender_display_name, u.avatar_url as sender_avatar_url, " +
                "c.name as channel_name " +
                "FROM messages m " +
                "INNER JOIN users u ON m.sender_id = u.id " +
                "INNER JOIN channels c ON m.channel_id = c.id " +
                "WHERE m.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessageDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get message by ID: " + e.getMessage());
        }

        return null;
    }

    // Get message history for a channel
    public List<MessageDTO> getMessageHistory(int channelId, int limit, int offset) {
        String sql = "SELECT m.id, m.content, m.channel_id, m.sender_id, m.type, " +
                "m.reply_to_message_id, m.sent_at, m.edited_at, m.deleted_at, m.is_deleted, " +
                "u.username as sender_username, u.display_name as sender_display_name, u.avatar_url as sender_avatar_url, " +
                "c.name as channel_name " +
                "FROM messages m " +
                "INNER JOIN users u ON m.sender_id = u.id " +
                "INNER JOIN channels c ON m.channel_id = c.id " +
                "WHERE m.channel_id = ? AND m.is_deleted = FALSE " +
                "ORDER BY m.sent_at DESC LIMIT ? OFFSET ?";

        List<MessageDTO> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MessageDTO message = mapResultSetToMessageDTO(rs);
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get message history: " + e.getMessage());
        }

        return messages;
    }

    // Get recent channel messages - 50 by default
    public List<MessageDTO> getRecentMessages(int channelId) {
        return getMessageHistory(channelId, 50, 0);
    }

    // Edit message
    public boolean editMessage(int messageId, String newContent) {
        String sql = "UPDATE messages SET content = ?, edited_at = ? WHERE id = ? AND is_deleted = FALSE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newContent);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(3, messageId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("[Database] Message edited: " + messageId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to edit message: " + e.getMessage());
        }

        return false;
    }

    // Soft delete message - mark as read
    public boolean deleteMessage(int messageId) {
        String sql = "UPDATE messages SET is_deleted = TRUE, deleted_at = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, messageId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("[Database] Message deleted: " + messageId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete message: " + e.getMessage());
        }

        return false;
    }

    // Permanently delete message
    public boolean hardDeleteMessage(int messageId) {
        String sql = "DELETE FROM messages WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                System.out.println("[Database] Message hard deleted: " + messageId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to hard delete message: " + e.getMessage());
        }
        return false;
    }

    // Get message by sender
    public List<MessageDTO> getMessagesBySender(int senderId, int limit) {
        String sql = "SELECT m.id, m.content, m.channel_id, m.sender_id, m.type, " +
                "m.reply_to_message_id, m.sent_at, m.edited_at, m.deleted_at, m.is_deleted, " +
                "u.username as sender_username, u.display_name as sender_display_name, u.avatar_url as sender_avatar_url, " +
                "c.name as channel_name " +
                "FROM messages m " +
                "INNER JOIN users u ON m.sender_id = u.id " +
                "INNER JOIN channels c ON m.channel_id = c.id " +
                "WHERE m.sender_id = ? AND m.is_deleted = FALSE " +
                "ORDER BY m.sent_at DESC LIMIT ?";

        List<MessageDTO> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, senderId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessageDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get messages by sender: " + e.getMessage());
        }

        return messages;
    }

    // Search message in the channel
    public List<MessageDTO> searchMessages(int channelId, String searchTerm, int limit) {
        String sql = "SELECT m.id, m.content, m.channel_id, m.sender_id, m.type, " +
                "m.reply_to_message_id, m.sent_at, m.edited_at, m.deleted_at, m.is_deleted, " +
                "u.username as sender_username, u.display_name as sender_display_name, u.avatar_url as sender_avatar_url, " +
                "c.name as channel_name " +
                "FROM messages m " +
                "INNER JOIN users u ON m.sender_id = u.id " +
                "INNER JOIN channels c ON m.channel_id = c.id " +
                "WHERE m.channel_id = ? AND m.is_deleted = FALSE AND m.content LIKE ? " +
                "ORDER BY m.sent_at DESC LIMIT ?";

        List<MessageDTO> messages = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            stmt.setString(2, "%" + searchTerm + "%");
            stmt.setInt(3, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessageDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to search messages: " + e.getMessage());
        }

        return messages;
    }

    // Mark a message as read by user
    public boolean markMessageAsRead(int messageId, int userId) {
        String sql = "INSERT INTO message_read_receipts (message_id, user_id, read_at) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE read_at = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());
            stmt.setInt(1, messageId);
            stmt.setInt(2, userId);
            stmt.setTimestamp(3, now);
            stmt.setTimestamp(4, now);

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to mark message as read: " + e.getMessage());
            return false;
        }
    }

    // Mark all messages in a channel as read
    public boolean markChannelAsRead(int channelId, int userId) {
        String sql = "INSERT INTO message_read_receipts (message_id, user_id, read_at) " +
                "SELECT m.id, ?, ? FROM messages m " +
                "WHERE m.channel_id = ? AND m.is_deleted = FALSE " +
                "ON DUPLICATE KEY UPDATE read_at = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, now);
            stmt.setInt(3, channelId);
            stmt.setTimestamp(4, now);

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to mark channel as read: " + e.getMessage());
            return false;
        }
    }

    // Get users who have read specific message
    public List<Integer> getUsersWhoReadMessage(int messageId) {
        String sql = "SELECT user_id FROM message_read_receipts WHERE message_id = ?";

        List<Integer> userIds = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getInt("user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get users who read message: " + e.getMessage());
        }
        return userIds;
    }

    // Count unread messages for a user in the channel
    public int countUnreadMessages(int channelId, int userId) {
        String sql = "SELECT COUNT(*) FROM messages m " +
                "LEFT JOIN message_read_receipts mrr ON m.id = mrr.message_id AND mrr.user_id = ? " +
                "WHERE m.channel_id = ? AND m.is_deleted = FALSE AND mrr.message_id IS NULL";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, channelId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB Error] Failed to count unread messages: " + e.getMessage());
        }

        return 0;
    }

    // Delete all messages in the channel
    public int deleteAllMessagesInChannel(int channelId) {
        String sql = "DELETE FROM messages WHERE channel_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            int deleted = stmt.executeUpdate();
            System.out.println("[Database] Deleted " + deleted + " messages from channel " + channelId);
            return deleted;

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete all messages in channel: " + e.getMessage());
            return 0;
        }
    }

    // Map ResultSet row to MessageDTO object
    private MessageDTO mapResultSetToMessageDTO(ResultSet rs) throws SQLException {
        MessageDTO message = new MessageDTO();
        message.setId(rs.getInt("id"));
        message.setContent(rs.getString("content"));
        message.setChannelId(rs.getInt("channel_id"));
        message.setSenderId(rs.getInt("sender_id"));
        message.setSenderUsername(rs.getString("sender_username"));
        message.setSenderDisplayName(rs.getString("sender_display_name"));
        message.setSenderAvatarUrl(rs.getString("sender_avatar_url"));
        message.setChannelName(rs.getString("channel_name"));

        String typeStr = rs.getString("type");
        if (typeStr != null) {
            try {
                message.setType(MessageType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                message.setType(MessageType.TEXT);
            }
        }

        int replyToId = rs.getInt("reply_to_message_id");
        if (!rs.wasNull() && replyToId > 0) {
            message.setReplyToMessageId(replyToId);
        }

        Timestamp sentAt = rs.getTimestamp("sent_at");
        if (sentAt != null) {
            message.setSentAt(sentAt.toLocalDateTime());
        }

        Timestamp editedAt = rs.getTimestamp("edited_at");
        if (editedAt != null) {
            message.setEditedAt(editedAt.toLocalDateTime());
        }

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) {
            message.setDeletedAt(deletedAt.toLocalDateTime());
        }

        boolean isDeleted = rs.getBoolean("is_deleted");
        if (isDeleted) {
            message.setStatus(MessageStatus.DELETED);
        } else if (editedAt != null) {
            message.setStatus(MessageStatus.EDITED);
        } else {
            message.setStatus(MessageStatus.SENT);
        }

        return message;
    }
}