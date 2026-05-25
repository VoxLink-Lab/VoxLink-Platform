package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a single recorded action in the system.
 */
public class AuditLogEntryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Core entry information
    private int id;
    private AuditActionType actionType;
    private String description;

    // Who performed the action
    private int actorId;
    private String actorUsername;
    private String actorDisplayName;

    // Who/what was affected (can be null)
    private Integer targetUserId;
    private String targetUsername;
    private Integer targetChannelId;
    private String targetChannelName;
    private Integer targetWorkspaceId;
    private String targetWorkspaceName;

    // Context
    private int workspaceId;
    private String workspaceName;
    private Integer channelId;

    // Timestamp
    private LocalDateTime timestamp;

    // Additional data (JSON or key-value)
    private String additionalData;

    // IP tracking (for security)
    private String ipAddress;

    // Status
    private boolean wasSuccessful;
    private String failureReason;

    // Default constructor
    public AuditLogEntryDTO() {
        this.timestamp = LocalDateTime.now();
        this.wasSuccessful = true;
    }


    // Constructor for creating a new audit log entry
    public AuditLogEntryDTO(AuditActionType actionType, int actorId, String actorUsername,
                            int workspaceId, String description) {
        this();
        this.actionType = actionType;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.workspaceId = workspaceId;
        this.description = description;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public void setActionType(AuditActionType actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getActorId() {
        return actorId;
    }

    public void setActorId(int actorId) {
        this.actorId = actorId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getActorDisplayName() {
        return actorDisplayName != null ? actorDisplayName : actorUsername;
    }

    public void setActorDisplayName(String actorDisplayName) {
        this.actorDisplayName = actorDisplayName;
    }

    public Integer getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Integer targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public Integer getTargetChannelId() {
        return targetChannelId;
    }

    public void setTargetChannelId(Integer targetChannelId) {
        this.targetChannelId = targetChannelId;
    }

    public String getTargetChannelName() {
        return targetChannelName;
    }

    public void setTargetChannelName(String targetChannelName) {
        this.targetChannelName = targetChannelName;
    }

    public Integer getTargetWorkspaceId() {
        return targetWorkspaceId;
    }

    public void setTargetWorkspaceId(Integer targetWorkspaceId) {
        this.targetWorkspaceId = targetWorkspaceId;
    }

    public String getTargetWorkspaceName() {
        return targetWorkspaceName;
    }

    public void setTargetWorkspaceName(String targetWorkspaceName) {
        this.targetWorkspaceName = targetWorkspaceName;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isWasSuccessful() {
        return wasSuccessful;
    }

    public void setWasSuccessful(boolean wasSuccessful) {
        this.wasSuccessful = wasSuccessful;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }


    // Generate a formatted string for CSV export
    public String toCsvString() {
        return String.format("%d,%s,%s,%d,%s,%s,%s,%s,%s,%b",
                id,
                timestamp,
                actionType.getCode(),
                actorId,
                actorUsername,
                targetUserId != null ? targetUserId : "",
                targetUsername != null ? targetUsername : "",
                workspaceName,
                description,
                wasSuccessful
        );
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (by %s)",
                timestamp, actionType.getDisplayName(), description, actorUsername);
    }
}