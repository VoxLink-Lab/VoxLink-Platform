package voxlink.server.src.main.network;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.repository.*;
import voxlink.shared.dto.*;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ClientHandler manages a single client connection.
 */
public class ClientHandler implements Runnable {

    private static final java.util.concurrent.CopyOnWriteArrayList<ClientHandler> activeClients = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final Socket clientSocket;
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;
    private final PacketProcessor packetProcessor;

    private int userId;
    private String username;
    private String authToken;
    private int currentWorkspaceId;
    private int currentChannelId;
    private boolean isAuthenticated;
    private boolean isRunning;

    // Repositories for database operations
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final RoleRepository roleRepository;
    private final InviteRepository inviteRepository;
    private final AuditLogRepository auditLogRepository;
    private final FileAttachmentRepository fileRepository;

    // Constructor - initializes streams and repositories
    public ClientHandler(Socket socket) throws IOException {
        this.clientSocket = socket;
        this.clientSocket.setSoTimeout(ServerConfig.SOCKET_TIMEOUT_MS);

        // Initialize streams
        this.outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(clientSocket.getInputStream());

        this.packetProcessor = new PacketProcessor(this);

        // Initialize repositories
        this.userRepository = new UserRepository();
        this.workspaceRepository = new WorkspaceRepository();
        this.channelRepository = new ChannelRepository();
        this.messageRepository = new MessageRepository();
        this.roleRepository = new RoleRepository();
        this.inviteRepository = new InviteRepository();
        this.auditLogRepository = new AuditLogRepository();
        this.fileRepository = new FileAttachmentRepository();

        this.isAuthenticated = false;
        this.isRunning = true;
        this.userId = -1;
        this.currentWorkspaceId = -1;
        this.currentChannelId = -1;

        activeClients.add(this);

        System.out.println("[ClientHandler] New client connected from: " + getClientAddress());
    }

    @Override
    public void run() {
        try {
            while (isRunning && !clientSocket.isClosed()) {
                try {
                    // Read packet from client
                    Packet packet = (Packet) inputStream.readObject();

                    if (packet == null) {
                        continue;
                    }

                    // Process the packet
                    Packet response = processPacket(packet);

                    // Send response if not null
                    if (response != null) {
                        sendPacket(response);
                    }

                } catch (SocketTimeoutException e) {
                    // Send heartbeat ping to check if client is still alive
                    sendHeartbeat();
                } catch (EOFException e) {
                    System.out.println("[ClientHandler] Client disconnected: " + getClientAddress());
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("[ClientHandler] Unknown packet type: " + e.getMessage());
                }
            }
        } catch (SocketException e) {
            System.out.println("[ClientHandler] Connection closed: " + getClientAddress());
        } catch (IOException e) {
            System.err.println("[ClientHandler] I/O Error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // Process incoming packet and return response
    private Packet processPacket(Packet packet) {
        RequestType requestType = packet.getRequestType();

        if (requestType == null) {
            Packet errorPacket = new Packet(ResponseType.ERROR_GENERAL);
            errorPacket.error("Invalid request type");
            return errorPacket;
        }

        // Handle authentication requests
        if (requestType == RequestType.AUTH_LOGIN) {
            return handleLogin(packet);
        } else if (requestType == RequestType.AUTH_REGISTER) {
            return handleRegister(packet);
        }

        // All other requests require authentication
        if (!isAuthenticated) {
            Packet errorPacket = new Packet(ResponseType.ERROR_UNAUTHORIZED);
            errorPacket.error("Not authenticated");
            return errorPacket;
        }

        // Route to appropriate handler
        return packetProcessor.process(packet);
    }

    // --- AUTHENTICATION HELPERS ---

    private Packet handleLogin(Packet packet) {
        String username = packet.get("username").toString();
        String password = packet.get("password").toString();

        if (username == null || password == null) {
            Packet response = new Packet(ResponseType.AUTH_LOGIN_FAILURE);
            response.error("Username and password required");
            return response;
        }

        // Authenticate user
        UserDTO user = userRepository.authenticate(username, password);

        if (user == null) {
            Packet response = new Packet(ResponseType.AUTH_LOGIN_FAILURE);
            response.error("Invalid username or password");
            return response;
        }

        // Set client state
        this.userId = user.getId();
        this.username = user.getUsername();
        this.isAuthenticated = true;
        this.authToken = UUID.randomUUID().toString();
        user.setAuthToken(authToken);

        // Update user status to online
        userRepository.updateUserStatus(userId, UserStatus.ONLINE);

        // Log the login
        auditLogRepository.logAction(AuditActionType.USER_LOGIN,
                "User " + username + " logged in", userId, -1, clientSocket.getInetAddress().getHostAddress());

        // Create success response
        Packet response = new Packet(ResponseType.AUTH_LOGIN_SUCCESS);
        response.put("user", user);
        response.put("authToken", authToken);
        response.success();

        System.out.println("[ClientHandler] User logged in: " + username + " (ID: " + userId + ")");

        return response;
    }

    private Packet handleRegister(Packet packet) {
        String username = packet.get("username").toString();
        String password = packet.get("password").toString();
        String email = packet.get("email").toString();
        String displayName = packet.get("displayName").toString();

        if (username == null || password == null || email == null) {
            Packet response = new Packet(ResponseType.AUTH_REGISTER_FAILURE);
            response.error("Username, password, and email required");
            return response;
        }

        // Check if username already exists
        if (userRepository.getUserByUsername(username) != null) {
            Packet response = new Packet(ResponseType.AUTH_REGISTER_FAILURE);
            response.error("Username already taken");
            return response;
        }

        // Check if email already exists
        if (userRepository.getUserByEmail(email) != null) {
            Packet response = new Packet(ResponseType.AUTH_REGISTER_FAILURE);
            response.error("Email already registered");
            return response;
        }

        // Create user (in production, hash the password)
        UserDTO user = userRepository.createUser(username, password, email, displayName);

        if (user == null) {
            Packet response = new Packet(ResponseType.AUTH_REGISTER_FAILURE);
            response.error("Failed to create user");
            return response;
        }

        // Log the registration
        auditLogRepository.logAction(AuditActionType.USER_REGISTER,
                "New user registered: " + username, user.getId(), -1, clientSocket.getInetAddress().getHostAddress());

        Packet response = new Packet(ResponseType.AUTH_REGISTER_SUCCESS);
        response.put("user", user);
        response.success();

        System.out.println("[ClientHandler] New user registered: " + username);

        return response;
    }

    // --- MESSAGE HANDLERS ---

    // Handle sending a message to a channel
    public Packet handleSendMessage(int channelId, String content, int replyToId) {
        // Check if user is in the channel
        if (!channelRepository.isMemberOfChannel(channelId, userId)) {
            Packet response = new Packet(ResponseType.MESSAGE_SEND_FAILURE);
            response.error("You are not a member of this channel");
            return response;
        }

        // Check channel type permission
        ChannelDTO channel = channelRepository.getChannelById(channelId);
        if (channel == null) {
            Packet response = new Packet(ResponseType.MESSAGE_SEND_FAILURE);
            response.error("Channel not found");
            return response;
        }

        if (!channel.getType().isMembersCanSend() &&
                !roleRepository.isAtLeastModerator(userId, channel.getWorkspaceId())) {
            Packet response = new Packet(ResponseType.MESSAGE_SEND_FAILURE);
            response.error("Only moderators can send messages in this channel");
            return response;
        }

        // Send the message
        MessageDTO message = messageRepository.sendMessage(content, channelId, userId,
                MessageType.TEXT, replyToId);

        if (message == null) {
            Packet response = new Packet(ResponseType.MESSAGE_SEND_FAILURE);
            response.error("Failed to send message");
            return response;
        }

        // Update channel last activity
        channelRepository.updateLastActivity(channelId);

        Packet broadcastPacket = new Packet(ResponseType.MESSAGE_BROADCAST);
        broadcastPacket.put("message", message);
        broadcastPacket.success();
        for (ClientHandler client : activeClients) {
            if (client.isAuthenticated() && client.getUserId() != this.userId && channelRepository.isMemberOfChannel(channelId, client.getUserId())) {
                client.sendPacket(broadcastPacket);
            }
        }

        Packet response = new Packet(ResponseType.MESSAGE_SEND_SUCCESS);
        response.put("message", message);
        response.success();

        return response;
    }

    // --- WORKSPACE HANDLERS ---

    // Get all workspaces for the current user
    public Packet handleGetWorkspaces() {
        List<WorkspaceDTO> workspaces = workspaceRepository.getWorkspacesByUser(userId);

        Packet response = new Packet(ResponseType.WORKSPACE_LIST_DATA);
        response.put("workspaces", workspaces);
        response.success();

        return response;
    }

    // Join a workspace by invite code
    public Packet handleJoinWorkspace(String inviteCode) {
        // Validate invite
        WorkspaceDTO workspace = workspaceRepository.getWorkspaceByInviteCode(inviteRepository.validateInviteCode(inviteCode).getInviteCode());

        if (workspace == null) {
            Packet response = new Packet(ResponseType.WORKSPACE_JOIN_FAILURE);
            response.error("Invalid or expired invite code");
            return response;
        }

        // Check if already a member
        if (workspaceRepository.isMemberOfWorkspace(workspace.getId(), userId)) {
            Packet response = new Packet(ResponseType.WORKSPACE_JOIN_FAILURE);
            response.error("You are already a member of this workspace");
            return response;
        }

        // Add user to workspace
        boolean added = workspaceRepository.addMemberToWorkspace(workspace.getId(), userId);

        if (!added) {
            Packet response = new Packet(ResponseType.WORKSPACE_JOIN_FAILURE);
            response.error("Failed to join workspace");
            return response;
        }

        // Assign default role
        roleRepository.assignDefaultRole(userId, workspace.getId(), userId);

        // Increment invite usage
        inviteRepository.useInvite(inviteCode);

        // Log the join
        auditLogRepository.logAction(AuditActionType.WORKSPACE_JOIN,
                "User joined workspace: " + workspace.getName(), userId, workspace.getId(),
                clientSocket.getInetAddress().getHostAddress());

        Packet response = new Packet(ResponseType.WORKSPACE_JOIN_SUCCESS);
        response.put("workspace", workspace);
        response.success();

        System.out.println("[ClientHandler] User " + username + " joined workspace: " + workspace.getName());

        return response;
    }

    // Leave a workspace
    public Packet handleLeaveWorkspace(int workspaceId) {
        // Check if user is a member
        if (!workspaceRepository.isMemberOfWorkspace(workspaceId, userId)) {
            Packet response = new Packet(ResponseType.WORKSPACE_LEAVE_FAILURE);
            response.error("You are not a member of this workspace");
            return response;
        }

        // Remove user from workspace
        boolean removed = workspaceRepository.removeMemberFromWorkspace(workspaceId, userId);

        if (!removed) {
            Packet response = new Packet(ResponseType.WORKSPACE_LEAVE_FAILURE);
            response.error("Failed to leave workspace");
            return response;
        }

        // Remove all user roles from this workspace
        roleRepository.removeAllUserRoles(userId, workspaceId);

        Packet response = new Packet(ResponseType.WORKSPACE_LEAVE_SUCCESS);
        response.success();

        System.out.println("[ClientHandler] User " + username + " left workspace: " + workspaceId);

        return response;
    }

    // --- CHANNEL HANDLER ---

    // Get channels for a workspace
    public Packet handleGetChannels(int workspaceId) {
        // Check if user is in the workspace
        if (!workspaceRepository.isMemberOfWorkspace(workspaceId, userId)) {
            Packet response = new Packet(ResponseType.CHANNEL_LIST_DATA);
            response.put("channels", new ArrayList<>());
            return response;
        }

        List<ChannelDTO> channels = channelRepository.getChannelsByWorkspace(workspaceId);

        // Mark which channels user has joined
        for (ChannelDTO channel : channels) {
            boolean hasJoined = channelRepository.isMemberOfChannel(channel.getId(), userId);
            channel.setHasJoined(hasJoined);
            if (hasJoined) {
                channel.setUnreadCount(messageRepository.countUnreadMessages(channel.getId(), userId));
            }
        }

        Packet response = new Packet(ResponseType.CHANNEL_LIST_DATA);
        response.put("channels", channels);
        response.success();

        return response;
    }

    // Join a channel
    public Packet handleJoinChannel(int channelId) {
        ChannelDTO channel = channelRepository.getChannelById(channelId);

        if (channel == null) {
            Packet response = new Packet(ResponseType.CHANNEL_JOIN_FAILURE);
            response.error("Channel not found");
            return response;
        }

        // Check if user is in the workspace
        if (!workspaceRepository.isMemberOfWorkspace(channel.getWorkspaceId(), userId)) {
            Packet response = new Packet(ResponseType.CHANNEL_JOIN_FAILURE);
            response.error("You are not a member of this workspace");
            return response;
        }

        // Check if already joined
        if (channelRepository.isMemberOfChannel(channelId, userId)) {
            Packet response = new Packet(ResponseType.CHANNEL_JOIN_FAILURE);
            response.error("Already a member of this channel");
            return response;
        }

        // Add user to channel
        boolean added = channelRepository.addMemberToChannel(channelId, userId);

        if (!added) {
            Packet response = new Packet(ResponseType.CHANNEL_JOIN_FAILURE);
            response.error("Failed to join channel");
            return response;
        }

        Packet response = new Packet(ResponseType.CHANNEL_JOIN_SUCCESS);
        response.put("channel", channel);
        response.success();

        System.out.println("[ClientHandler] User " + username + " joined channel: " + channel.getName());

        return response;
    }

    // Leave a channel
    public Packet handleLeaveChannel(int channelId) {
        if (!channelRepository.isMemberOfChannel(channelId, userId)) {
            Packet response = new Packet(ResponseType.CHANNEL_LEAVE_FAILURE);
            response.error("You are not a member of this channel");
            return response;
        }

        boolean removed = channelRepository.removeMemberFromChannel(channelId, userId);

        if (!removed) {
            Packet response = new Packet(ResponseType.CHANNEL_LEAVE_FAILURE);
            response.error("Failed to leave channel");
            return response;
        }

        Packet response = new Packet(ResponseType.CHANNEL_LEAVE_SUCCESS);
        response.success();

        System.out.println("[ClientHandler] User " + username + " left channel: " + channelId);

        return response;
    }

    // Get message history for a channel
    public Packet handleGetMessageHistory(int channelId, int limit, int offset) {
        // Check if user is in the channel
        if (!channelRepository.isMemberOfChannel(channelId, userId)) {
            Packet response = new Packet(ResponseType.CHANNEL_HISTORY_DATA);
            response.put("messages", new ArrayList<>());
            return response;
        }

        List<MessageDTO> messages = messageRepository.getMessageHistory(channelId, limit, offset);

        Packet response = new Packet(ResponseType.CHANNEL_HISTORY_DATA);
        response.put("messages", messages);
        response.put("channelId", channelId);
        response.success();

        return response;
    }

    // --- USER STATUS HANDLER ---

    // Update user status
    public Packet handleUpdateStatus(UserStatus status) {
        boolean updated = userRepository.updateUserStatus(userId, status);

        Packet response = new Packet(ResponseType.USER_STATUS_UPDATED);
        response.put("status", status);
        response.success();

        Packet broadcastPacket = new Packet(ResponseType.USER_PRESENCE_BROADCAST);
        broadcastPacket.put("userId", userId);
        broadcastPacket.put("username", username);
        broadcastPacket.put("status", status.toString());
        broadcastPacket.success();
        for (ClientHandler client : activeClients) {
            if (client.isAuthenticated() && client.getUserId() != this.userId) {
                client.sendPacket(broadcastPacket);
            }
        }

        return response;
    }

    // Handle typing indicator
    public Packet handleTypingIndicator(int channelId, boolean isTyping) {
        Packet response = new Packet(ResponseType.MESSAGE_TYPING_BROADCAST);
        response.put("userId", userId);
        response.put("username", username);
        response.put("channelId", channelId);
        response.put("isTyping", isTyping);
        response.success();

        for (ClientHandler client : activeClients) {
            if (client.isAuthenticated() && client.getUserId() != this.userId && channelRepository.isMemberOfChannel(channelId, client.getUserId())) {
                client.sendPacket(response);
            }
        }

        return response;
    }

    // --- UTILITY METHODS ---

    // Send a packet to the client
    public void sendPacket(Packet packet) {
        try {
            outputStream.writeObject(packet);
            outputStream.flush();
        } catch (IOException e) {
            System.err.println("[ClientHandler] Failed to send packet: " + e.getMessage());
        }
    }

    // Send heartbeat ping to keep connection alive
    private void sendHeartbeat() {
        try {
            Packet heartbeat = new Packet(ResponseType.HEARTBEAT_PONG);
            sendPacket(heartbeat);
        } catch (Exception e) {
            // Client probably disconnected
            isRunning = false;
        }
    }

    // Clean up resources when client disconnects
    private void cleanup() {
        isRunning = false;

        if (isAuthenticated) {
            // Update user status to offline
            userRepository.updateUserStatus(userId, UserStatus.OFFLINE);

            // Log logout
            auditLogRepository.logAction(AuditActionType.USER_LOGOUT,
                    "User " + username + " logged out", userId, -1,
                    clientSocket.getInetAddress().getHostAddress());

            System.out.println("[ClientHandler] User disconnected: " + username);
        }

        activeClients.remove(this);

        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error closing resources: " + e.getMessage());
        }
    }

    // --- GETTERS ---

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthToken() {
        return authToken;
    }

    public int getCurrentWorkspaceId() {
        return currentWorkspaceId;
    }

    public void setCurrentWorkspaceId(int currentWorkspaceId) {
        this.currentWorkspaceId = currentWorkspaceId;
    }

    public int getCurrentChannelId() {
        return currentChannelId;
    }

    public void setCurrentChannelId(int currentChannelId) {
        this.currentChannelId = currentChannelId;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public String getClientAddress() {
        return clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
    }
}