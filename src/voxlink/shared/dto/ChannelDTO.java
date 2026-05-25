package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a communication channel within a workspace.
 */
public class ChannelDTO implements Serializable {

    private static final long serialVersionUID = 1l;

    // Core channel information
    private int id;
    private String name;
    private String description;
    private int workspaceId;
    private String workspaceName;

    // Channel type
    private ChannelType type;

    // Settings
    private boolean isArchived;
    private boolean isPrivate;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    // Creator information
    private int createdBy;
    private String createByUsername;

    // Statistics
    private int messageCount;
    private int membersCount;

    // User specific data
    private boolean hasJoined;
    private int unreadCount;

    // Default constructor
    public ChannelDTO() {
        this.isPrivate = false;
        this.hasJoined = false;
        this.isArchived = false;
        this.type = ChannelType.TEXT;
        this.unreadCount = 0;
        this.messageCount = 0;
        this.membersCount = 0;
    }

    // Constructor to create basic channel
    public ChannelDTO(int id, String name, int workspaceId, ChannelType type) {
        this.id = id;
        this.name = name;
        this.workspaceId = workspaceId;
        this.type = type;
    }

    // Getters and Setter


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public ChannelType getType() {
        return type;
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreateByUsername() {
        return createByUsername;
    }

    public void setCreateByUsername(String createByUsername) {
        this.createByUsername = createByUsername;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public int getMembersCount() {
        return membersCount;
    }

    public void setMembersCount(int membersCount) {
        this.membersCount = membersCount;
    }

    public boolean isHasJoined() {
        return hasJoined;
    }

    public void setHasJoined(boolean hasJoined) {
        this.hasJoined = hasJoined;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    // Check if channel is active
    public boolean isActive() {
        return !this.isArchived;
    }

    // Check if channel is text channel
    public boolean isTextChannel() {
        return this.type == ChannelType.TEXT;
    }

    // Check if channel is announcement channel
    public boolean isAnnouncementChannel() {
        return this.type == ChannelType.ANNOUNCEMENT;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChannelDTO that = (ChannelDTO) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "ChannelDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", workspaceId=" + workspaceId +
                ", type=" + type +
                ", membersCount=" + membersCount +
                '}';
    }
}
