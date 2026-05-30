package voxlink.server.src.main.rmi;

import voxlink.server.src.main.repository.*;
import voxlink.server.src.main.config.ServerConfig;
import voxlink.shared.dto.*;
import voxlink.shared.rmi.RemoteObserver;
import voxlink.shared.rmi.RemoteService;

import java.io.Serial;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * RemoteServiceImpl implements the RemoteService interface.
 * Provides remote method invocation capabilities for web portal and admin clients.
 */
public class RemoteServiceImpl extends UnicastRemoteObject implements RemoteService, Unreferenced {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RemoteServiceImpl.class.getName());

    // Repository instances
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final RoleRepository roleRepository;
    private final InviteRepository inviteRepository;
    private final AuditLogRepository auditLogRepository;
    private final FileAttachmentRepository fileRepository;

    // Track connected observers for real-time notifications
    private final Map<String, RemoteObserver> observers;
    private final Map<String, String> authTokens;

    // Server start time for uptime tracking
    private final long serverStartTime;
    private int activeConnections;

    public RemoteServiceImpl() throws RemoteException {
        super();
        this.userRepository = new UserRepository();
        this.workspaceRepository = new WorkspaceRepository();
        this.channelRepository = new ChannelRepository();
        this.messageRepository = new MessageRepository();
        this.roleRepository = new RoleRepository();
        this.inviteRepository = new InviteRepository();
        this.auditLogRepository = new AuditLogRepository();
        this.fileRepository = new FileAttachmentRepository();

        this.observers = new ConcurrentHashMap<>();
        this.authTokens = new ConcurrentHashMap<>();
        this.serverStartTime = System.currentTimeMillis();
        this.activeConnections = 0;

        LOGGER.info("RemoteServiceImpl initialized successfully");
    }

    // Called when the remote object is no longer referenced
    @Override
    public void unreferenced() {
        LOGGER.info("RemoteServiceImpl unreferenced - cleaning up");
        activeConnections--;
    }

    // --- AUTHENTICATION ---

    @Override
    public String authenticateRemote(String username, String password) throws RemoteException {
        LOGGER.info("Remote authentication attempt for user: " + username);

        UserDTO user = userRepository.authenticate(username, password);
        if (user != null) {
            String token = UUID.randomUUID().toString();
            authTokens.put(token, username);
            LOGGER.info("Remote authentication successful for: " + username);
            return token;
        }

        LOGGER.warning("Remote authentication failed for: " + username);
        return null;
    }

    @Override
    public boolean validateAuthToken(String authToken) throws RemoteException {
        return authTokens.containsKey(authToken);
    }

    // --- SERVER STATISTICS ---

    @Override
    public ServerStatsDTO getServerStats() throws RemoteException {
        ServerStatsDTO stats = new ServerStatsDTO();

        // User statistics
        List<UserDTO> onlineUsers = userRepository.getOnlineUsers();
        stats.setOnlineUsers(onlineUsers.size());
        stats.setTotalUsers(userRepository.getOnlineUsers().size());

        // Workspace statistics
        List<WorkspaceDTO> publicWorkspaces = workspaceRepository.getPublicWorkspaces();
        stats.setTotalWorkspaces(publicWorkspaces.size());
        stats.setPublicWorkspaces(publicWorkspaces.size());

        // Connection statistics
        stats.setActiveConnections(activeConnections);
        stats.setStatsGeneratedAt(LocalDateTime.now());

        // Uptime
        long uptime = getServerUptime();
        stats.setServerUptimeSince(LocalDateTime.now().minusSeconds(uptime));

        return stats;
    }

    @Override
    public long getServerUptime() throws RemoteException {
        return System.currentTimeMillis() - serverStartTime;
    }

    @Override
    public int getActiveConnections() throws RemoteException {
        return 0;
    }

    // --- USER MANAGEMENT ---

    @Override
    public UserDTO getUserById(int userId) throws RemoteException {
        return userRepository.getUserById(userId);
    }

    @Override
    public UserDTO getUserByUsername(String username) throws RemoteException {
        return userRepository.getUserByUsername(username);
    }

    @Override
    public List<UserDTO> getUsersByWorkspace(int workspaceId) throws RemoteException {
        return userRepository.getUsersByWorkspace(workspaceId);
    }

    @Override
    public int getOnlineUserCount() throws RemoteException {
        return userRepository.getOnlineUsers().size();
    }

    @Override
    public boolean kickUser(int adminId, int targetUserId, int workspaceId, String reason) throws RemoteException {
        // Verify admin has permission
        if (!roleRepository.isAtLeastModerator(adminId, workspaceId)) {
            LOGGER.warning("User " + adminId + " attempted to kick without permission");
            return false;
        }

        // Remove user from workspace
        boolean removed = workspaceRepository.removeMemberFromWorkspace(workspaceId, targetUserId);

        if (removed) {
            // Log the action
            auditLogRepository.logUserKick(adminId, targetUserId, workspaceId, reason, "RMI");

            // Notify all observers
            UserDTO targetUser = userRepository.getUserById(targetUserId);
            if (targetUser != null) {
                broadcastUserKicked(targetUserId, targetUser.getUsername(), workspaceId, adminId, reason);
            }

            LOGGER.info("User " + targetUserId + " kicked from workspace " + workspaceId + " by " + adminId);
        }

        return removed;
    }

    @Override
    public boolean banUser(int adminId, int targetUserId, int workspaceId, String reason) throws RemoteException {
        // Verify admin has permission
        if (!roleRepository.isAdmin(adminId, workspaceId)) {
            LOGGER.warning("User " + adminId + " attempted to ban without admin permission");
            return false;
        }

        // Remove user from workspace (ban = remove + mark for future blocks)
        boolean removed = workspaceRepository.removeMemberFromWorkspace(workspaceId, targetUserId);

        if (removed) {
            auditLogRepository.logUserBan(adminId, targetUserId, workspaceId, reason, "RMI");

            UserDTO targetUser = userRepository.getUserById(targetUserId);
            if (targetUser != null) {
                broadcastUserBanned(targetUserId, targetUser.getUsername(), workspaceId, adminId, reason);
            }

            LOGGER.info("User " + targetUserId + " banned from workspace " + workspaceId + " by " + adminId);
        }

        return removed;
    } // ========== Helper Methods ==========

    @Override
    public boolean unbanUser(int adminId, int targetUserId, int workspaceId) throws RemoteException {
        // Verify admin has permission
        if (!roleRepository.isAdmin(adminId, workspaceId)) {
            LOGGER.warning("User " + adminId + " attempted to unban without admin permission");
            return false;
        }

        // Re-add user to workspace (unban)
        boolean added = workspaceRepository.addMemberToWorkspace(workspaceId, targetUserId);

        if (added) {
            LOGGER.info("User " + targetUserId + " unbanned from workspace " + workspaceId + " by " + adminId);
        }

        return added;
    }

    // --- WORKSPACE MANAGEMENT ---

    @Override
    public List<WorkspaceDTO> getPublicWorkspaces() throws RemoteException {
        return workspaceRepository.getPublicWorkspaces();
    }

    @Override
    public WorkspaceDTO getWorkspaceById(int workspaceId) throws RemoteException {
        return workspaceRepository.getWorkspaceById(workspaceId);
    }

    @Override
    public WorkspaceDTO createWorkspace(String name, String description, int ownerId, boolean isPublic) throws RemoteException {
        WorkspaceDTO workspace = workspaceRepository.createWorkspace(name, description, ownerId, isPublic);

        if (workspace != null) {
            // Create default roles for the new workspace
            roleRepository.createDefaultRoles(workspace.getId());

            // Create default channel
            channelRepository.createChannel("general", "Default general channel", workspace.getId(), ChannelType.TEXT, false, ownerId);

            LOGGER.info("Workspace created: " + name + " (ID: " + workspace.getId() + ") by user " + ownerId);
        }

        return workspace;
    }

    @Override
    public boolean deleteWorkspace(int adminId, int workspaceId) throws RemoteException {
        // Verify admin is the owner
        WorkspaceDTO workspace = workspaceRepository.getWorkspaceById(workspaceId);
        if (workspace == null || workspace.getOwnerId() != adminId) {
            LOGGER.warning("User " + adminId + " attempted to delete workspace " + workspaceId + " without ownership");
            return false;
        }

        boolean deleted = workspaceRepository.deleteWorkspace(workspaceId);

        if (deleted) {
            auditLogRepository.logAction(AuditActionType.WORKSPACE_DELETE,
                    "Workspace " + workspace.getName() + " deleted", adminId, workspaceId, "RMI");
            LOGGER.info("Workspace " + workspaceId + " deleted by owner " + adminId);
        }

        return deleted;
    }

    // --- CHANNEL MANAGEMENT ---

    @Override
    public List<ChannelDTO> getChannelsByWorkspace(int workspaceId) throws RemoteException {
        return channelRepository.getChannelsByWorkspace(workspaceId);
    }

    @Override
    public ChannelDTO createChannel(String name, String description, int workspaceId,
                                    String channelType, boolean isPrivate, int createdBy) throws RemoteException {
        ChannelType type;
        try {
            type = ChannelType.valueOf(channelType.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = ChannelType.TEXT;
        }

        ChannelDTO channel = channelRepository.createChannel(name, description, workspaceId, type, isPrivate, createdBy);

        if (channel != null) {
            auditLogRepository.logChannelCreate(createdBy, channel.getId(), name, workspaceId, "RMI");
            broadcastChannelCreated(channel.getId(), name, workspaceId);
            LOGGER.info("Channel created: " + name + " in workspace " + workspaceId + " by " + createdBy);
        }

        return channel;
    }

    @Override
    public boolean deleteChannel(int adminId, int channelId) throws RemoteException {
        ChannelDTO channel = channelRepository.getChannelById(channelId);
        if (channel == null) {
            return false;
        }

        // Verify admin has permission (moderator or above)
        if (!roleRepository.isAtLeastModerator(adminId, channel.getWorkspaceId())) {
            LOGGER.warning("User " + adminId + " attempted to delete channel without permission");
            return false;
        }

        boolean deleted = channelRepository.deleteChannel(channelId);

        if (deleted) {
            auditLogRepository.logChannelDelete(adminId, channelId, channel.getName(), channel.getWorkspaceId(), "RMI");
            broadcastChannelDeleted(channelId, channel.getName(), channel.getWorkspaceId());
            LOGGER.info("Channel " + channelId + " deleted by " + adminId);
        }

        return deleted;
    }

    // ========== Invite Management ==========

    @Override
    public InviteDTO createInvite(int workspaceId, int createdBy, int expiresInDays, int maxUses) throws RemoteException {
        // Verify user has permission to create invites
        if (!roleRepository.isAtLeastModerator(createdBy, workspaceId)) {
            LOGGER.warning("User " + createdBy + " attempted to create invite without permission");
            return null;
        }

        String inviteCode = generateInviteCode();
        Timestamp expiresAt = null;

        if (expiresInDays > 0) {
            expiresAt = new Timestamp(System.currentTimeMillis() + (expiresInDays * 24L * 60 * 60 * 1000));
        }

        InviteType type;
        if (maxUses == -1 && expiresInDays == 0) {
            type = InviteType.PERMANENT;
        } else if (maxUses == 1) {
            type = InviteType.ONE_TIME;
        } else if (maxUses > 1) {
            type = InviteType.LIMITED;
        } else {
            type = InviteType.EXPIRING;
        }

        InviteDTO invite = inviteRepository.createInvite(inviteCode, workspaceId, createdBy, expiresAt, maxUses, type, null, null);

        if (invite != null) {
            LOGGER.info("Invite created for workspace " + workspaceId + " by " + createdBy);
        }

        return invite;
    }

    @Override
    public InviteDTO validateInviteCode(String inviteCode) throws RemoteException {
        return inviteRepository.validateInviteCode(inviteCode);
    }

    @Override
    public boolean useInvite(String inviteCode) throws RemoteException {
        return inviteRepository.useInvite(inviteCode);
    }

    @Override
    public List<InviteDTO> getActiveInvites(int workspaceId) throws RemoteException {
        return inviteRepository.getActiveInvitesByWorkspace(workspaceId);
    }

    // --- ROLE AND PERMISSION MANAGEMENT ---

    @Override
    public boolean assignRole(int adminId, int targetUserId, int workspaceId, String roleName) throws RemoteException {
        // Verify admin has permission
        if (!roleRepository.isAdmin(adminId, workspaceId)) {
            LOGGER.warning("User " + adminId + " attempted to assign role without admin permission");
            return false;
        }

        boolean assigned = roleRepository.assignRoleByName(targetUserId, workspaceId, roleName, adminId);

        if (assigned) {
            auditLogRepository.logRoleAssign(adminId, targetUserId, roleName, workspaceId, "RMI");
            LOGGER.info("Role " + roleName + " assigned to user " + targetUserId + " by " + adminId);
        }

        return assigned;
    }

    @Override
    public boolean revokeRole(int adminId, int targetUserId, int workspaceId, String roleName) throws RemoteException {
        // Verify admin has permission
        if (!roleRepository.isAdmin(adminId, workspaceId)) {
            LOGGER.warning("User " + adminId + " attempted to revoke role without admin permission");
            return false;
        }

        RoleDTO role = roleRepository.getRoleByName(workspaceId, roleName);
        if (role == null) {
            return false;
        }

        boolean revoked = roleRepository.removeRoleFromUser(targetUserId, role.getId(), workspaceId);

        if (revoked) {
            LOGGER.info("Role " + roleName + " revoked from user " + targetUserId + " by " + adminId);
        }

        return revoked;
    }

    @Override
    public boolean hasPermission(int userId, int workspaceId, String permission) throws RemoteException {
        return roleRepository.hasPermission(userId, workspaceId, permission);
    }

    // --- AUDIT LOG MANAGEMENT ---

    @Override
    public List<AuditLogEntryDTO> getAuditLogs(int adminId, int workspaceId, int limit, int offset) throws RemoteException {
        // Verify admin has permission to view audit logs
        if (!roleRepository.isAdmin(adminId, workspaceId)) {
            LOGGER.warning("User " + adminId + " attempted to view audit logs without admin permission");
            return new ArrayList<>();
        }

        return auditLogRepository.getLogsByWorkspace(workspaceId, limit, offset);
    }

    @Override
    public String exportAuditLogsToCSV(int adminId, int workspaceId) throws RemoteException {
        // Verify admin has permission
        if (!roleRepository.isAdmin(adminId, workspaceId)) {
            LOGGER.warning("User " + adminId + " attempted to export audit logs without admin permission");
            return null;
        }

        List<AuditLogEntryDTO> logs = auditLogRepository.getAllLogsForExport(workspaceId);
        return auditLogRepository.exportToCSV(logs);
    }

    // --- SYSTEM MANAGEMENT ---

    @Override
    public boolean shutdownServer(int adminId) throws RemoteException {
        // In a real implementation, verify admin is super admin
        LOGGER.warning("Server shutdown requested by user " + adminId);

        // Notify all observers about shutdown
        broadcastServerShutdown("Admin initiated shutdown", 30);

        // Start shutdown in a separate thread
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return true;
    }

    @Override
    public boolean broadcastSystemMessage(int adminId, String message) throws RemoteException {
        // Verify admin has permission
        WorkspaceDTO workspace = workspaceRepository.getWorkspacesByUser(adminId).get(0);
        if (workspace == null || !roleRepository.isAtLeastModerator(adminId, workspace.getId())) {
            return false;
        }

        broadcastSystemAnnouncement(message, adminId);
        LOGGER.info("System announcement from " + adminId + ": " + message);
        return true;
    }

    // --- OBSERVER MANAGEMENT

    // Register an observer to receive notifications
    public void registerObserver(String clientId, RemoteObserver observer) {
        observers.put(clientId, observer);
        activeConnections++;
        LOGGER.info("Observer registered: " + clientId);
    }

    // Unregister an observer
    public void unregisterObserver(String clientId) {
        observers.remove(clientId);
        activeConnections--;
        LOGGER.info("Observer unregistered: " + clientId);
    }

    // --- BROADCASE METHODS ---
    private void broadcastUserKicked(int kickedUserId, String kickedUsername, int workspaceId, int kickedBy, String reason) {
        observers.values().forEach(observer -> {
            try {
                observer.onUserKicked(kickedUserId, kickedUsername, workspaceId, kickedBy, reason);
            } catch (RemoteException e) {
                LOGGER.warning("Failed to notify observer: " + e.getMessage());
            }
        });
    }

    private void broadcastUserBanned(int bannedUserId, String bannedUsername, int workspaceId, int bannedBy, String reason) {
        observers.values().forEach(observer -> {
            try {
                observer.onUserBanned(bannedUserId, bannedUsername, workspaceId, bannedBy, reason);
            } catch (RemoteException e) {
                LOGGER.warning("Failed to notify observer: " + e.getMessage());
            }
        });
    }

    private void broadcastChannelCreated(int channelId, String channelName, int workspaceId) {
        observers.values().forEach(observer -> {
            try {
                observer.onChannelCreated(channelId, channelName, workspaceId);
            } catch (RemoteException e) {
                LOGGER.warning("Failed to notify observer: " + e.getMessage());
            }
        });
    }

    private void broadcastChannelDeleted(int channelId, String channelName, int workspaceId) {
        observers.values().forEach(observer -> {
            try {
                observer.onChannelDeleted(channelId, channelName, workspaceId);
            } catch (RemoteException e) {
                LOGGER.warning("Failed to notify observer: " + e.getMessage());
            }
        });
    }

    private void broadcastSystemAnnouncement(String message, int sentBy) {
        observers.values().forEach(observer -> {
            try {
                observer.onSystemAnnouncement(message, sentBy);
            } catch (RemoteException e) {
                LOGGER.warning("Failed to notify observer: " + e.getMessage());
            }
        });
    }

    private void broadcastServerShutdown(String reason, int secondsUntilShutdown) {
        observers.values().forEach(observer -> {
            try {
                observer.onServerShutdown(reason, secondsUntilShutdown);
            } catch (RemoteException e) {
                LOGGER.warning("Failed to notify observer about shutdown: " + e.getMessage());
            }
        });
    }

    // Generate a unique invite code
    private String generateInviteCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}