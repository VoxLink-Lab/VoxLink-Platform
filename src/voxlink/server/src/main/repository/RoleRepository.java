package voxlink.server.src.main.repository;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.RoleDTO;
import voxlink.shared.dto.UserRoleInWorkspace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RoleRepository handles all database operations for Role entities.
 */
public class RoleRepository {

    // Create a role
    public RoleDTO createRole(String name, String description, int workspaceId, int priority,
                              boolean isDefault, boolean isSystemRole, Set<String> permissions) {
        String sql = "INSERT INTO roles (name, description, workspace_id, priority, is_default, is_system_role, permissions) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, workspaceId);
            stmt.setInt(4, priority);
            stmt.setBoolean(5, isDefault);
            stmt.setBoolean(6, isSystemRole);

            // Convert permissions set to comma-separated string
            String permissionsStr = String.join(",", permissions);
            stmt.setString(7, permissionsStr);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int roleId = generatedKeys.getInt(1);
                        System.out.println("[Database] Created role: " + name + " (ID: " + roleId + ") in workspace " + workspaceId);
                        return getRoleById(roleId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to create role: " + e.getMessage());
        }

        return null;
    }

    // Create the default system roles for a new workspace
    public void createDefaultRoles(int workspaceId) {
        // ADMIN role
        Set<String> adminPerms = RoleDTO.Permissions.getAllPermissions();
        createRole("ADMIN", "Full access to all workspace features", workspaceId, 100, false, true, adminPerms);

        // MODERATOR role
        Set<String> modPerms = RoleDTO.Permissions.getModeratorPermissions();
        createRole("MODERATOR", "Can manage members and moderate content", workspaceId, 50, false, true, modPerms);

        // MEMBER role (default)
        Set<String> memberPerms = RoleDTO.Permissions.getMemberPermissions();
        createRole("MEMBER", "Standard member permissions", workspaceId, 10, true, true, memberPerms);

        System.out.println("[Database] Created default roles for workspace " + workspaceId);
    }

    // Get role by ID
    public RoleDTO getRoleById(int roleId) {
        String sql = "SELECT id, name, description, workspace_id, priority, is_default, is_system_role, permissions " +
                "FROM roles WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRoleDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get role by ID: " + e.getMessage());
        }

        return null;
    }

    // Get role by name in the workspace
    public RoleDTO getRoleByName(int workspaceId, String roleName) {
        String sql = "SELECT id, name, description, workspace_id, priority, is_default, is_system_role, permissions " +
                "FROM roles WHERE workspace_id = ? AND name = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setString(2, roleName.toUpperCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRoleDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get role by name: " + e.getMessage());
        }

        return null;
    }

    // Get all roles in the workspace
    public List<RoleDTO> getRolesByWorkspace(int workspaceId) {
        String sql = "SELECT id, name, description, workspace_id, priority, is_default, is_system_role, permissions " +
                "FROM roles WHERE workspace_id = ? ORDER BY priority DESC";

        List<RoleDTO> roles = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(mapResultSetToRoleDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get roles by workspace: " + e.getMessage());
        }

        return roles;
    }

    // Update role permission
    public boolean updateRolePermissions(int roleId, Set<String> permissions) {
        String sql = "UPDATE roles SET permissions = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String permissionsStr = String.join(",", permissions);
            stmt.setString(1, permissionsStr);
            stmt.setInt(2, roleId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update role permissions: " + e.getMessage());
            return false;
        }
    }

    // Update role priority
    public boolean updateRolePriority(int roleId, int priority) {
        String sql = "UPDATE roles SET priority = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, priority);
            stmt.setInt(2, roleId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update role priority: " + e.getMessage());
            return false;
        }
    }

    // Delete role - can't delete system roles
    public boolean deleteRole(int roleId) {
        // First check if it's a system role
        String checkSql = "SELECT is_system_role FROM roles WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, roleId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getBoolean("is_system_role")) {
                    System.err.println("[Database Error] Cannot delete system role");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to check system role: " + e.getMessage());
            return false;
        }

        String sql = "DELETE FROM roles WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roleId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[DB] Deleted role: " + roleId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete role: " + e.getMessage());
        }
        return false;
    }

    // Assign a role to a user in a workspace
    public boolean assignRoleToUser(int userId, int roleId, int workspaceId, int assignedBy) {
        String sql = "INSERT INTO user_roles (user_id, role_id, workspace_id, assigned_at, assigned_by) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE assigned_at = ?, assigned_by = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());
            stmt.setInt(1, userId);
            stmt.setInt(2, roleId);
            stmt.setInt(3, workspaceId);
            stmt.setTimestamp(4, now);
            stmt.setInt(5, assignedBy);
            stmt.setTimestamp(6, now);
            stmt.setInt(7, assignedBy);

            stmt.executeUpdate();
            System.out.println("[Database] Assigned role " + roleId + " to user " + userId + " in workspace " + workspaceId);
            return true;

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to assign role to user: " + e.getMessage());
            return false;
        }
    }

    // Assign a role by name to a user
    public boolean assignRoleByName(int userId, int workspaceId, String roleName, int assignedBy) {
        RoleDTO role = getRoleByName(workspaceId, roleName);
        if (role == null) {
            System.err.println("[Database Error] Role not found: " + roleName);
            return false;
        }
        return assignRoleToUser(userId, role.getId(), workspaceId, assignedBy);
    }

   // Remove a role from the workspace
    public boolean removeRoleFromUser(int userId, int roleId, int workspaceId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ? AND workspace_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, roleId);
            stmt.setInt(3, workspaceId);

            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                System.out.println("[Database] Removed role " + roleId + " from user " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to remove role from user: " + e.getMessage());
        }

        return false;
    }

    // Get all roles assigned to a user in the workspace
    public Set<RoleDTO> getUserRoles(int userId, int workspaceId) {
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

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(mapResultSetToRoleDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB Error] Failed to get user roles: " + e.getMessage());
        }

        return roles;
    }

    // Get a user's highest role in a workspace (for permission checks)
    public UserRoleInWorkspace getUserHighestRole(int userId, int workspaceId) {
        Set<RoleDTO> roles = getUserRoles(userId, workspaceId);

        for (RoleDTO role : roles) {
            if (role.getName().equals("ADMIN")) {
                return UserRoleInWorkspace.ADMIN;
            }
        }

        for (RoleDTO role : roles) {
            if (role.getName().equals("MODERATOR")) {
                return UserRoleInWorkspace.MODERATOR;
            }
        }

        return UserRoleInWorkspace.MEMBER;
    }

    // Check if a user has a specific permission in a workspace
    public boolean hasPermission(int userId, int workspaceId, String permission) {
        Set<RoleDTO> roles = getUserRoles(userId, workspaceId);

        for (RoleDTO role : roles) {
            if (role.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    // Check if user is an admin in the workspace
    public boolean isAdmin(int userId, int workspaceId) {
        return hasPermission(userId, workspaceId, RoleDTO.Permissions.DELETE_WORKSPACE);
    }

    // Check if user is a moderator in the workspace
    public boolean isAtLeastModerator(int userId, int workspaceId) {
        return hasPermission(userId, workspaceId, RoleDTO.Permissions.KICK_MEMBER);
    }

    // Get default role for the workspace
    public RoleDTO getDefaultRole(int workspaceId) {
        String sql = "SELECT id, name, description, workspace_id, priority, is_default, is_system_role, permissions " +
                "FROM roles WHERE workspace_id = ? AND is_default = TRUE LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRoleDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get default role: " + e.getMessage());
        }
        return null;
    }

    // Assign default role to a user when they join a workspace
    public boolean assignDefaultRole(int userId, int workspaceId, int assignedBy) {
        RoleDTO defaultRole = getDefaultRole(workspaceId);
        if (defaultRole == null) {
            System.err.println("[DB Error] No default role found for workspace " + workspaceId);
            return false;
        }
        return assignRoleToUser(userId, defaultRole.getId(), workspaceId, assignedBy);
    }

    // Get all users with a specific role in a workspace
    public List<Integer> getUsersWithRole(int workspaceId, String roleName) {
        String sql = "SELECT ur.user_id FROM user_roles ur " +
                "INNER JOIN roles r ON ur.role_id = r.id " +
                "WHERE ur.workspace_id = ? AND r.name = ?";

        List<Integer> userIds = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setString(2, roleName.toUpperCase());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getInt("user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get users with role: " + e.getMessage());
        }
        return userIds;
    }

    // Remove all roles from a user in a workspace (when they leave)
    public boolean removeAllUserRoles(int userId, int workspaceId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND workspace_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, workspaceId);

            pstmt.executeUpdate();
            System.out.println("[Database] Removed all roles from user " + userId + " in workspace " + workspaceId);
            return true;

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to remove all user roles: " + e.getMessage());
            return false;
        }
    }

    // Map ResultSet row to RoleDTO object
    private RoleDTO mapResultSetToRoleDTO(ResultSet rs) throws SQLException {
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

        return role;
    }
}