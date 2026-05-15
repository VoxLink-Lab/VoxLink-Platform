package voxlink.server.src.main.model;

public class Role {
    private int roleId;
    private int workspaceId;
    private String roleName;
    private boolean canManageChannels;
    private boolean canKickUsers;
    private boolean canDeleteMessages;

    public Role() {
    }

    public Role(int roleId, int workspaceId, String roleName, boolean canManageChannels, boolean canKickUsers,
            boolean canDeleteMessages) {
        this.roleId = roleId;
        this.workspaceId = workspaceId;
        this.roleName = roleName;
        this.canManageChannels = canManageChannels;
        this.canKickUsers = canKickUsers;
        this.canDeleteMessages = canDeleteMessages;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public boolean isCanManageChannels() {
        return canManageChannels;
    }

    public void setCanManageChannels(boolean canManageChannels) {
        this.canManageChannels = canManageChannels;
    }

    public boolean isCanKickUsers() {
        return canKickUsers;
    }

    public void setCanKickUsers(boolean canKickUsers) {
        this.canKickUsers = canKickUsers;
    }

    public boolean isCanDeleteMessages() {
        return canDeleteMessages;
    }

    public void setCanDeleteMessages(boolean canDeleteMessages) {
        this.canDeleteMessages = canDeleteMessages;
    }

}
