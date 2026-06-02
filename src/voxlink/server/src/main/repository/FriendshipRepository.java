package voxlink.server.src.main.repository;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.UserDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FriendshipRepository {

    private final UserRepository userRepository;

    public FriendshipRepository() {
        this.userRepository = new UserRepository();
    }

    public boolean addFriendRequest(int userId, String targetUsername) {
        // Find target user
        UserDTO targetUser = userRepository.getUserByUsername(targetUsername);
        if (targetUser == null) return false;
        
        int friendId = targetUser.getId();
        if (userId == friendId) return false; // Can't add yourself

        String query = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'PENDING')";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, friendId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("[FriendshipRepository] Error sending friend request: " + e.getMessage());
            return false;
        }
    }

    public boolean acceptFriendRequest(int userId, int friendId) {
        String query = "UPDATE friendships SET status = 'ACCEPTED' WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, friendId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, friendId);
            
            if (stmt.executeUpdate() > 0) {
                // Ensure bidirectional relationship if not already present
                String checkQuery = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setInt(1, userId);
                    checkStmt.setInt(2, friendId);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        String insertReverse = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'ACCEPTED')";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertReverse)) {
                            insertStmt.setInt(1, userId);
                            insertStmt.setInt(2, friendId);
                            insertStmt.executeUpdate();
                        }
                    }
                }
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println("[FriendshipRepository] Error accepting friend request: " + e.getMessage());
            return false;
        }
    }

    public List<UserDTO> getFriends(int userId) {
        List<UserDTO> friends = new ArrayList<>();
        String query = """
            SELECT u.* FROM users u 
            JOIN friendships f ON u.id = f.friend_id 
            WHERE f.user_id = ? AND f.status = 'ACCEPTED'
        """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    friends.add(userRepository.mapResultSetToUserDTO(rs));
                }
            }
            
            // Also get requests where this user is the friend_id
            String queryReverse = """
                SELECT u.* FROM users u 
                JOIN friendships f ON u.id = f.user_id 
                WHERE f.friend_id = ? AND f.status = 'ACCEPTED'
            """;
            try (PreparedStatement stmtRev = conn.prepareStatement(queryReverse)) {
                stmtRev.setInt(1, userId);
                try (ResultSet rsRev = stmtRev.executeQuery()) {
                    while (rsRev.next()) {
                        UserDTO user = userRepository.mapResultSetToUserDTO(rsRev);
                        if (!friends.contains(user)) {
                            friends.add(user);
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("[FriendshipRepository] Error getting friends: " + e.getMessage());
        }
        
        return friends;
    }
}
