package voxlink.client.src.main.state;

import voxlink.client.src.main.network.ServerConnection;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

/**
 * UserStore manages the current user's data and authentication state.
 * */
public class UserStore {

    private static UserStore instance;

    private UserDTO currentUser;
    private String authToken;
    private boolean isAuthenticated;

    // Private constructor for singleton pattern
    private UserStore() {
        this.isAuthenticated = false;
    }

    // Get singleton instance
    public static synchronized UserStore getInstance() {
        if (instance == null) {
            instance = new UserStore();
        }
        return instance;
    }

    // Set current user after successful login
    public void setCurrentUser(UserDTO user, String token) {
        this.currentUser = user;
        this.authToken = token;
        this.isAuthenticated = true;

        // Also update AppState
        AppState.getInstance().login(user, token);
    }

    // Update user profile
    public boolean updateProfile(String displayName, String avatarUrl, String customStatus) {
        if (!isAuthenticated || currentUser == null) {
            System.err.println("[UserStore] Cannot update profile: not authenticated");
            return false;
        }

        Packet packet = new Packet(RequestType.AUTH_GET_PROFILE);
        packet.setAuthToken(authToken);
        packet.setUserId(currentUser.getId());

        if (displayName != null) {
            packet.put("displayName", displayName);
        }
        if (avatarUrl != null) {
            packet.put("avatarUrl", avatarUrl);
        }
        if (customStatus != null) {
            packet.put("customStatus", customStatus);
        }

        ServerConnection.getInstance().sendPacket(packet);

        // Update local user data
        if (displayName != null) {
            currentUser.setDisplayName(displayName);
        }
        if (avatarUrl != null) {
            currentUser.setAvatarUrl(avatarUrl);
        }
        if (customStatus != null) {
            currentUser.setCustomStatus(customStatus);
        }

        System.out.println("[UserStore] Profile updated for: " + currentUser.getUsername());
        return true;
    }

    // Update user status
    public boolean updateStatus(UserStatus status) {
        if (!isAuthenticated || currentUser == null) {
            System.err.println("[UserStore] Cannot update status: not authenticated");
            return false;
        }

        Packet packet = new Packet(RequestType.USER_UPDATE_STATUS);
        packet.setAuthToken(authToken);
        packet.setUserId(currentUser.getId());
        packet.put("status", status.name());

        ServerConnection.getInstance().sendPacket(packet);

        // Update local user data
        currentUser.setStatus(status);

        System.out.println("[UserStore] Status updated to: " + status.getDisplayName());
        return true;
    }

    // Get current user
    public UserDTO getCurrentUser() {
        return currentUser;
    }

    // Get auth token
    public String getAuthToken() {
        return authToken;
    }

    // Check if user is authenticated
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    // Get user ID
    public int getUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    // Get username
    public String getUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    // Get display name (falls back to username)
    public String getDisplayName() {
        if (currentUser == null) {
            return null;
        }
        return currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : currentUser.getUsername();
    }

    // Get current user status
    public UserStatus getCurrentStatus() {
        return currentUser != null ? currentUser.getStatus() : UserStatus.OFFLINE;
    }

    // Clear user data on logout
    public void clear() {
        this.currentUser = null;
        this.authToken = null;
        this.isAuthenticated = false;
        System.out.println("[UserStore] User data cleared");
    }

    // Handle profile update response from server
    public void handleProfileUpdateResponse(Packet packet) {
        if (packet.getResponseType() == ResponseType.AUTH_PROFILE_DATA) {
            UserDTO updatedUser = (UserDTO) packet.get("user");
            if (updatedUser != null && currentUser != null &&
                    updatedUser.getId() == currentUser.getId()) {
                this.currentUser = updatedUser;
                System.out.println("[UserStore] Profile updated from server");
            }
        }
    }

    // Check if user has a specific role in a workspace
    public boolean hasRole(int workspaceId, String roleName) {
        if (currentUser == null) {
            return false;
        }
        return currentUser.hasRole(workspaceId, roleName);
    }

    // Check if user is admin of a workspace
    public boolean isAdminOfWorkspace(int workspaceId) {
        return hasRole(workspaceId, "ADMIN");
    }

    // Check if user is moderator of a workspace
    public boolean isModeratorOfWorkspace(int workspaceId) {
        return hasRole(workspaceId, "MODERATOR") || isAdminOfWorkspace(workspaceId);
    }
}