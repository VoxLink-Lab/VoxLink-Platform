package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents real-time server statistics.
 */
public class ServerStatsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // User statistics
    private int totalUsers;
    private int onlineUsers;
    private int idleUsers;
    private int dndUsers;
    private int todayActiveUsers;
    private int weeklyActiveUsers;

    // Workspace statistics
    private int totalWorkspaces;
    private int publicWorkspaces;
    private int privateWorkspaces;

    // Channel statistics
    private int totalChannels;
    private int textChannels;
    private int voiceChannels;
    private int announcementChannels;

    // Message statistics
    private int totalMessagesToday;
    private int totalMessagesAllTime;
    private double messagesPerMinute;

    // File statistics
    private int totalFiles;
    private long totalStorageUsedBytes;
    private int filesUploadedToday;

    // Invite statistics
    private int activeInvites;
    private int invitesUsedToday;

    // Performance metrics
    private double averageResponseTimeMs;
    private int activeConnections;
    private int peakConnectionsToday;

    // Timestamps
    private LocalDateTime statsGeneratedAt;
    private LocalDateTime serverUptimeSince;

    // Per-workspace breakdown (optional)
    private Map<Integer, Integer> usersPerWorkspace;

    // Default constructor
    public ServerStatsDTO() {
        this.statsGeneratedAt = LocalDateTime.now();
        this.usersPerWorkspace = new HashMap<>();
    }

    // Getters and Setters
    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getOnlineUsers() {
        return onlineUsers;
    }

    public void setOnlineUsers(int onlineUsers) {
        this.onlineUsers = onlineUsers;
    }

    public int getIdleUsers() {
        return idleUsers;
    }

    public void setIdleUsers(int idleUsers) {
        this.idleUsers = idleUsers;
    }

    public int getDndUsers() {
        return dndUsers;
    }

    public void setDndUsers(int dndUsers) {
        this.dndUsers = dndUsers;
    }

    public int getTodayActiveUsers() {
        return todayActiveUsers;
    }

    public void setTodayActiveUsers(int todayActiveUsers) {
        this.todayActiveUsers = todayActiveUsers;
    }

    public int getWeeklyActiveUsers() {
        return weeklyActiveUsers;
    }

    public void setWeeklyActiveUsers(int weeklyActiveUsers) {
        this.weeklyActiveUsers = weeklyActiveUsers;
    }

    public int getTotalWorkspaces() {
        return totalWorkspaces;
    }

    public void setTotalWorkspaces(int totalWorkspaces) {
        this.totalWorkspaces = totalWorkspaces;
    }

    public int getPublicWorkspaces() {
        return publicWorkspaces;
    }

    public void setPublicWorkspaces(int publicWorkspaces) {
        this.publicWorkspaces = publicWorkspaces;
    }

    public int getPrivateWorkspaces() {
        return privateWorkspaces;
    }

    public void setPrivateWorkspaces(int privateWorkspaces) {
        this.privateWorkspaces = privateWorkspaces;
    }

    public int getTotalChannels() {
        return totalChannels;
    }

    public void setTotalChannels(int totalChannels) {
        this.totalChannels = totalChannels;
    }

    public int getTextChannels() {
        return textChannels;
    }

    public void setTextChannels(int textChannels) {
        this.textChannels = textChannels;
    }

    public int getVoiceChannels() {
        return voiceChannels;
    }

    public void setVoiceChannels(int voiceChannels) {
        this.voiceChannels = voiceChannels;
    }

    public int getAnnouncementChannels() {
        return announcementChannels;
    }

    public void setAnnouncementChannels(int announcementChannels) {
        this.announcementChannels = announcementChannels;
    }

    public int getTotalMessagesToday() {
        return totalMessagesToday;
    }

    public void setTotalMessagesToday(int totalMessagesToday) {
        this.totalMessagesToday = totalMessagesToday;
    }

    public int getTotalMessagesAllTime() {
        return totalMessagesAllTime;
    }

    public void setTotalMessagesAllTime(int totalMessagesAllTime) {
        this.totalMessagesAllTime = totalMessagesAllTime;
    }

    public double getMessagesPerMinute() {
        return messagesPerMinute;
    }

    public void setMessagesPerMinute(double messagesPerMinute) {
        this.messagesPerMinute = messagesPerMinute;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalStorageUsedBytes() {
        return totalStorageUsedBytes;
    }

    public void setTotalStorageUsedBytes(long totalStorageUsedBytes) {
        this.totalStorageUsedBytes = totalStorageUsedBytes;
    }

    public String getFormattedStorageUsed() {
        if (totalStorageUsedBytes < 1024) {
            return totalStorageUsedBytes + " B";
        } else if (totalStorageUsedBytes < 1024 * 1024) {
            return String.format("%.1f KB", totalStorageUsedBytes / 1024.0);
        } else if (totalStorageUsedBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", totalStorageUsedBytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", totalStorageUsedBytes / (1024.0 * 1024 * 1024));
        }
    }

    public int getFilesUploadedToday() {
        return filesUploadedToday;
    }

    public void setFilesUploadedToday(int filesUploadedToday) {
        this.filesUploadedToday = filesUploadedToday;
    }

    public int getActiveInvites() {
        return activeInvites;
    }

    public void setActiveInvites(int activeInvites) {
        this.activeInvites = activeInvites;
    }

    public int getInvitesUsedToday() {
        return invitesUsedToday;
    }

    public void setInvitesUsedToday(int invitesUsedToday) {
        this.invitesUsedToday = invitesUsedToday;
    }

    public double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public void setAverageResponseTimeMs(double averageResponseTimeMs) {
        this.averageResponseTimeMs = averageResponseTimeMs;
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        this.activeConnections = activeConnections;
    }

    public int getPeakConnectionsToday() {
        return peakConnectionsToday;
    }

    public void setPeakConnectionsToday(int peakConnectionsToday) {
        this.peakConnectionsToday = peakConnectionsToday;
    }

    public LocalDateTime getStatsGeneratedAt() {
        return statsGeneratedAt;
    }

    public void setStatsGeneratedAt(LocalDateTime statsGeneratedAt) {
        this.statsGeneratedAt = statsGeneratedAt;
    }

    public LocalDateTime getServerUptimeSince() {
        return serverUptimeSince;
    }

    public void setServerUptimeSince(LocalDateTime serverUptimeSince) {
        this.serverUptimeSince = serverUptimeSince;
    }

    public Map<Integer, Integer> getUsersPerWorkspace() {
        return usersPerWorkspace;
    }

    public void setUsersPerWorkspace(Map<Integer, Integer> usersPerWorkspace) {
        this.usersPerWorkspace = usersPerWorkspace;
    }

    // Get total active users
    public int getTotalActiveUsers() {
        return onlineUsers + idleUsers + dndUsers;
    }

   // Get online percentage
    public double getOnlinePercentage() {
        if (totalUsers == 0) return 0;
        return (onlineUsers * 100.0) / totalUsers;
    }
}