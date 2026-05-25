package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a real-time "user is typing" notification.
 */
public class TypingIndicatorDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Who is typing
    private int userId;
    private String username;
    private String displayName;

    // Where they are typing
    private int workspaceId;
    private int channelId;

    // Typing state
    private boolean isTyping;
    private LocalDateTime startedTypingAt;
    private String contentPreview;

    // For rate limiting
    private int typingEventCount;

     // Default constructor
    public TypingIndicatorDTO() {
        this.isTyping = true;
        this.startedTypingAt = LocalDateTime.now();
        this.typingEventCount = 0;
    }


    // Constructor for typing start
    public TypingIndicatorDTO(int userId, String username, int workspaceId, int channelId) {
        this();
        this.userId = userId;
        this.username = username;
        this.workspaceId = workspaceId;
        this.channelId = channelId;
    }

    // Constructor for typing stop
    public TypingIndicatorDTO(int userId, int workspaceId, int channelId, boolean stop) {
        this();
        this.userId = userId;
        this.workspaceId = workspaceId;
        this.channelId = channelId;
        this.isTyping = !stop;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : username;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void setTyping(boolean typing) {
        isTyping = typing;
    }

    public LocalDateTime getStartedTypingAt() {
        return startedTypingAt;
    }

    public void setStartedTypingAt(LocalDateTime startedTypingAt) {
        this.startedTypingAt = startedTypingAt;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        if (contentPreview != null && contentPreview.length() > 50) {
            this.contentPreview = contentPreview.substring(0, 50) + "...";
        } else {
            this.contentPreview = contentPreview;
        }
    }

    public int getTypingEventCount() {
        return typingEventCount;
    }

    public void setTypingEventCount(int typingEventCount) {
        this.typingEventCount = typingEventCount;
    }


    // Increment typing event counter
    public void incrementEventCount() {
        this.typingEventCount++;
    }


    // Check if typing indicator has expired (no activity for 5 seconds)
    public boolean hasExpired() {
        return startedTypingAt != null &&
                LocalDateTime.now().isAfter(startedTypingAt.plusSeconds(5));
    }


    // Generate display text for UI
    public String getDisplayText() {
        if (!isTyping) {
            return "";
        }
        String name = getDisplayName();
        if (contentPreview != null && !contentPreview.isEmpty()) {
            return name + " is typing: " + contentPreview;
        }
        return name + " is typing...";
    }

    @Override
    public String toString() {
        return "TypingIndicatorDTO{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", channelId=" + channelId +
                ", isTyping=" + isTyping +
                '}';
    }
}