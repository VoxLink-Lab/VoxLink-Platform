package voxlink.server.src.main.model;

import java.sql.Timestamp;

public class Workspace {
    private int workspaceId;
    private String name;
    private String description;
    private int ownerId;
    private String inviteCode;
    private Timestamp createdAt;

    public Workspace() {
    }

    public Workspace(int workspaceId, String name, String description, int ownerId, String inviteCode,
            Timestamp createdAt) {
        this.workspaceId = workspaceId;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

}
