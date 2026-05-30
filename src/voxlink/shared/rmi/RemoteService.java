package voxlink.shared.rmi;

import voxlink.shared.dto.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RemoteService interface defines all methods that can be called remotely
 */
public interface RemoteService extends Remote {
    // Get real-time server statistics
    ServerStatsDTO getServerStats() throws RemoteException;

    // Get running time of a server
    long getServerUptime() throws RemoteException;

    // Get current active connections
    int getActiveConnections() throws RemoteException;

    // Get user
    UserDTO getUserById(int userId) throws RemoteException;
    UserDTO getUSerByUsername(String username) throws RemoteException;
    List<UserDTO> getUsersByWorkspace(int workspaceId) throws RemoteException;
    int getOnlineUserCount() throws RemoteException;

    // Remove user from workspace
    boolean kickUser(int adminId, int targetUserId, int workspaceId, String reason) throws RemoteException;

    // Ban user from workspace
    boolean banUser(int adminId, int targetUserId, int workspaceId, String reason) throws RemoteException;

    // Unban user
    boolean unbanUser(int adminId, int targetUserId, int workspaceId) throws RemoteException;

    // --- WORKSPACE MANAGEMENT ---
    List<WorkspaceDTO> getAllPublicWorkspaces() throws RemoteException;
    WorkspaceDTO getWorkspaceById(int workspaceId) throws RemoteException;
    WorkspaceDTO createWorkspace(String name, String description, int ownerId, boolean isPublic) throws RemoteException;
    boolean deleteWorkspace(int adminId, int workspaceId) throws RemoteException;

    // --- CHANNEL MANAGEMENT ---
    List<ChannelDTO> getChannelsByWorkspace(int workspaceId) throws RemoteException;
    ChannelDTO createChannel(String name, String description, int workspaceId, String channelType, boolean isPrivate, int createdBy) throws RemoteException;
    boolean deleteChannel(int adminId, int channelId) throws RemoteException;

    // --- INVITE MANAGEMENT ---

    // Create invite code for workspace
    InviteDTO createInvite(int workspaceId, int createdBy, int expiresInDays, int maxUses) throws RemoteException;

   // Validate invite code
    WorkspaceDTO validateInviteCode(String inviteCode) throws RemoteException;

   // Increment invite code usage
    boolean useInvite(String inviteCode) throws RemoteException;

    // Get active invites for workspace
    List<InviteDTO> getActiveInvites(int workspaceId) throws RemoteException;

    // --- ROLE AND PERMISSION MANAGEMENT ---

    // Assign a role to a user
    boolean assignRole(int adminId, int targetUserId, int workspaceId, String roleName) throws RemoteException;

    // Remove role from a user
    boolean revokeRole(int adminId, int targetUserId, int workspaceId, String roleName) throws RemoteException;

    // Check if user has specific permission
    boolean hasPermission(int userId, int workspaceId, String permission) throws RemoteException;

    // --- AUDIT LOG MANAGEMENT ---

   // Get audit logs for a workspace
    List<AuditLogEntryDTO> getAuditLogs(int adminId, int workspaceId, int limit, int offset) throws RemoteException;

   // Export audit logs to CSV
    String exportAuditLogsToCSV(int adminId, int workspaceId) throws RemoteException;

    // --- SYSTEM MANAGEMENT ---

    // Shutdown the server
    boolean shutdownServer(int adminId) throws RemoteException;

    // Broadcast system message to all user
    boolean broadcastSystemMessage(int adminId, String message) throws RemoteException;

    // --- AUTHENTICATION ---

   // Authenticate a user for remote access
    String authenticateRemote(String username, String password) throws RemoteException;

    // Validate auth token
    boolean validateAuthToken(String authToken) throws RemoteException;
}
