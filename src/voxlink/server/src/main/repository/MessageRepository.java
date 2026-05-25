package voxlink.server.src.main.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import voxlink.server.src.main.database.DBConnection;
import voxlink.server.src.main.model.Message;

public class MessageRepository {

    public boolean createMessage(Message message) {
        String query = "INSERT INTO messages (channel_id, sender_id, receiver_id, content) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            if (message.getChannelId() > 0) {
                pstmt.setInt(1, message.getChannelId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            
            pstmt.setInt(2, message.getSenderId());
            
            if (message.getReceiverId() > 0) {
                pstmt.setInt(3, message.getReceiverId());
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            
            pstmt.setString(4, message.getContent());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        message.setMessageId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error creating message: " + e.getMessage());
        }
        return false;
    }

    public Message getMessageById(int messageId) {
        String query = "SELECT * FROM messages WHERE message_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, messageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessage(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting message by ID: " + e.getMessage());
        }
        return null;
    }

    public List<Message> getMessagesByChannelId(int channelId) {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT * FROM messages WHERE channel_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, channelId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting messages by channel ID: " + e.getMessage());
        }
        return messages;
    }

    public List<Message> getDirectMessages(int user1Id, int user2Id) {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT * FROM messages WHERE " +
                       "(sender_id = ? AND receiver_id = ?) OR " +
                       "(sender_id = ? AND receiver_id = ?) " +
                       "ORDER BY created_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, user1Id);
            pstmt.setInt(2, user2Id);
            pstmt.setInt(3, user2Id);
            pstmt.setInt(4, user1Id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting direct messages: " + e.getMessage());
        }
        return messages;
    }

    public boolean updateMessageContent(int messageId, String newContent) {
        String query = "UPDATE messages SET content = ?, edited = TRUE WHERE message_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, newContent);
            pstmt.setInt(2, messageId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating message: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteMessage(int messageId) {
        String query = "DELETE FROM messages WHERE message_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, messageId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting message: " + e.getMessage());
        }
        return false;
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getInt("message_id"),
            rs.getInt("channel_id"),
            rs.getInt("sender_id"),
            rs.getInt("receiver_id"),
            rs.getString("content"),
            rs.getBoolean("edited"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at")
        );
    }
}
