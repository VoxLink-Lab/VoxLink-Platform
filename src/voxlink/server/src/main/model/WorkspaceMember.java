package voxlink.server.src.main.model;

import java.sql.Timestamp;

public class WorkspaceMember {
    private int memberId;
    private int workspaceId;
    private int userId;
    private int roleId;
    private Timestamp joinedAt;

    public WorkspaceMember() {

    }

    public WorkspaceMember(int memberId, int workspaceId, int userId, int roleId, Timestamp joinedAt) {
        this.memberId = memberId;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.roleId = roleId;
        this.joinedAt = joinedAt;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }
}
