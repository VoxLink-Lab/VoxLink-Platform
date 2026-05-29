package voxlink.server.src.main.repository;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.InviteDTO;
import voxlink.shared.dto.InviteType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * InviteRepository handles all database operations for Invite entities.
 */
public class InviteRepository {

    // Create new invite link for the repository
    public InviteDTO createInvite(String inviteCode, int workspaceId, int createdBy,
                                  Timestamp expiresAt, int maxUses, InviteType type,
                                  Integer invitedUserId, String targetChannelName) {
        String sql = "INSERT INTO invites (invite_code, workspace_id, created_by, created_at, " +
                "expires_at, max_uses, uses_so_far, is_active, invited_user_id, target_channel_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, inviteCode);
            stmt.setInt(2, workspaceId);
            stmt.setInt(3, createdBy);
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            stmt.setTimestamp(5, expiresAt);
            stmt.setInt(6, maxUses);
            stmt.setInt(7, 0); // uses_so_far starts at 0
            stmt.setBoolean(8, true); // is_active = true
            if (invitedUserId != null) {
                stmt.setInt(9, invitedUserId);
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }
            stmt.setString(10, targetChannelName);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int inviteId = generatedKeys.getInt(1);
                        System.out.println("[Database] Created invite: " + inviteCode + " (ID: " + inviteId + ") for workspace " + workspaceId);
                        return getInviteById(inviteId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to create invite: " + e.getMessage());
        }
        return null;
    }

    // Create simple one time invite code
    public InviteDTO createOneTimeInvite(String inviteCode, int workspaceId, int createdBy) {
        return createInvite(inviteCode, workspaceId, createdBy, null, 1, InviteType.ONE_TIME, null, null);
    }

    // Create a permanent invite code
    public InviteDTO createPermanentInvite(String inviteCode, int workspaceId, int createdBy) {
        return createInvite(inviteCode, workspaceId, createdBy, null, -1, InviteType.PERMANENT, null, null);
    }

    // Create time expiring invite code
    public InviteDTO createExpiringInvite(String inviteCode, int workspaceId, int createdBy, int daysUntilExpiry) {
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + (daysUntilExpiry * 24L * 60 * 60 * 1000));
        return createInvite(inviteCode, workspaceId, createdBy, expiresAt, -1, InviteType.EXPIRING, null, null);
    }

    // Get invite by ID
    public InviteDTO getInviteById(int inviteId) {
        String sql = "SELECT i.id, i.invite_code, i.workspace_id, i.created_by, i.created_at, " +
                "i.expires_at, i.max_uses, i.uses_so_far, i.is_active, i.invited_user_id, i.target_channel_name, " +
                "w.name as workspace_name, u.username as created_by_username " +
                "FROM invites i " +
                "INNER JOIN workspaces w ON i.workspace_id = w.id " +
                "INNER JOIN users u ON i.created_by = u.id " +
                "WHERE i.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, inviteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInviteDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get invite by ID: " + e.getMessage());
        }
        return null;
    }

    // Get invite by invite code
    public InviteDTO getInviteByCode(String inviteCode) {
        String sql = "SELECT i.id, i.invite_code, i.workspace_id, i.created_by, i.created_at, " +
                "i.expires_at, i.max_uses, i.uses_so_far, i.is_active, i.invited_user_id, i.target_channel_name, " +
                "w.name as workspace_name, u.username as created_by_username " +
                "FROM invites i " +
                "INNER JOIN workspaces w ON i.workspace_id = w.id " +
                "INNER JOIN users u ON i.created_by = u.id " +
                "WHERE i.invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInviteDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get invite by code: " + e.getMessage());
        }
        return null;
    }

    // Get all active inivites for a wrokspace
    public List<InviteDTO> getActiveInvitesByWorkspace(int workspaceId) {
        String sql = "SELECT i.id, i.invite_code, i.workspace_id, i.created_by, i.created_at, " +
                "i.expires_at, i.max_uses, i.uses_so_far, i.is_active, i.invited_user_id, i.target_channel_name, " +
                "w.name as workspace_name, u.username as created_by_username " +
                "FROM invites i " +
                "INNER JOIN workspaces w ON i.workspace_id = w.id " +
                "INNER JOIN users u ON i.created_by = u.id " +
                "WHERE i.workspace_id = ? AND i.is_active = TRUE";

        List<InviteDTO> invites = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invites.add(mapResultSetToInviteDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get active invites: " + e.getMessage());
        }
        return invites;
    }

    // Check if invite code is still valid
    public InviteDTO validateInviteCode(String inviteCode) {
        InviteDTO invite = getInviteByCode(inviteCode);

        if (invite == null) {
            System.out.println("[Database] Invite code not found: " + inviteCode);
            return null;
        }

        if (!invite.isValid()) {
            System.out.println("[Database] Invite code is invalid/expired: " + inviteCode);
            return null;
        }

        return invite;
    }

    // Increment invite link usage
    public boolean useInvite(String inviteCode) {
        String sql = "UPDATE invites SET uses_so_far = uses_so_far + 1 WHERE invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("[Database] Invite used: " + inviteCode);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to use invite: " + e.getMessage());
        }
        return false;
    }

    // Deactivate invite code
    public boolean deactivateInvite(int inviteId) {
        String sql = "UPDATE invites SET is_active = FALSE WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, inviteId);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("[Database] Deactivated invite: " + inviteId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to deactivate invite: " + e.getMessage());
        }
        return false;
    }

    // Deactivate invite by invite code
    public boolean deactivateInviteByCode(String inviteCode) {
        String sql = "UPDATE invites SET is_active = FALSE WHERE invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("[Database] Deactivated invite by code: " + inviteCode);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to deactivate invite by code: " + e.getMessage());
        }
        return false;
    }

    // Delete an invite permanently
    public boolean deleteInvite(int inviteId) {
        String sql = "DELETE FROM invites WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, inviteId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Deleted invite: " + inviteId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete invite: " + e.getMessage());
        }
        return false;
    }

    // Delete all invites for a workspace
    public int deleteInvitesByWorkspace(int workspaceId) {
        String sql = "DELETE FROM invites WHERE workspace_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Deleted " + deleted + " invites from workspace " + workspaceId);
            }
            return deleted;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete invites by workspace: " + e.getMessage());
        }
        return 0;
    }

    // Update invite expiration date
    public boolean updateInviteExpiration(int inviteId, Timestamp newExpiresAt) {
        String sql = "UPDATE invites SET expires_at = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, newExpiresAt);
            stmt.setInt(2, inviteId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update invite expiration: " + e.getMessage());
            return false;
        }
    }

    // Check if a user has been specifically invited to a workspace
    public boolean hasSpecificInvite(int workspaceId, int userId) {
        String sql = "SELECT 1 FROM invites WHERE workspace_id = ? AND invited_user_id = ? AND is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to check specific invite: " + e.getMessage());
        }
        return false;
    }

    // Get usage count for an invite code
    public int getInviteUsageCount(String inviteCode) {
        String sql = "SELECT uses_so_far FROM invites WHERE invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("uses_so_far");
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get invite usage count: " + e.getMessage());
        }
        return -1;
    }

    // Get remaining uses for an invite
    public int getRemainingUses(String inviteCode) {
        String sql = "SELECT max_uses, uses_so_far FROM invites WHERE invite_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, inviteCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int maxUses = rs.getInt("max_uses");
                    int usesSoFar = rs.getInt("uses_so_far");

                    if (maxUses == -1) {
                        return -1; // Unlimited
                    }
                    return Math.max(0, maxUses - usesSoFar);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get remaining uses: " + e.getMessage());
        }
        return 0;
    }

    // Clean up expired invites (deactivate them)
    public int cleanupExpiredInvites() {
        String sql = "UPDATE invites SET is_active = FALSE WHERE expires_at IS NOT NULL AND expires_at < ? AND is_active = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("[Database] Cleaned up " + updated + " expired invites");
            }
            return updated;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to cleanup expired invites: " + e.getMessage());
        }
        return 0;
    }

    // Map ResultSet row to InviteDTO object
    private InviteDTO mapResultSetToInviteDTO(ResultSet rs) throws SQLException {
        InviteDTO invite = new InviteDTO();
        invite.setId(rs.getInt("id"));
        invite.setInviteCode(rs.getString("invite_code"));
        invite.setWorkspaceId(rs.getInt("workspace_id"));
        invite.setWorkspaceName(rs.getString("workspace_name"));
        invite.setCreatedBy(rs.getInt("created_by"));
        invite.setCreatedByUsername(rs.getString("created_by_username"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            invite.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            invite.setExpiresAt(expiresAt.toLocalDateTime());
        }

        invite.setMaxUses(rs.getInt("max_uses"));
        invite.setUsesSoFar(rs.getInt("uses_so_far"));
        invite.setActive(rs.getBoolean("is_active"));

        int invitedUserId = rs.getInt("invited_user_id");
        if (!rs.wasNull() && invitedUserId > 0) {
            invite.setInvitedUserId(invitedUserId);
        }

        invite.setTargetChannelName(rs.getString("target_channel_name"));

        // Determine invite type based on properties
        if (invite.getMaxUses() == -1 && invite.getExpiresAt() == null) {
            invite.setType(InviteType.PERMANENT);
        } else if (invite.getMaxUses() == 1) {
            invite.setType(InviteType.ONE_TIME);
        } else if (invite.getMaxUses() > 1) {
            invite.setType(InviteType.LIMITED);
        } else if (invite.getExpiresAt() != null) {
            invite.setType(InviteType.EXPIRING);
        } else {
            invite.setType(InviteType.ONE_TIME);
        }

        return invite;
    }
}