package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a user in the system that can be sent between client and server
 */
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1l;

    // Core user information
    private int id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;

    // User status information
    private UserStatus status;
    private String customStatus;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

    // Role and permission data
    private Set<RoleDTO> roles;

    // Session information
    private String authToken;
    private int activeWorkspaceId;
    private int activeChannelId;

    // Default constructor
    public UserDTO() {
        this.roles = new HashSet<>();
        this.status = UserStatus.OFFLINE;
    }

    // Constructor for basic user creation
    public UserDTO(int id, String username, String email, String displayName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
    }

    // Getters and Setters


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getCustomStatus() {
        return customStatus;
    }

    public void setCustomStatus(String customStatus) {
        this.customStatus = customStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public Set<RoleDTO> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleDTO> roles) {
        this.roles = roles;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public int getActiveWorkspaceId() {
        return activeWorkspaceId;
    }

    public void setActiveWorkspaceId(int activeWorkspaceId) {
        this.activeWorkspaceId = activeWorkspaceId;
    }

    public int getActiveChannelId() {
        return activeChannelId;
    }

    public void setActiveChannelId(int activeChannelId) {
        this.activeChannelId = activeChannelId;
    }

    // Check if user has role in a workspace
    public boolean hasRole(int workspaceId, String roleName) {
        return roles
                .stream()
                .anyMatch(role -> role.getWorkspaceId() == workspaceId &&
                                            role.getRoleName().euqalsIgnoreCase(roleName)
                        );
    }

    // Check if user is admin of a workspace
    public boolean isAdmin(int workspaceId) {
        return hasRole(workspaceId, "ADMIN");
    }

    // Check if user if moderator of a workspace
    public boolean isModerator(int workspaceId) {
        return hasRole(workspaceId, "MODERATOR");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UserDTO userDTO = (UserDTO) obj;
        return id == userDTO.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", status=" + status +
                '}';
    }
}
