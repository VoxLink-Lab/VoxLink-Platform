package voxlink.client.src.main.model;

import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.WorkspaceDTO;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.util.List;
import java.util.function.Consumer;

/**
 * WorkspaceModel handles client-side business logic for workspace operations.
 */
public class WorkspaceModel {

    private static WorkspaceModel instance;
    private final ServerConnection connection;
    private final UserStore userStore;
    private final AppState appState;

    // Private constructor for singleton pattern
    private WorkspaceModel() {
        this.connection = ServerConnection.getInstance();
        this.userStore = UserStore.getInstance();
        this.appState = AppState.getInstance();
    }

    // Get singleton instance
    public static synchronized WorkspaceModel getInstance() {
        if (instance == null) {
            instance = new WorkspaceModel();
        }
        return instance;
    }

    // Fetch all workspaces for the current user
    public void fetchWorkspaces(Consumer<FetchWorkspacesResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new FetchWorkspacesResult(false, null, "Not authenticated"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.WORKSPACE_LIST);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.WORKSPACE_LIST_DATA) {
                    @SuppressWarnings("unchecked")
                    List<WorkspaceDTO> workspaces = (List<WorkspaceDTO>) response.get("workspaces");

                    if (workspaces != null) {
                        appState.setWorkspaces(workspaces);
                        if (callback != null) {
                            callback.accept(new FetchWorkspacesResult(true, workspaces, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new FetchWorkspacesResult(false, null, "Invalid response"));
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.accept(new FetchWorkspacesResult(false, null, response.getErrorMessage()));
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Join a workspace using invite code
    public void joinWorkspace(String inviteCode, Consumer<JoinWorkspaceResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new JoinWorkspaceResult(false, null, "Not authenticated"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.WORKSPACE_JOIN);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("inviteCode", inviteCode);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.WORKSPACE_JOIN_SUCCESS) {
                    WorkspaceDTO workspace = (WorkspaceDTO) response.get("workspace");

                    if (workspace != null) {
                        appState.addWorkspace(workspace);
                        if (callback != null) {
                            callback.accept(new JoinWorkspaceResult(true, workspace, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new JoinWorkspaceResult(false, null, "Invalid response"));
                        }
                    }
                    connection.removePacketListener(this);
                } else if (response.getResponseType() == ResponseType.WORKSPACE_JOIN_FAILURE) {
                    if (callback != null) {
                        callback.accept(new JoinWorkspaceResult(false, null, response.getErrorMessage()));
                    }
                    connection.removePacketListener(this);
                }
            }
        });
    }

    // Leave a workspace
    public void leaveWorkspace(int workspaceId, Consumer<LeaveWorkspaceResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new LeaveWorkspaceResult(false, "Not authenticated"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.WORKSPACE_LEAVE);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("workspaceId", workspaceId);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.WORKSPACE_LEAVE_SUCCESS) {
                    appState.removeWorkspace(workspaceId);

                    // If leaving current workspace, clear current channel
                    WorkspaceDTO currentWorkspace = appState.getCurrentWorkspace();
                    if (currentWorkspace != null && currentWorkspace.getId() == workspaceId) {
                        appState.setCurrentWorkspace(null);
                        appState.setCurrentChannel(null);
                        appState.setCurrentChannels(null);
                    }

                    if (callback != null) {
                        callback.accept(new LeaveWorkspaceResult(true, null));
                    }
                } else {
                    if (callback != null) {
                        callback.accept(new LeaveWorkspaceResult(false, response.getErrorMessage()));
                    }
                }
                connection.removePacketListener(this);
            }
        });
    }

    // Create a new workspace
    public void createWorkspace(String name, String description, boolean isPublic,
                                Consumer<CreateWorkspaceResult> callback) {
        if (!userStore.isAuthenticated()) {
            if (callback != null) {
                callback.accept(new CreateWorkspaceResult(false, null, "Not authenticated"));
            }
            return;
        }

        Packet packet = new Packet(RequestType.WORKSPACE_CREATE);
        packet.setAuthToken(userStore.getAuthToken());
        packet.setUserId(userStore.getUserId());
        packet.put("name", name);
        if (description != null) {
            packet.put("description", description);
        }
        packet.put("isPublic", isPublic);

        connection.sendPacket(packet);

        connection.addPacketListener(new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet response) {
                if (response.getResponseType() == ResponseType.WORKSPACE_CREATE_SUCCESS) {
                    WorkspaceDTO workspace = (WorkspaceDTO) response.get("workspace");

                    if (workspace != null) {
                        appState.addWorkspace(workspace);
                        if (callback != null) {
                            callback.accept(new CreateWorkspaceResult(true, workspace, null));
                        }
                    } else {
                        if (callback != null) {
                            callback.accept(new CreateWorkspaceResult(false, null, "Invalid response"));
                        }
                    }
                    connection.removePacketListener(this);
                } else if (response.getResponseType() == ResponseType.WORKSPACE_CREATE_FAILURE) {
                    if (callback != null) {
                        callback.accept(new CreateWorkspaceResult(false, null, response.getErrorMessage()));
                    }
                    connection.removePacketListener(this);
                }
            }
        });
    }

    // Set current active workspace
    public void setCurrentWorkspace(WorkspaceDTO workspace) {
        appState.setCurrentWorkspace(workspace);
        if (workspace != null) {
            // Clear current channel when switching workspace
            appState.setCurrentChannel(null);
            // Fetch channels for this workspace
            ChannelModel.getInstance().fetchChannels(workspace.getId(), null);
        }
    }

    // Get current workspace
    public WorkspaceDTO getCurrentWorkspace() {
        return appState.getCurrentWorkspace();
    }

    // Get all workspaces
    public List<WorkspaceDTO> getWorkspaces() {
        return appState.getWorkspaces();
    }

    // Get workspace by ID
    public WorkspaceDTO getWorkspaceById(int workspaceId) {
        return appState.getWorkspaceById(workspaceId);
    }

    // --- RESULT CLASSES ---

    public static class FetchWorkspacesResult {
        private final boolean success;
        private final List<WorkspaceDTO> workspaces;
        private final String errorMessage;// ========== Result Classes ==========

        public FetchWorkspacesResult(boolean success, List<WorkspaceDTO> workspaces, String errorMessage) {
            this.success = success;
            this.workspaces = workspaces;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public List<WorkspaceDTO> getWorkspaces() { return workspaces; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class JoinWorkspaceResult {
        private final boolean success;
        private final WorkspaceDTO workspace;
        private final String errorMessage;

        public JoinWorkspaceResult(boolean success, WorkspaceDTO workspace, String errorMessage) {
            this.success = success;
            this.workspace = workspace;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public WorkspaceDTO getWorkspace() { return workspace; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class LeaveWorkspaceResult {
        private final boolean success;
        private final String errorMessage;

        public LeaveWorkspaceResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class CreateWorkspaceResult {
        private final boolean success;
        private final WorkspaceDTO workspace;
        private final String errorMessage;

        public CreateWorkspaceResult(boolean success, WorkspaceDTO workspace, String errorMessage) {
            this.success = success;
            this.workspace = workspace;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public WorkspaceDTO getWorkspace() { return workspace; }
        public String getErrorMessage() { return errorMessage; }
    }
}