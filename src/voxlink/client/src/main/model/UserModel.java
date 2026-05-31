package voxlink.client.src.main.model;

import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.util.function.Consumer;

/**
 * UserModel handles client-side business logic for user operations.
 */
public class UserModel {

    private static UserModel instance;
    private final ServerConnection connection;
    private final UserStore userStore;
    private final AppState appState;

    // Private constructor for singleton pattern
    private UserModel() {
        this.connection = ServerConnection.getInstance();
        this.userStore = UserStore.getInstance();
        this.appState = AppState.getInstance();
    }

    // Get singleton instance
    public static synchronized UserModel getInstance() {
        if (instance == null) {
            instance = new UserModel();
        }
        return instance;
    }

    // Login to the server
    public void login(String username, String password, Consumer<LoginResult> callback) {
        // Create login packet
        Packet packet = new Packet(RequestType.AUTH_LOGIN);
        packet.put("username", username);
        packet.put("password", password);

        // Send packet and handle response asynchronously
        connection.sendPacket(packet);

        // Register temporary listener for response
        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.AUTH_LOGIN_SUCCESS) {
                    UserDTO user = (UserDTO) response.get("user");
                    String authToken = response.get("authToken").toString();

                    if (user != null && authToken != null) {
                        userStore.setCurrentUser(user, authToken);
                        appState.setConnected(true);

                        if (callback != null) {
                            callback.accept(new LoginResult(true, user, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new LoginResult(false, null, "Invalid server response"));
                        }
                    }

                } else if (response.getResponseType() == ResponseType.AUTH_LOGIN_FAILURE) {
                    if (callback != null) {
                        callback.accept(new LoginResult(false, null, response.getErrorMessage()));
                    }
                }

                // Remove self after handling
                connection.removePacketListener(this);
            }
        });
    }

    // Register a new user account
    public void register(String username, String password, String email,
                         String displayName, Consumer<RegisterResult> callback) {
        Packet packet = new Packet(RequestType.AUTH_REGISTER);
        packet.put("username", username);
        packet.put("password", password);
        packet.put("email", email);
        if (displayName != null) {
            packet.put("displayName", displayName);
        }

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.AUTH_REGISTER_SUCCESS) {
                    UserDTO user = (UserDTO) response.get("user");

                    if (callback != null) {
                        callback.accept(new RegisterResult(true, user, null));
                    }

                } else if (response.getResponseType() == ResponseType.AUTH_REGISTER_FAILURE) {
                    if (callback != null) {
                        callback.accept(new RegisterResult(false, null, response.getErrorMessage()));
                    }
                }

                connection.removePacketListener(this);
            }
        });
    }

    // Logout current user
    public void logout() {
        if (userStore.isAuthenticated()) {
            Packet packet = new Packet(RequestType.AUTH_LOGOUT);
            packet.setAuthToken(userStore.getAuthToken());
            packet.setUserId(userStore.getUserId());
            connection.sendPacket(packet);
        }

        userStore.clear();
        appState.logout();
    }

    // Update user profile
    public void updateProfile(String displayName, String avatarUrl,
                              String customStatus, Consumer<Boolean> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) callback.accept(false);
            return;
        }

        Packet packet = new Packet(RequestType.AUTH_GET_PROFILE);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());

        if (displayName != null) packet.put("displayName", displayName);
        if (avatarUrl != null) packet.put("avatarUrl", avatarUrl);
        if (customStatus != null) packet.put("customStatus", customStatus);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                boolean success = response.getResponseType() == ResponseType.AUTH_PROFILE_DATA;
                if (success && callback != null) {
                    UserDTO updatedUser = (UserDTO) response.get("user");
                    if (updatedUser != null) {
                        userStore.setCurrentUser(updatedUser, userStore.getAuthToken());
                    }
                }
                if (callback != null) callback.accept(success);
                connection.removePacketListener(this);
            }
        });
    }

    // Update user status
    public void updateStatus(UserStatus status, Consumer<Boolean> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) callback.accept(false);
            return;
        }

        boolean localSuccess = userStore.updateStatus(status);

        if (callback != null) {
            callback.accept(localSuccess);
        }
    }

    // Get current user
    public UserDTO getCurrentUser() {
        return userStore.getCurrentUser();
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return userStore.isAuthenticated();
    }

    // Get user ID
    public int getUserId() {
        return userStore.getUserId();
    }

    // Get username
    public String getUsername() {
        return userStore.getUsername();
    }

    // Get display name
    public String getDisplayName() {
        return userStore.getDisplayName();
    }

    // Check if user is admin of the workspace
    public boolean isAdminOfWorkspace(int workspaceId) {
        return userStore.isAdminOfWorkspace(workspaceId);
    }

    // Check if user is moderator of the workspace
    public boolean isModeratorOfWorkspace(int workspaceId) {
        return userStore.isModeratorOfWorkspace(workspaceId);
    }

    /**
     * Result class for login operation
     */
    public static class LoginResult {
        private final boolean success;
        private final UserDTO user;
        private final String errorMessage;

        public LoginResult(boolean success, UserDTO user, String errorMessage) {
            this.success = success;
            this.user = user;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public UserDTO getUser() { return user; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Result class for register operation
     */
    public static class RegisterResult {
        private final boolean success;
        private final UserDTO user;
        private final String errorMessage;

        public RegisterResult(boolean success, UserDTO user, String errorMessage) {
            this.success = success;
            this.user = user;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public UserDTO getUser() { return user; }
        public String getErrorMessage() { return errorMessage; }
    }
}