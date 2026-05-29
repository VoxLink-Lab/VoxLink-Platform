package voxlink.server.src.main.repository;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.WorkspaceDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkspaceRepository handles all database operations for Workspace entities.
 */
public class WorkspaceRepository {

    // Create a new workspaceDTO
    public WorkspaceDTO createWorkspace(String name, String description, int ownerId, boolean isPublic) {
        String sql = "INSERT INTO workspaces (name, description, owner_id, is_public, default_channel_name, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, ownerId);
            stmt.setBoolean(4, isPublic);
            stmt.setString(5, "general");
            stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int workspaceId = generatedKeys.getInt(1);
                        System.out.println("[Database] Created workspace: " + name + " (ID: " + workspaceId + ")");

                        // Add owner as a member of the workspace
                        addMemberToWorkspace(workspaceId, ownerId);

                        return getWorkspaceById(workspaceId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to create workspace: " + e.getMessage());
        }
        return null;
    }

    // Get workspace by ID
    public WorkspaceDTO getWorkspaceById(int workspaceId) {
        String sql = "SELECT * FROM workspaces WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    WorkspaceDTO workspace = mapResultSetToWorkspaceDTO(rs);

                    // Get member count
                    workspace.setMemberCount(getMemberCount(workspaceId));

                    // Get owner username
                    String ownerUsername = getOwnerUsername(workspaceId);
                    workspace.setOwnerUsername(ownerUsername);

                    return workspace;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get workspace by ID: " + e.getMessage());
        }

        return null;
    }

    // Get workspace by invite code
    public WorkspaceDTO getWorkspaceByInviteCode(String inviteCode) {
        String sql = "SELECT * FROM workspaces WHERE invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    WorkspaceDTO workspace = mapResultSetToWorkspaceDTO(rs);
                    workspace.setMemberCount(getMemberCount(workspace.getId()));
                    return workspace;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get workspace by invite code: " + e.getMessage());
        }

        return null;
    }

    // Get all workspaces a user belong to
    public List<WorkspaceDTO> getWorkspacesByUser(int userId) {
        String sql = "SELECT w.id, w.name, w.description, w.icon_url, w.banner_url, w.owner_id, w.is_public, " +
                "w.default_channel_name, w.invite_code, w.invite_expires_at, w.max_invite_uses, w.created_at " +
                "FROM workspaces w " +
                "INNER JOIN workspace_members wm ON w.id = wm.workspace_id " +
                "WHERE wm.user_id = ?";

        List<WorkspaceDTO> workspaces = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WorkspaceDTO workspace = mapResultSetToWorkspaceDTO(rs);
                    workspace.setMemberCount(getMemberCount(workspace.getId()));
                    workspaces.add(workspace);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get workspaces by user: " + e.getMessage());
        }

        return workspaces;
    }

    // Get all public workspaces
    public List<WorkspaceDTO> getPublicWorkspaces() {
        String sql = "SELECT * FROM workspaces WHERE is_public = TRUE";

        List<WorkspaceDTO> workspaces = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                WorkspaceDTO workspace = mapResultSetToWorkspaceDTO(rs);
                workspace.setMemberCount(getMemberCount(workspace.getId()));
                workspaces.add(workspace);
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get public workspaces: " + e.getMessage());
        }

        return workspaces;
    }

   // Update workspace details
    public boolean updateWorkspace(int workspaceId, String name, String description, String iconUrl, String bannerUrl) {
        StringBuilder sql = new StringBuilder("UPDATE workspaces SET ");
        List<Object> params = new ArrayList<>();

        if (name != null) {
            sql.append("name = ?, ");
            params.add(name);
        }
        if (description != null) {
            sql.append("description = ?, ");
            params.add(description);
        }
        if (iconUrl != null) {
            sql.append("icon_url = ?, ");
            params.add(iconUrl);
        }
        if (bannerUrl != null) {
            sql.append("banner_url = ?, ");
            params.add(bannerUrl);
        }

        if (params.isEmpty()) {
            return true;
        }

        sql.append("created_at = created_at WHERE id = ?");
        params.add(workspaceId);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(String.valueOf(sql))) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("[Database] Updated workspace: " + workspaceId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update workspace: " + e.getMessage());
        }

        return false;
    }

    // Delete workspace
    public boolean deleteWorkspace(int workspaceId) {
        String sql = "DELETE FROM workspaces WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Deleted workspace: " + workspaceId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete workspace: " + e.getMessage());
        }

        return false;
    }

    // Add member to a workspace
    public boolean addMemberToWorkspace(int workspaceId, int userId) {
        String sql = "INSERT INTO workspace_members (workspace_id, user_id, joined_at) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setInt(2, userId);
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();

            System.out.println("[Database] User " + userId + " joined workspace " + workspaceId);
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("[Database] User already in workspace");
                return true;
            }

            System.err.println("[Database Error] Failed to add member to workspace: " + e.getMessage());
            return false;
        }
    }

    // Remove user from workspace
    public boolean removeMemberFromWorkspace(int workspaceId, int userId) {
        String sql = "DELETE FROM workspace_members WHERE workspace_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setInt(2, userId);

            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                System.out.println("[Database] User " + userId + " left workspace " + workspaceId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to remove member from workspace: " + e.getMessage());
        }

        return false;
    }

    // Check if user is member of workspace
    public boolean isMemberOfWorkspace(int workspaceId, int userId) {
        String sql = "SELECT 1 FROM workspace_members WHERE workspace_id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to check workspace membership: " + e.getMessage());
            return false;
        }
    }

    // Get member count for the workspace
    public int getMemberCount(int workspaceId) {
        String sql = "SELECT COUNT(*) FROM workspace_members WHERE workspace_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get member count: " + e.getMessage());
        }

        return 0;
    }

    // Get owner username
    private String getOwnerUsername(int workspaceId) {
        String sql = "SELECT u.username FROM users u " +
                "INNER JOIN workspaces w ON u.id = w.owner_id " +
                "WHERE w.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get owner username: " + e.getMessage());
        }

        return null;
    }

    // Generate an invite code for a workspace
    public boolean setInviteCode(int workspaceId, String inviteCode, Timestamp expiresAt, int maxUses) {
        String sql = "UPDATE workspaces SET invite_code = ?, invite_expires_at = ?, max_invite_uses = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);
            stmt.setTimestamp(2, expiresAt);
            stmt.setInt(3, maxUses);
            stmt.setInt(4, workspaceId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to set invite code: " + e.getMessage());
            return false;
        }
    }

    // Remove invite code form workspace
    public boolean removeInviteCode(int workspaceId) {
        String sql = "UPDATE workspaces SET invite_code = NULL, invite_expires_at = NULL WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
           return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to remove invite code: " + e.getMessage());
            return false;
        }
    }

    // Validate workspace's invite code
    public WorkspaceDTO validateInviteCode(String inviteCode) {
        String sql = "SELECT * FROM workspaces WHERE invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Check if invite is expired
                    Timestamp expiresAt = rs.getTimestamp("invite_expires_at");
                    if (expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()))) {
                        System.out.println("[Database] Invite code expired: " + inviteCode);
                        return null;
                    }

                    // Check usage limit
                    int maxUses = rs.getInt("max_invite_uses");
                    if (maxUses > 0) {
                        int currentUses = getInviteUsageCount(inviteCode);
                        if (currentUses >= maxUses) {
                            System.out.println("[Database] Invite code reached max uses: " + inviteCode);
                            return null;
                        }
                    }

                    return mapResultSetToWorkspaceDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to validate invite code: " + e.getMessage());
        }

        return null;
    }

    // Increment invite usage for invite code
    public void incrementInviteUsage(String inviteCode) {
        String sql = "UPDATE workspaces SET invite_uses_so_far = COALESCE(invite_uses_so_far, 0) + 1 " +
                "WHERE invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to increment invite usage: " + e.getMessage());
        }
    }

    // Get current usage count for invite code
    private int getInviteUsageCount(String inviteCode) {
        String sql = "SELECT invite_uses_so_far FROM workspaces WHERE invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("invite_uses_so_far");
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get invite usage: " + e.getMessage());
        }

        return 0;
    }

    // Map ResultSet row to WorkspaceDTO object
    private WorkspaceDTO mapResultSetToWorkspaceDTO(ResultSet rs) throws SQLException {
        WorkspaceDTO workspace = new WorkspaceDTO();
        workspace.setId(rs.getInt("id"));
        workspace.setName(rs.getString("name"));
        workspace.setDescription(rs.getString("description"));
        workspace.setIconUrl(rs.getString("icon_url"));
        workspace.setBannerUrl(rs.getString("banner_url"));
        workspace.setOwnerId(rs.getInt("owner_id"));
        workspace.setPublic(rs.getBoolean("is_public"));
        workspace.setDefaultChannelName(rs.getString("default_channel_name"));
        workspace.setInviteCode(rs.getString("invite_code"));

        Timestamp inviteExpiresAt = rs.getTimestamp("invite_expires_at");
        if (inviteExpiresAt != null) {
            workspace.setInviteExpiresAt(inviteExpiresAt.toLocalDateTime());
        }

        workspace.setMaxInviteUses(rs.getInt("max_invite_uses"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            workspace.setCreatedAt(createdAt.toLocalDateTime());
        }

        return workspace;
    }
}