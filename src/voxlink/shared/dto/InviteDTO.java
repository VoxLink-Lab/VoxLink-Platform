package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents an invitation link for joining a workspace.
 */
public class InviteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Core invite information
    private int id;
    private String inviteCode;
    private int workspaceId;
    private String workspaceName;

    // Creator information
    private int createdBy;
    private String createdByUsername;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Usage limits
    private int maxUses;
    private int usesSoFar;

    // Settings
    private InviteType type;
    private boolean isActive;

    // Additional metadata
    private Integer invitedUserId;
    private String targetChannelName;

    // Default constructor
    public InviteDTO() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.maxUses = 1;
        this.usesSoFar = 0;
        this.type = InviteType.ONE_TIME;
    }

    // Constructor for creating a new invite code
    public InviteDTO(String inviteCode, int workspaceId, int createdBy, InviteType type) {
        this();
        this.inviteCode = inviteCode;
        this.workspaceId = workspaceId;
        this.createdBy = createdBy;
        this.type = type;

        // Set defaults based on type
        switch (type) {
            case PERMANENT:
                this.maxUses = -1;
                this.expiresAt = null;
                break;
            case ONE_TIME:
                this.maxUses = 1;
                this.expiresAt = null;
                break;
            case LIMITED:
                this.maxUses = 10;
                this.expiresAt = null;
                break;
            case EXPIRING:
                this.maxUses = -1;
                this.expiresAt = LocalDateTime.now().plusDays(7);
                break;
        }
    }

    // Getter and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
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

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }

    public int getUsesSoFar() {
        return usesSoFar;
    }

    public void setUsesSoFar(int usesSoFar) {
        this.usesSoFar = usesSoFar;
    }

    public InviteType getType() {
        return type;
    }

    public void setType(InviteType type) {
        this.type = type;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Integer getInvitedUserId() {
        return invitedUserId;
    }

    public void setInvitedUserId(Integer invitedUserId) {
        this.invitedUserId = invitedUserId;
    }

    public String getTargetChannelName() {
        return targetChannelName;
    }

    public void setTargetChannelName(String targetChannelName) {
        this.targetChannelName = targetChannelName;
    }

    // Check if invite code is still valid
    public boolean isValid() {
        if (!isActive) {
            return false;
        }

        // Check expiration
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }

        // Check usage limit
        if (maxUses > 0 && usesSoFar >= maxUses) {
            return false;
        }

        return true;
    }

    // Get full invite url
    public String getInviteUrl(String serverBaseUrl) {
        return serverBaseUrl + "/invite/" + inviteCode;
    }

    // Increment usage count
    public void incrementUsage() {
        this.usesSoFar++;

        // Auto-deactivate if max uses reached
        if (maxUses > 0 && usesSoFar >= maxUses) {
            this.isActive = false;
        }
    }

    // Get remaining use for invite code
    public int getRemainingUses() {
        if (maxUses <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, maxUses - usesSoFar);
    }

    // Get time until expiration
    public long getHoursUntilExpiration() {
        if (expiresAt == null) {
            return -1;
        }
        return Math.max(0, java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        InviteDTO that = (InviteDTO) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "InviteDTO{" +
                "inviteCode='" + inviteCode + '\'' +
                ", workspaceId=" + workspaceId +
                ", type=" + type +
                ", isValid=" + isValid() +
                ", usesRemaining=" + getRemainingUses() +
                '}';
    }
}