package voxlink.shared.rmi;

import voxlink.shared.dto.MessageDTO;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RemoteObserver interface defines callback methods that the server can call on remote clients to notify them of events in real-time
 */
public interface RemoteObserver extends Remote {

    // Called when a new message is sent to the channel
    void onNewMessage(MessageDTO message, int workspaceId) throws RemoteException;

    // Called when a message is edited
    void onMessageEdited(int messageId, String newContent, int channelId) throws RemoteException;

    // Called when a message is deleted
    void onMessageDeleted(int messageId, int channelId) throws RemoteException;

   // Called when a user status is changed
    void onUserStatusChanged(UserDTO user, UserStatus oldStatus, UserStatus newStatus) throws RemoteException;

    // Called when a user joined a workspace
    void onUserJoinedWorkspace(UserDTO user, int workspaceId) throws RemoteException;

   // Called when a user left a workspace
    void onUserLeftWorkspace(int userId, String username, int workspaceId) throws RemoteException;

    // Called when a user joins a channel
    void onUserJoinedChannel(UserDTO user, int channelId, int workspaceId) throws RemoteException;

    // Called when a user left a channel
    void onUserLeftChannel(int userId, String username, int channelId) throws RemoteException;

    // Called when a new channel is created
    void onChannelCreated(int channelId, String channelName, int workspaceId) throws RemoteException;

    // Called when a new channel is created
    void onChannelDeleted(int channelId, String channelName, int workspaceId) throws RemoteException;

   // Called when a user starts typing
    void onUserTyping(int userId, String username, int channelId, int workspaceId) throws RemoteException;

    // Called when a user stops typing
    void onUserStoppedTyping(int userId, int channelId, int workspaceId) throws RemoteException;

    // Called when a new file is uploaded to a channel
    void onFileUploaded(int fileId, String fileName, int channelId, int uploadedBy) throws RemoteException;

    // Called when a user is removed from a workspace
    void onUserKicked(int kickedUserId, String kickedUsername, int workspaceId,
                      int kickedBy, String reason) throws RemoteException;

    // Called when a user is banned from a workspace
    void onUserBanned(int bannedUserId, String bannedUsername, int workspaceId,
                      int bannedBy, String reason) throws RemoteException;

    // Called when their is server announcement
    void onSystemAnnouncement(String announcement, int sentBy) throws RemoteException;

    // Called when server statistics are updated
    void onServerStatsUpdate(int onlineUsers, int totalUsers, int activeWorkspaces) throws RemoteException;

    // Called when a server is about to shutdown
    void onServerShutdown(String reason, int secondsUntilShutdown) throws RemoteException;
}