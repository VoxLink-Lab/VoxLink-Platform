package voxlink.server.src.main.model;

import java.sql.Timestamp;

public class Channel {
    private int channelId;
    private int workspaceId;
    private String name;
    private String type; // 'TEXT', 'VOICE'
    private boolean isPrivate;
    private Timestamp createdAt;
    private String channelProfilePicture;

    public Channel() {
    }

    public Channel(int channelId, int workspaceId, String name, String type, boolean isPrivate, Timestamp createdAt,
            String channelProfilePicture) {
        this.channelId = channelId;
        this.workspaceId = workspaceId;
        this.name = name;
        this.type = type;
        this.isPrivate = isPrivate;
        this.createdAt = createdAt;
        this.channelProfilePicture = channelProfilePicture;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getChannelProfilePicture() {
        return channelProfilePicture;
    }

    public void setChannelProfilePicture(String channelProfilePicture) {
        this.channelProfilePicture = channelProfilePicture;
    }

}
