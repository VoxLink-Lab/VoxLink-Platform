package voxlink.client.src.main.state;

import voxlink.shared.dto.ChannelDTO;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;
import voxlink.shared.dto.WorkspaceDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AppState manages the global state of the VoxLink client application.
 */
public class AppState {

    private static AppState instance;

    // State variables
    private UserDTO currentUser;
    private String authToken;
    private boolean isLoggedIn;
    private boolean isConnected;

    private WorkspaceDTO currentWorkspace;
    private ChannelDTO currentChannel;

    private List<WorkspaceDTO> workspaces;
    private List<ChannelDTO> currentChannels;
    private List<UserDTO> currentChannelMembers;

    // Listeners for state changes
    private final List<StateListener> listeners;

    // Private constructor for singleton pattern
    private AppState() {
        this.workspaces = new ArrayList<>();
        this.currentChannels = new ArrayList<>();
        this.currentChannelMembers = new ArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.isLoggedIn = false;
        this.isConnected = false;
    }

    // Get singleton instance
    public static synchronized AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    public void login(UserDTO user, String token) {
        this.currentUser = user;
        this.authToken = token;
        this.isLoggedIn = true;
        notifyLoginStateChanged(true);
        System.out.println("[AppState] User logged in: " + user.getUsername());
    }

    public void logout() {
        this.currentUser = null;
        this.authToken = null;
        this.isLoggedIn = false;
        this.currentWorkspace = null;
        this.currentChannel = null;
        this.workspaces.clear();
        this.currentChannels.clear();
        this.currentChannelMembers.clear();
        notifyLoginStateChanged(false);
        System.out.println("[AppState] User logged out");
    }

    // Set connection status
    public void setConnected(boolean connected) {
        this.isConnected = connected;
        notifyConnectionStateChanged(connected);
    }

    // --- WORKSPACE METHODS ---

    // Set user's workspaces
    public void setWorkspaces(List<WorkspaceDTO> workspaces) {
        this.workspaces = workspaces;
        notifyWorkspacesChanged();
    }

    // Add a workspace to the list
    public void addWorkspace(WorkspaceDTO workspace) {
        this.workspaces.add(workspace);
        notifyWorkspacesChanged();
    }

    // Remove a workspace from the list
    public void removeWorkspace(int workspaceId) {
        this.workspaces.removeIf(w -> w.getId() == workspaceId);
        if (currentWorkspace != null && currentWorkspace.getId() == workspaceId) {
            setCurrentWorkspace(null);
        }
        notifyWorkspacesChanged();
    }

    // Set current active workspace
    public void setCurrentWorkspace(WorkspaceDTO workspace) {
        this.currentWorkspace = workspace;
        notifyCurrentWorkspaceChanged(workspace);
        System.out.println("[AppState] Current workspace: " + (workspace != null ? workspace.getName() : "none"));
    }

    // Get current active workspace
    public WorkspaceDTO getCurrentWorkspace() {
        return currentWorkspace;
    }

    // Get workspace by ID
    public WorkspaceDTO getWorkspaceById(int workspaceId) {
        return workspaces.stream()
                .filter(w -> w.getId() == workspaceId)
                .findFirst()
                .orElse(null);
    }

    // --- CHANNEL METHODS ---

    // Set channels for current workspace
    public void setCurrentChannels(List<ChannelDTO> channels) {
        this.currentChannels = channels;
        notifyChannelsChanged();
    }

    // Add a channel to current workspace
    public void addChannel(ChannelDTO channel) {
        this.currentChannels.add(channel);
        notifyChannelsChanged();
    }

    // Remove a channel from current workspace
    public void removeChannel(int channelId) {
        this.currentChannels.removeIf(c -> c.getId() == channelId);
        if (currentChannel != null && currentChannel.getId() == channelId) {
            setCurrentChannel(null);
        }
        notifyChannelsChanged();
    }

    // Set current active channel
    public void setCurrentChannel(ChannelDTO channel) {
        this.currentChannel = channel;
        notifyCurrentChannelChanged(channel);
        System.out.println("[AppState] Current channel: " + (channel != null ? channel.getName() : "none"));
    }

    // Get current active channel
    public ChannelDTO getCurrentChannel() {
        return currentChannel;
    }

    // Get channel by ID
    public ChannelDTO getChannelById(int channelId) {
        return currentChannels.stream()
                .filter(c -> c.getId() == channelId)
                .findFirst()
                .orElse(null);
    }

    // --- CHANNEL MEMBER METHODS ---

    // Set members of current channel
    public void setCurrentChannelMembers(List<UserDTO> members) {
        this.currentChannelMembers = members;
        notifyChannelMembersChanged();
    }

    // Add a member to current channel
    public void addChannelMember(UserDTO user) {
        if (!currentChannelMembers.contains(user)) {
            currentChannelMembers.add(user);
            notifyChannelMembersChanged();
        }
    }

    // Remove a member from current channel
    public void removeChannelMember(int userId) {
        currentChannelMembers.removeIf(u -> u.getId() == userId);
        notifyChannelMembersChanged();
    }

    // Update member status
    public void updateMemberStatus(int userId, UserStatus status) {
        for (UserDTO user : currentChannelMembers) {
            if (user.getId() == userId) {
                user.setStatus(status);
                break;
            }
        }
        notifyChannelMembersChanged();
    }

    // --- GETTERS ---

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public String getAuthToken() {
        return authToken;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public List<WorkspaceDTO> getWorkspaces() {
        return workspaces;
    }

    public List<ChannelDTO> getCurrentChannels() {
        return currentChannels;
    }

    public List<UserDTO> getCurrentChannelMembers() {
        return currentChannelMembers;
    }

    // --- LISTENER MANAGEMENT ---

    // Add a state change listener
    public void addListener(StateListener listener) {
        listeners.add(listener);
    }

    // Remove a state change listener
    public void removeListener(StateListener listener) {
        listeners.remove(listener);
    }

    // --- NOTIFICATION METHODS ---

    private void notifyLoginStateChanged(boolean loggedIn) {
        for (StateListener listener : listeners) {
            listener.onLoginStateChanged(loggedIn);
        }
    }

    private void notifyConnectionStateChanged(boolean connected) {
        for (StateListener listener : listeners) {
            listener.onConnectionStateChanged(connected);
        }
    }

    private void notifyWorkspacesChanged() {
        for (StateListener listener : listeners) {
            listener.onWorkspacesChanged(workspaces);
        }
    }

    private void notifyCurrentWorkspaceChanged(WorkspaceDTO workspace) {
        for (StateListener listener : listeners) {
            listener.onCurrentWorkspaceChanged(workspace);
        }
    }

    private void notifyChannelsChanged() {
        for (StateListener listener : listeners) {
            listener.onChannelsChanged(currentChannels);
        }
    }

    private void notifyCurrentChannelChanged(ChannelDTO channel) {
        for (StateListener listener : listeners) {
            listener.onCurrentChannelChanged(channel);
        }
    }

    private void notifyChannelMembersChanged() {
        for (StateListener listener : listeners) {
            listener.onChannelMembersChanged(currentChannelMembers);
        }
    }

    // Interface for state change listeners
    public interface StateListener {
        void onLoginStateChanged(boolean loggedIn);
        void onConnectionStateChanged(boolean connected);
        void onWorkspacesChanged(List<WorkspaceDTO> workspaces);
        void onCurrentWorkspaceChanged(WorkspaceDTO workspace);
        void onChannelsChanged(List<ChannelDTO> channels);
        void onCurrentChannelChanged(ChannelDTO channel);
        void onChannelMembersChanged(List<UserDTO> members);
    }

    // Adapter class for StateListener (convenience for implementing only needed methods)
    public static class StateAdapter implements StateListener {
        @Override public void onLoginStateChanged(boolean loggedIn) {}
        @Override public void onConnectionStateChanged(boolean connected) {}
        @Override public void onWorkspacesChanged(List<WorkspaceDTO> workspaces) {}
        @Override public void onCurrentWorkspaceChanged(WorkspaceDTO workspace) {}
        @Override public void onChannelsChanged(List<ChannelDTO> channels) {}
        @Override public void onCurrentChannelChanged(ChannelDTO channel) {}
        @Override public void onChannelMembersChanged(List<UserDTO> members) {}
    }
}