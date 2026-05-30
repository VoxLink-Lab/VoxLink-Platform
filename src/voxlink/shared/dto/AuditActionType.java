package voxlink.shared.dto;

import java.io.Serializable;

/**
 * Represents the type of action being logged.
 */
public enum AuditActionType implements Serializable {

    // User Management
    USER_LOGIN("user.login", "🔐", "User Login"),
    USER_LOGOUT("user.logout", "🚪", "User Logout"),
    USER_REGISTER("user.register", "📝", "User Registered"),
    USER_UPDATE_PROFILE("user.update_profile", "✏️", "Profile Updated"),

    // Moderation Actions
    USER_KICK("user.kick", "👢", "User Kicked"),
    USER_BAN("user.ban", "🔨", "User Banned"),
    USER_UNBAN("user.unban", "", "User Unbanned"),
    USER_MUTE("user.mute", "🔇", "User Muted"),
    USER_UNMUTE("user.unmute", "🔊", "User Unmuted"),

    // Role Management
    ROLE_ASSIGN("role.assign", "👑", "Role Assigned"),
    ROLE_REVOKE("role.revoke", "⬇️", "Role Revoked"),
    ROLE_CREATE("role.create", "✨", "Role Created"),
    ROLE_DELETE("role.delete", "🗑️", "Role Deleted"),

    // Channel Management
    CHANNEL_CREATE("channel.create", "➕", "Channel Created"),
    CHANNEL_DELETE("channel.delete", "", "Channel Deleted"),
    CHANNEL_EDIT("channel.edit", "📝", "Channel Edited"),

    // Workspace Management
    WORKSPACE_CREATE("workspace.create", "🏗️", "Workspace Created"),
    WORKSPACE_DELETE("workspace.delete", "💥", "Workspace Deleted"),
    WORKSPACE_EDIT("workspace.edit", "⚙️", "Workspace Edited"),
    WORKSPACE_JOIN("workspace.join", "🚪", "User Joined Workspace"),
    WORKSPACE_LEAVE("workspace.leave", "🚶", "User Left Workspace"),

    // Message Management
    MESSAGE_DELETE("message.delete", "🗑️", "Message Deleted"),
    MESSAGE_BULK_DELETE("message.bulk_delete", "📦", "Bulk Messages Deleted"),

    // Invite Management
    INVITE_CREATE("invite.create", "📨", "Invite Created"),
    INVITE_DELETE("invite.delete", "🚫", "Invite Deleted"),
    INVITE_USE("invite.use", "", "Invite Used"),

    // File Management
    FILE_UPLOAD("file.upload", "📤", "File Uploaded"),
    FILE_DELETE("file.delete", "🗑️", "File Deleted"),
    FILE_DOWNLOAD("file.download", "📥", "File Downloaded"),

    // Audit & Admin
    AUDIT_EXPORT("audit.export", "📄", "Audit Log Exported"),
    AUDIT_VIEW("audit.view", "👁️", "Audit Log Viewed"),

    // System Events
    SERVER_START("server.start", "🟢", "Server Started"),
    SERVER_SHUTDOWN("server.shutdown", "🔴", "Server Shutdown"),
    BACKUP_CREATED("backup.created", "💾", "Backup Created");

    private final String code;
    private final String icon;
    private final String displayName;

    AuditActionType(String code, String icon, String displayName) {
        this.code = code;
        this.icon = icon;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AuditActionType fromCode(String code) {
        for (AuditActionType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}