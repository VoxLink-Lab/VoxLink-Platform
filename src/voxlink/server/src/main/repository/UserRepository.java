package voxlink.server.src.main.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.RoleDTO;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;

/**
 * UserRepository handles all database operations related to user.
 */
public class UserRepository {

    // Register new user to database
    public UserDTO createUser(String username, String passwordHash, String email, String displayName) {
        String sql = "INSERT INTO users (username, password_hash, email, display_name, status, created_at, last_active_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try(Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, email);
            stmt.setString(4, displayName);
            stmt.setString(5, UserStatus.OFFLINE.name());
            stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            stmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));

            int rowsAffected = stmt.executeUpdate();

            if(rowsAffected > 0) {
                try(ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if(generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);
                        System.out.println("[Database] Created a new user: " + username + "(ID: " + userId + ")");
                        return getUserById(userId);
                    }
                }
            }

        } catch(SQLException e) {
            System.err.println("[Database Error] Failed to create user: " + e.getMessage());

            if(e.getMessage().contains("Duplicate entry") && e.getMessage().contains("username")) {
                System.err.println("Username " + username + " already exists!");
            } else if(e.getMessage().contains("Duplicate entry") && e.getMessage().contains("email")) {
                System.err.println("Email " + email + " already exists!");
            }
        }

        return null;
    }

    // Fetch user detail by ID
    public UserDTO getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try(Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try(ResultSet row = stmt.executeQuery()) {
                if(row.next()) {
                    return mapResultSetToUserDTO(row);
                }
            }

        } catch(SQLException e) {
            System.err.println("[Database Error] Failed to get user by ID: " + e.getMessage());
        }

        return null;
    }

    // Fetch user detail by username
    public UserDTO getUserByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            try(ResultSet row = stmt.executeQuery()) {
                if(row.next()) {
                    return mapResultSetToUserDTO(row);
                }
            }

        } catch (SQLException e) {
            System.out.println("[Database Error] Failed to get user by username: " + e.getMessage());
        }

        return null;
    }

    // Fetch user detail by email
    public UserDTO getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            try(ResultSet row = stmt.executeQuery()) {
                if(row.next()) {
                    return mapResultSetToUserDTO(row);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error getting user by email: " + e.getMessage());
        }

        return null;
    }

    // Validate user credentials
    public UserDTO authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? AND is_active = TRUE";

        try(Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try(ResultSet row = stmt.executeQuery()) {
                if(row.next()) {
                    UserDTO user = mapResultSetToUserDTO(row);
                    System.out.println("[Database] user authenticated: " + username);
                    updateLastActive(user.getId());
                    return user;
                }
            }

        } catch(SQLException e) {
            System.err.println("[Database Error] Authentication Failed: " + e.getMessage());
        }

        return null;
    }

    // Update user status
    public boolean updateUserStatus(int userId, UserStatus status) {
        String sql = "UPDATE users SET status = ?, last_active_at = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update user status: " + e.getMessage());
        }

        return false;
    }

    // Update last time user is active
    public void updateLastActive(int userId) {
        String sql = "UPDATE users SET last_active_at = ? WHERE id = ?";

        try(Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, userId);
            stmt.executeUpdate();

        } catch(SQLException e) {
            System.err.println("[Database Error] Failed to update last active at: " + e.getMessage());
        }
    }

    // Update user profile
    public boolean updateProfile(int userId, String displayName, String avatar_url, String customStatus) {

        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        List<Object> params = new ArrayList<>();

        if(displayName != null) {
            sql.append("display_name = ? ");
            params.add(displayName);
        }

        if(avatar_url != null) {
            sql.append("avatar_url = ? ");
            params.add(avatar_url);
        }

        if(customStatus != null) {
            sql.append("custom_status = ? ");
            params.add(customStatus);
        }

        if(params.isEmpty()) {
            return true;
        }

        sql.append("last_active_at = ? WHERE id = ?");
        params.add(new Timestamp(System.currentTimeMillis()));
        params.add(userId);

        try(Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(String.valueOf(sql))) {

            for(int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            return stmt.executeUpdate() > 0;

        } catch(SQLException e) {
            System.err.println("[Database Error] Failed to update user profile: " + e.getMessage());
        }

        return false;
    }

    // Get currently online users
    public List<UserDTO> getOnlineUsers() {
        String sql = "SELECT * FROM users WHERE status IN ('ONLINE', 'IDEL', 'DO_NOT_DISTURB')";
        List<UserDTO> onlineUsers = new ArrayList<>();

        try(Connection connection = DBConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {

            while(rs.next()) {
                onlineUsers.add(mapResultSetToUserDTO(rs));
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get online users: " + e.getMessage());
        }

        return onlineUsers;
    }

    // Get users by workspace ID
    public List<UserDTO> getUsersByWorkspace(int workspaceId) {
        String sql = "SELECT u.id, u.username, u.email, u.display_name, u.avatar_url, u.status, u.custom_status, " +
                "u.created_at, u.last_active_at, u.is_active " +
                "FROM users u " +
                "INNER JOIN workspace_members wm ON u.id = wm.user_id " +
                "WHERE wm.workspace_id = ?";

        List<UserDTO> users = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            try(ResultSet rows = stmt.executeQuery()) {
                while(rows.next()) {
                    users.add(mapResultSetToUserDTO(rows));
                }
            }

        } catch (SQLException e) {
            System.err.println("[DB Error] Failed to get users by workspace: " + e.getMessage());
        }

        return users;
    }

    // Get user roles in workspace
    public Set<RoleDTO> getUserRolesInWorkspace(int userId, int workspaceId) {
        String sql = "SELECT r.id, r.name, r.description, r.workspace_id, r.priority, " +
                "r.is_default, r.is_system_role, r.permissions " +
                "FROM roles r " +
                "INNER JOIN user_roles ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ? AND ur.workspace_id = ?";

        Set<RoleDTO> roles = new HashSet<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, workspaceId);

            try(ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) {
                    RoleDTO role = new RoleDTO();
                    role.setId(rs.getInt("id"));
                    role.setName(rs.getString("name"));
                    role.setDescription(rs.getString("description"));
                    role.setWorkspaceId(rs.getInt("workspace_id"));
                    role.setPriority(rs.getInt("priority"));
                    role.setDefault(rs.getBoolean("is_default"));
                    role.setSystemRole(rs.getBoolean("is_system_role"));

                    String permissionsStr = rs.getString("permissions");
                    if (permissionsStr != null && !permissionsStr.isEmpty()) {
                        for (String perm : permissionsStr.split(",")) {
                            role.addPermission(perm.trim());
                        }
                    }
                    roles.add(role);
                }
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get user roles: " + e.getMessage());
        }

        return roles;
    }

    // Set user inactive
    public boolean setUserInactive(int userId) {
        String sql = "UPDATE users SET is_active = FALSE WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("[Database] User set to inactive: " + userId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete user: " + e.getMessage());
        }

        return false;
    }

    // Delete user account
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] User deleted: " + userId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to hard delete user: " + e.getMessage());
        }

        return false;
    }

    // Map fetched data to a userDTO
    public UserDTO mapResultSetToUserDTO(ResultSet rs) throws SQLException {

        UserDTO user = new UserDTO();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setDisplayName(rs.getString("display_name"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setCustomStatus(rs.getString("custom_status"));

        String status = rs.getString("status");
        if(status != null){
            try {
                user.setStatus(UserStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                user.setStatus(UserStatus.OFFLINE);
            }
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if(createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastActiveAt = rs.getTimestamp("last_active_at");
        if(lastActiveAt != null) {
            user.setLastActiveAt(lastActiveAt.toLocalDateTime());
        }

        return user;
    }
}
