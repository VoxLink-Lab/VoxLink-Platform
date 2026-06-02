package voxlink.server.src.main.repository;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.ChannelDTO;
import voxlink.shared.dto.ChannelType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all database operations for Channel entities.
 */
public class ChannelRepository {

    // Create a new channel in the workspace
    public ChannelDTO createChannel(String name, String description, int workspaceId,
                                    ChannelType type, boolean isPrivate, int createdBy) {
        String sql = "INSERT INTO channels (name, description, workspace_id, type, is_private, created_by, created_at, last_activity_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, workspaceId);
            stmt.setString(4, type.name());
            stmt.setBoolean(5, isPrivate);
            stmt.setInt(6, createdBy);
            stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            stmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int channelId = generatedKeys.getInt(1);
                        System.out.println("[Database] Created channel: " + name + " (ID: " + channelId + ") in workspace " + workspaceId);
                        return getChannelById(channelId);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to create channel: " + e.getMessage());
            if (e.getMessage().contains("Duplicate entry")) {
                System.err.println("Channel name already exists in this workspace: " + name);
            }
        }

        return null;
    }

    // Get channel by ID
    public ChannelDTO getChannelById(int channelId) {
        String sql = "SELECT c.id, c.name, c.description, c.workspace_id, c.type, c.is_private, " +
                "c.is_archived, c.created_by, c.created_at, c.last_activity_at, w.name as workspace_name " +
                "FROM channels c " +
                "LEFT JOIN workspaces w ON c.workspace_id = w.id " +
                "WHERE c.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ChannelDTO channel = mapResultSetToChannelDTO(rs);
                    channel.setMembersCount(getMemberCount(channelId));
                    channel.setMessageCount(getMessageCount(channelId));
                    return channel;
                }
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get channel by ID: " + e.getMessage());
        }

        return null;
    }

    // Get all channels in the workspace
    public List<ChannelDTO> getChannelsByWorkspace(int workspaceId) {
        String sql = "SELECT c.id, c.name, c.description, c.workspace_id, c.type, c.is_private, " +
                "c.is_archived, c.created_by, c.created_at, c.last_activity_at, w.name as workspace_name " +
                "FROM channels c " +
                "INNER JOIN workspaces w ON c.workspace_id = w.id " +
                "WHERE c.workspace_id = ? AND c.is_archived = FALSE " +
                "ORDER BY c.type DESC, c.name ASC";

        List<ChannelDTO> channels = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChannelDTO channel = mapResultSetToChannelDTO(rs);
                    channels.add(channel);
                }
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get channels by workspace: " + e.getMessage());
        }

        // Set member and message count for the channel
        for (ChannelDTO channel : channels) {
            channel.setMembersCount(getMemberCount(channel.getId()));
            channel.setMessageCount(getMessageCount(channel.getId()));
        }

        return channels;
    }

    // Get all channels a user belongs to
    public List<ChannelDTO> getUserChannelsInWorkspace(int workspaceId, int userId) {
        String sql = "SELECT c.id, c.name, c.description, c.workspace_id, c.type, c.is_private, " +
                "c.is_archived, c.created_by, c.created_at, c.last_activity_at, w.name as workspace_name " +
                "FROM channels c " +
                "INNER JOIN workspaces w ON c.workspace_id = w.id " +
                "INNER JOIN channel_members cm ON c.id = cm.channel_id " +
                "WHERE c.workspace_id = ? AND cm.user_id = ? AND c.is_archived = FALSE";

        List<ChannelDTO> channels = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChannelDTO channel = mapResultSetToChannelDTO(rs);
                    channels.add(channel);
                }
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get user channels: " + e.getMessage());
        }

        // Set message and member count for the channel
        for(ChannelDTO channel : channels) {
            channel.setHasJoined(true);
            channel.setMembersCount(getMemberCount(channel.getId()));
            channel.setUnreadCount(getUnreadCount(channel.getId(), userId));
        }

        return channels;
    }

    // Get all direct messages a user belongs to
    public List<ChannelDTO> getDirectMessages(int userId) {
        String sql = "SELECT c.id, c.name, c.description, c.workspace_id, c.type, c.is_private, " +
                "c.is_archived, c.created_by, c.created_at, c.last_activity_at, NULL as workspace_name " +
                "FROM channels c " +
                "INNER JOIN channel_members cm ON c.id = cm.channel_id " +
                "WHERE c.workspace_id IS NULL AND cm.user_id = ? AND c.is_archived = FALSE " +
                "ORDER BY c.last_activity_at DESC";

        List<ChannelDTO> channels = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChannelDTO channel = mapResultSetToChannelDTO(rs);
                    channels.add(channel);
                }
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get user DMs: " + e.getMessage());
        }

        for (ChannelDTO channel : channels) {
            channel.setHasJoined(true);
            channel.setMembersCount(getMemberCount(channel.getId()));
            channel.setUnreadCount(getUnreadCount(channel.getId(), userId));
        }

        return channels;
    }

    // Create a Direct Message channel
    public ChannelDTO createDirectMessage(int userId1, int userId2) {
        // Check if DM already exists
        String checkSql = "SELECT c.id FROM channels c " +
                "JOIN channel_members cm1 ON c.id = cm1.channel_id " +
                "JOIN channel_members cm2 ON c.id = cm2.channel_id " +
                "WHERE c.workspace_id IS NULL AND cm1.user_id = ? AND cm2.user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, userId1);
            checkStmt.setInt(2, userId2);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return getChannelById(rs.getInt(1)); // DM already exists
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to check existing DM: " + e.getMessage());
        }

        // Create new DM
        String insertSql = "INSERT INTO channels (name, description, workspace_id, type, is_private, created_by) " +
                "VALUES (?, ?, NULL, 'DIRECT_MESSAGE', TRUE, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, "DM");
            stmt.setString(2, "Direct Message");
            stmt.setInt(3, userId1);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int channelId = generatedKeys.getInt(1);
                        addMemberToChannel(channelId, userId1);
                        addMemberToChannel(channelId, userId2);
                        return getChannelById(channelId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to create DM: " + e.getMessage());
        }
        return null;
    }

    // Update channel details
    public boolean updateChannel(int channelId, String name, String description) {
        StringBuilder sql = new StringBuilder("UPDATE channels SET ");
        List<Object> params = new ArrayList<>();

        if (name != null) {
            sql.append("name = ?, ");
            params.add(name);
        }
        if (description != null) {
            sql.append("description = ?, ");
            params.add(description);
        }

        if (params.isEmpty()) {
            return true;
        }

        sql.append("last_activity_at = ? WHERE id = ?");
        params.add(new Timestamp(System.currentTimeMillis()));
        params.add(channelId);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("[Database] Updated channel: " + channelId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update channel: " + e.getMessage());
        }

        return false;
    }

    // Archive channel - soft delete
    public boolean archiveChannel(int channelId) {
        String sql = "UPDATE channels SET is_archived = TRUE WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("[Database] Archived channel: " + channelId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to archive channel: " + e.getMessage());
        }

        return false;
    }

    // Permanently delete channel
    public boolean deleteChannel(int channelId) {
        String sql = "DELETE FROM channels WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Deleted channel: " + channelId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete channel: " + e.getMessage());
        }

        return false;
    }

    // Add member to the channel
    public boolean addMemberToChannel(int channelId, int userId) {
        String sql = "INSERT INTO channel_members (channel_id, user_id, joined_at, last_read_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            stmt.setInt(2, userId);
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

            stmt.executeUpdate();
            System.out.println("[Database] User " + userId + " joined channel " + channelId);
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                return true;
            }
            System.err.println("[Database Error] Failed to add member to channel: " + e.getMessage());
            return false;
        }
    }

    // Remove member from a channel
    public boolean removeMemberFromChannel(int channelId, int userId) {
        String sql = "DELETE FROM channel_members WHERE channel_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            stmt.setInt(2, userId);

            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                System.out.println("[Database] User " + userId + " left channel " + channelId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to remove member from channel: " + e.getMessage());
        }

        return false;
    }

    // Check if a user is member of a channel
    public boolean isMemberOfChannel(int channelId, int userId) {
        String sql = "SELECT 1 FROM channel_members WHERE channel_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to check channel membership: " + e.getMessage());
            return false;
        }
    }

    // Get all member IDs of a channel
    public List<Integer> getChannelMemberIds(int channelId) {
        String sql = "SELECT user_id FROM channel_members WHERE channel_id = ?";
        List<Integer> memberIds = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setInt(1, channelId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    memberIds.add(rs.getInt("user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get channel members: " + e.getMessage());
        }
        
        return memberIds;
    }

    // Get member count for the channel
    public int getMemberCount(int channelId) {
        String sql = "SELECT COUNT(*) FROM channel_members WHERE channel_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get channel member count: " + e.getMessage());
        }

        return 0;
    }

    // Get message count for a channel
    public int getMessageCount(int channelId) {
        String sql = "SELECT COUNT(*) FROM messages WHERE channel_id = ? AND is_deleted = FALSE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get message count: " + e.getMessage());
        }
        return 0;
    }

    // Get unread message for a user in the channel
    public int getUnreadCount(int channelId, int userId) {
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
            System.err.println("[Database Error] Failed to get unread count: " + e.getMessage());
        }

        return 0;
    }

    // Update last read timestamp for a user in channel
    public boolean updateLastRead(int channelId, int userId) {
        String sql = "UPDATE channel_members SET last_read_at = ? WHERE channel_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, channelId);
            stmt.setInt(3, userId);

            int updated = stmt.executeUpdate();
            return updated > 0;

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update last read: " + e.getMessage());
            return false;
        }
    }

    // Update channel's last activity timestamp
    public void updateLastActivity(int channelId) {
        String sql = "UPDATE channels SET last_activity_at = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, channelId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Database Warning] Failed to update last activity: " + e.getMessage());
        }
    }

    // Get default channel for a workspace
    public ChannelDTO getDefaultChannel(int workspaceId) {
        String sql = "SELECT w.default_channel_name, c.id, c.name, c.description, c.workspace_id, c.type, " +
                "c.is_private, c.is_archived, c.created_by, c.created_at, c.last_activity_at " +
                "FROM workspaces w " +
                "LEFT JOIN channels c ON w.id = c.workspace_id AND c.name = w.default_channel_name " +
                "WHERE w.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt("id") > 0) {
                    return mapResultSetToChannelDTO(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get default channel: " + e.getMessage());
        }

        return null;
    }


    // Map ResultSet row to ChannelDTO object
    private ChannelDTO mapResultSetToChannelDTO(ResultSet rs) throws SQLException {
        ChannelDTO channel = new ChannelDTO();
        channel.setId(rs.getInt("id"));
        channel.setName(rs.getString("name"));
        channel.setDescription(rs.getString("description"));
        channel.setWorkspaceId(rs.getInt("workspace_id"));
        try {
            channel.setWorkspaceName(rs.getString("workspace_name"));
        } catch (SQLException e) {
            // Might not exist in some queries (like DMs)
        }

        String typeStr = rs.getString("type");
        if (typeStr != null) {
            try {
                channel.setType(ChannelType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                channel.setType(ChannelType.TEXT);
            }
        }

        channel.setPrivate(rs.getBoolean("is_private"));
        channel.setArchived(rs.getBoolean("is_archived"));
        channel.setCreatedBy(rs.getInt("created_by"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            channel.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastActivityAt = rs.getTimestamp("last_activity_at");
        if (lastActivityAt != null) {
            channel.setLastActivityAt(lastActivityAt.toLocalDateTime());
        }

        return channel;
    }
}