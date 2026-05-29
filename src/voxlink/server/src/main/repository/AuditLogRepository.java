package voxlink.server.src.main.repository;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.AuditActionType;
import voxlink.shared.dto.AuditLogEntryDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditLogRepository handles all database operations for audit logging.
 */
public class AuditLogRepository {

    // Create a new audit log entry
    public AuditLogEntryDTO createLogEntry(AuditActionType actionType, String description,
                                           int actorId, Integer targetUserId,
                                           Integer targetChannelId, Integer targetWorkspaceId,
                                           int workspaceId, String ipAddress,
                                           String additionalData, boolean wasSuccessful,
                                           String failureReason) {
        String sql = "INSERT INTO audit_log (action_type, description, actor_id, target_user_id, " +
                "target_channel_id, target_workspace_id, workspace_id, ip_address, " +
                "additional_data, was_successful, failure_reason, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, actionType.getCode());
            stmt.setString(2, description);
            stmt.setInt(3, actorId);

            if (targetUserId != null) {
                stmt.setInt(4, targetUserId);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }

            if (targetChannelId != null) {
                stmt.setInt(5, targetChannelId);
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }

            if (targetWorkspaceId != null) {
                stmt.setInt(6, targetWorkspaceId);
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }

            stmt.setInt(7, workspaceId);
            stmt.setString(8, ipAddress);
            stmt.setString(9, additionalData);
            stmt.setBoolean(10, wasSuccessful);
            stmt.setString(11, failureReason);
            stmt.setTimestamp(12, new Timestamp(System.currentTimeMillis()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int logId = generatedKeys.getInt(1);
                        System.out.println("[Database] Created audit log entry: " + actionType.getCode() + " (ID: " + logId + ")");
                        return getLogEntryById(logId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to create audit log entry: " + e.getMessage());
        }
        return null;
    }

    // Simplified log entry creation for successful actions
    public AuditLogEntryDTO logAction(AuditActionType actionType, String description,
                                      int actorId, int workspaceId, String ipAddress) {
        return createLogEntry(actionType, description, actorId, null, null, null,
                workspaceId, ipAddress, null, true, null);
    }

    // Log a user kick action
    public AuditLogEntryDTO logUserKick(int actorId, int targetUserId, int workspaceId,
                                        String reason, String ipAddress) {
        String description = "User " + actorId + " kicked user " + targetUserId;
        if (reason != null && !reason.isEmpty()) {
            description += " Reason: " + reason;
        }
        return createLogEntry(AuditActionType.USER_KICK, description, actorId, targetUserId,
                null, null, workspaceId, ipAddress, reason, true, null);
    }

    // Log a user ban action
    public AuditLogEntryDTO logUserBan(int actorId, int targetUserId, int workspaceId,
                                       String reason, String ipAddress) {
        String description = "User " + actorId + " banned user " + targetUserId;
        if (reason != null && !reason.isEmpty()) {
            description += " Reason: " + reason;
        }
        return createLogEntry(AuditActionType.USER_BAN, description, actorId, targetUserId,
                null, null, workspaceId, ipAddress, reason, true, null);
    }

    // Log a channel creation
    public AuditLogEntryDTO logChannelCreate(int actorId, int channelId, String channelName,
                                             int workspaceId, String ipAddress) {
        String description = "User " + actorId + " created channel: " + channelName;
        return createLogEntry(AuditActionType.CHANNEL_CREATE, description, actorId, null,
                channelId, null, workspaceId, ipAddress, channelName, true, null);
    }

    // Log a channel deletion
    public AuditLogEntryDTO logChannelDelete(int actorId, int channelId, String channelName,
                                             int workspaceId, String ipAddress) {
        String description = "User " + actorId + " deleted channel: " + channelName;
        return createLogEntry(AuditActionType.CHANNEL_DELETE, description, actorId, null,
                channelId, null, workspaceId, ipAddress, channelName, true, null);
    }

    // Log a role assignment
    public AuditLogEntryDTO logRoleAssign(int actorId, int targetUserId, String roleName,
                                          int workspaceId, String ipAddress) {
        String description = "User " + actorId + " assigned role " + roleName + " to user " + targetUserId;
        return createLogEntry(AuditActionType.ROLE_ASSIGN, description, actorId, targetUserId,
                null, null, workspaceId, ipAddress, roleName, true, null);
    }

    // Log a failed action
    public AuditLogEntryDTO logFailedAction(AuditActionType actionType, String description,
                                            int actorId, int workspaceId, String ipAddress,
                                            String failureReason) {
        return createLogEntry(actionType, description, actorId, null, null, null,
                workspaceId, ipAddress, null, false, failureReason);
    }

    // Get log entry by ID
    public AuditLogEntryDTO getLogEntryById(int logId) {
        String sql = "SELECT al.id, al.action_type, al.description, al.actor_id, " +
                "al.target_user_id, al.target_channel_id, al.target_workspace_id, " +
                "al.workspace_id, al.ip_address, al.additional_data, al.was_successful, " +
                "al.failure_reason, al.timestamp, " +
                "u1.username as actor_username, u1.display_name as actor_display_name, " +
                "u2.username as target_username, " +
                "w.name as workspace_name " +
                "FROM audit_log al " +
                "LEFT JOIN users u1 ON al.actor_id = u1.id " +
                "LEFT JOIN users u2 ON al.target_user_id = u2.id " +
                "LEFT JOIN workspaces w ON al.workspace_id = w.id " +
                "WHERE al.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, logId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuditLogEntryDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get log entry by ID: " + e.getMessage());
        }
        return null;
    }

    // Get audit logs for a specific workspace (paginated)
    public List<AuditLogEntryDTO> getLogsByWorkspace(int workspaceId, int limit, int offset) {
        String sql = "SELECT al.id, al.action_type, al.description, al.actor_id, " +
                "al.target_user_id, al.target_channel_id, al.target_workspace_id, " +
                "al.workspace_id, al.ip_address, al.additional_data, al.was_successful, " +
                "al.failure_reason, al.timestamp, " +
                "u1.username as actor_username, u1.display_name as actor_display_name, " +
                "u2.username as target_username, " +
                "w.name as workspace_name " +
                "FROM audit_log al " +
                "LEFT JOIN users u1 ON al.actor_id = u1.id " +
                "LEFT JOIN users u2 ON al.target_user_id = u2.id " +
                "LEFT JOIN workspaces w ON al.workspace_id = w.id " +
                "WHERE al.workspace_id = ? " +
                "ORDER BY al.timestamp DESC LIMIT ? OFFSET ?";

        List<AuditLogEntryDTO> logs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLogEntryDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get logs by workspace: " + e.getMessage());
        }
        return logs;
    }

    // Get audit logs by actor
    public List<AuditLogEntryDTO> getLogsByActor(int actorId, int limit) {
        String sql = "SELECT al.id, al.action_type, al.description, al.actor_id, " +
                "al.target_user_id, al.target_channel_id, al.target_workspace_id, " +
                "al.workspace_id, al.ip_address, al.additional_data, al.was_successful, " +
                "al.failure_reason, al.timestamp, " +
                "u1.username as actor_username, u1.display_name as actor_display_name, " +
                "u2.username as target_username, " +
                "w.name as workspace_name " +
                "FROM audit_log al " +
                "LEFT JOIN users u1 ON al.actor_id = u1.id " +
                "LEFT JOIN users u2 ON al.target_user_id = u2.id " +
                "LEFT JOIN workspaces w ON al.workspace_id = w.id " +
                "WHERE al.actor_id = ? " +
                "ORDER BY al.timestamp DESC LIMIT ?";

        List<AuditLogEntryDTO> logs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, actorId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLogEntryDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get logs by actor: " + e.getMessage());
        }
        return logs;
    }

    // Get audit logs by action type
    public List<AuditLogEntryDTO> getLogsByActionType(AuditActionType actionType, int limit) {
        String sql = "SELECT al.id, al.action_type, al.description, al.actor_id, " +
                "al.target_user_id, al.target_channel_id, al.target_workspace_id, " +
                "al.workspace_id, al.ip_address, al.additional_data, al.was_successful, " +
                "al.failure_reason, al.timestamp, " +
                "u1.username as actor_username, u1.display_name as actor_display_name, " +
                "u2.username as target_username, " +
                "w.name as workspace_name " +
                "FROM audit_log al " +
                "LEFT JOIN users u1 ON al.actor_id = u1.id " +
                "LEFT JOIN users u2 ON al.target_user_id = u2.id " +
                "LEFT JOIN workspaces w ON al.workspace_id = w.id " +
                "WHERE al.action_type = ? " +
                "ORDER BY al.timestamp DESC LIMIT ?";

        List<AuditLogEntryDTO> logs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, actionType.getCode());
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLogEntryDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get logs by action type: " + e.getMessage());
        }
        return logs;
    }

    // Get audit logs within a date range
    public List<AuditLogEntryDTO> getLogsByDateRange(int workspaceId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT al.id, al.action_type, al.description, al.actor_id, " +
                "al.target_user_id, al.target_channel_id, al.target_workspace_id, " +
                "al.workspace_id, al.ip_address, al.additional_data, al.was_successful, " +
                "al.failure_reason, al.timestamp, " +
                "u1.username as actor_username, u1.display_name as actor_display_name, " +
                "u2.username as target_username, " +
                "w.name as workspace_name " +
                "FROM audit_log al " +
                "LEFT JOIN users u1 ON al.actor_id = u1.id " +
                "LEFT JOIN users u2 ON al.target_user_id = u2.id " +
                "LEFT JOIN workspaces w ON al.workspace_id = w.id " +
                "WHERE al.workspace_id = ? AND al.timestamp BETWEEN ? AND ? " +
                "ORDER BY al.timestamp DESC";

        List<AuditLogEntryDTO> logs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate));
            stmt.setTimestamp(3, Timestamp.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLogEntryDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get logs by date range: " + e.getMessage());
        }
        return logs;
    }

    // Get all audit logs for export (no pagination limit)
    public List<AuditLogEntryDTO> getAllLogsForExport(int workspaceId) {
        String sql = "SELECT al.id, al.action_type, al.description, al.actor_id, " +
                "al.target_user_id, al.target_channel_id, al.target_workspace_id, " +
                "al.workspace_id, al.ip_address, al.additional_data, al.was_successful, " +
                "al.failure_reason, al.timestamp, " +
                "u1.username as actor_username, u1.display_name as actor_display_name, " +
                "u2.username as target_username, " +
                "w.name as workspace_name " +
                "FROM audit_log al " +
                "LEFT JOIN users u1 ON al.actor_id = u1.id " +
                "LEFT JOIN users u2 ON al.target_user_id = u2.id " +
                "LEFT JOIN workspaces w ON al.workspace_id = w.id " +
                "WHERE al.workspace_id = ? " +
                "ORDER BY al.timestamp DESC";

        List<AuditLogEntryDTO> logs = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLogEntryDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get all logs for export: " + e.getMessage());
        }
        return logs;
    }

    // Get count of audit logs for a workspace
    public int getLogCount(int workspaceId) {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE workspace_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get log count: " + e.getMessage());
        }
        return 0;
    }

    // Delete audit logs older than specified days
    public int cleanupOldLogs(int daysToKeep) {
        String sql = "DELETE FROM audit_log WHERE timestamp < DATE_SUB(NOW(), INTERVAL ? DAY)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, daysToKeep);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Cleaned up " + deleted + " old audit logs");
            }
            return deleted;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to cleanup old logs: " + e.getMessage());
        }
        return 0;
    }

    // Delete all audit logs for a workspace
    public int deleteLogsByWorkspace(int workspaceId) {
        String sql = "DELETE FROM audit_log WHERE workspace_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Deleted " + deleted + " audit logs from workspace " + workspaceId);
            }
            return deleted;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete logs by workspace: " + e.getMessage());
        }
        return 0;
    }

    //  Generate CSV formatted string from audit logs
    public String exportToCSV(List<AuditLogEntryDTO> logs) {
        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("ID,Timestamp,Action Type,Actor ID,Actor Username,Target User ID,Target Username,Workspace,Description,Successful\n");

        // CSV Rows
        for (AuditLogEntryDTO log : logs) {
            csv.append(log.getId()).append(",");
            csv.append(log.getTimestamp()).append(",");
            csv.append(log.getActionType().getCode()).append(",");
            csv.append(log.getActorId()).append(",");
            csv.append(escapeCSV(log.getActorUsername())).append(",");
            csv.append(log.getTargetUserId() != null ? log.getTargetUserId() : "").append(",");
            csv.append(escapeCSV(log.getTargetUsername())).append(",");
            csv.append(escapeCSV(log.getWorkspaceName())).append(",");
            csv.append(escapeCSV(log.getDescription())).append(",");
            csv.append(log.isWasSuccessful() ? "YES" : "NO").append("\n");
        }

        return csv.toString();
    }

    // Escape special characters for CSV format
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // Map ResultSet row to AuditLogEntryDTO object
    private AuditLogEntryDTO mapResultSetToAuditLogEntryDTO(ResultSet rs) throws SQLException {
        AuditLogEntryDTO log = new AuditLogEntryDTO();
        log.setId(rs.getInt("id"));

        String actionTypeCode = rs.getString("action_type");
        AuditActionType actionType = AuditActionType.fromCode(actionTypeCode);
        log.setActionType(actionType != null ? actionType : AuditActionType.SERVER_START);

        log.setDescription(rs.getString("description"));
        log.setActorId(rs.getInt("actor_id"));
        log.setActorUsername(rs.getString("actor_username"));
        log.setActorDisplayName(rs.getString("actor_display_name"));

        int targetUserId = rs.getInt("target_user_id");
        if (!rs.wasNull()) {
            log.setTargetUserId(targetUserId);
            log.setTargetUsername(rs.getString("target_username"));
        }

        int targetChannelId = rs.getInt("target_channel_id");
        if (!rs.wasNull()) {
            log.setTargetChannelId(targetChannelId);
        }

        int targetWorkspaceId = rs.getInt("target_workspace_id");
        if (!rs.wasNull()) {
            log.setTargetWorkspaceId(targetWorkspaceId);
        }

        log.setWorkspaceId(rs.getInt("workspace_id"));
        log.setWorkspaceName(rs.getString("workspace_name"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setAdditionalData(rs.getString("additional_data"));
        log.setWasSuccessful(rs.getBoolean("was_successful"));
        log.setFailureReason(rs.getString("failure_reason"));

        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            log.setTimestamp(timestamp.toLocalDateTime());
        }

        return log;
    }
}