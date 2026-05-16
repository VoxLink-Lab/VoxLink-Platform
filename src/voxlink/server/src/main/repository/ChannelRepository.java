package voxlink.server.src.main.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import voxlink.server.src.main.database.DBConnection;
import voxlink.server.src.main.model.Channel;

public class ChannelRepository {

    public boolean createChannel(Channel channel) {
        String query = "INSERT INTO channels (workspace_id, name, type, is_private, channel_profile_picture) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, channel.getWorkspaceId());
            pstmt.setString(2, channel.getName());
            pstmt.setString(3, channel.getType() != null ? channel.getType() : "TEXT");
            pstmt.setBoolean(4, channel.isPrivate());
            pstmt.setString(5, channel.getChannelProfilePicture());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        channel.setChannelId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error creating channel: " + e.getMessage());
        }
        return false;
    }

    public Channel getChannelById(int channelId) {
        String query = "SELECT * FROM channels WHERE channel_id = ?";
        try (Connection conn = DBConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, channelId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToChannel(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting channel by ID: " + e.getMessage());
        }
        return null;
    }

    public List<Channel> getChannelsByWorkspaceId(int workspaceId) {
        List<Channel> channels = new ArrayList<>();
        String query = "SELECT * FROM channels WHERE workspace_id = ?";
        try (Connection conn = DBConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, workspaceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    channels.add(mapResultSetToChannel(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting channels by workspace ID: " + e.getMessage());
        }
        return channels;
    }

    public boolean deleteChannel(int channelId) {
        String query = "DELETE FROM channels WHERE channel_id = ?";
        try (Connection conn = DBConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, channelId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting channel: " + e.getMessage());
        }
        return false;
    }

    private Channel mapResultSetToChannel(ResultSet rs) throws SQLException {
        return new Channel(
                rs.getInt("channel_id"),
                rs.getInt("workspace_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getBoolean("is_private"),
                rs.getTimestamp("created_at"),
                rs.getString("channel_profile_picture"));
    }
}
