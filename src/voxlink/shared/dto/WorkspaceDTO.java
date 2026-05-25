package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a server or workspace in the system
 */
public class WorkspaceDTO implements Serializable {

    private static final long serialVersionUID = 1l;

    // Core workspace information
    private int id;
    private String name;
    private String description;
    private String iconUrl;

    // Ownership and creation
    private int ownerId;
    private String ownerUsername;
    private LocalDateTime createdAt;

    // Member information
    private int memberCount;
    private List<Integer> membersId;

    // Channel information
    private List<ChannelDTO> channels;

    // Invite information
    private String inviteCode;
    private LocalDateTime inviteExpiresAt;
    private int maxInviteUses;
    private int inviteUsesSoFar;

    // Settings
    private boolean isPublic;
    private String defaultChannelName;

    private UserRoleInWorkspace userRole;

    // Default constructor
    public WorkspaceDTO() {
        this.channels = new ArrayList<>();
        this.membersId = new ArrayList<>();
        this.isPublic = false;
        this.memberCount = 0;
    }

    // Constructor for basic workspace creation
    public WorkspaceDTO(int id, String name, int ownerId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public List<Integer> getMembersId() {
        return membersId;
    }

    public void setMembersId(List<Integer> membersId) {
        this.membersId = membersId;
    }

    public List<ChannelDTO> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelDTO> channels) {
        this.channels = channels;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public LocalDateTime getInviteExpiresAt() {
        return inviteExpiresAt;
    }

    public void setInviteExpiresAt(LocalDateTime inviteExpiresAt) {
        this.inviteExpiresAt = inviteExpiresAt;
    }

    public int getMaxInviteUses() {
        return maxInviteUses;
    }

    public void setMaxInviteUses(int maxInviteUses) {
        this.maxInviteUses = maxInviteUses;
    }

    public int getInviteUsesSoFar() {
        return inviteUsesSoFar;
    }

    public void setInviteUsesSoFar(int inviteUsesSoFar) {
        this.inviteUsesSoFar = inviteUsesSoFar;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getDefaultChannelName() {
        return defaultChannelName;
    }

    public void setDefaultChannelName(String defaultChannelName) {
        this.defaultChannelName = defaultChannelName;
    }

    public UserRoleInWorkspace getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRoleInWorkspace userRole) {
        this.userRole = userRole;
    }

    // Add member id
    public void addMemberId(int userId) {
        if(this.membersId == null) this.membersId = new ArrayList<>();

        this.membersId.add(userId);
        this.memberCount = this.membersId.size();
    }

    // Add channel
    public void addChannel(ChannelDTO channel) {
        if(this.channels == null) this.channels = new ArrayList<>();

        this.channels.add(channel);
    }

    // Check is user is owner of the workspace
    public boolean isOwner(int userId) {
        return this.ownerId == userId;
    }

    // Check if invite is still valid
    public boolean isInviteValid() {
        if(inviteCode.isEmpty()) return false;
        if(inviteExpiresAt != null && LocalDateTime.now().isAfter(inviteExpiresAt)) return false;
        if(maxInviteUses > 0 && inviteUsesSoFar >= maxInviteUses) return false;

        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WorkspaceDTO that = (WorkspaceDTO) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "WorkspaceDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", memberCount=" + memberCount +
                ", channels=" + (channels != null ? channels.size() : 0) +
                '}';
    }
}
