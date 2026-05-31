package voxlink.shared.protocol;

/**
 * Enum representing all possible requests from client to server
 */
public enum RequestType {
    // Authentication and User Management
    AUTH_LOGIN,
    AUTH_REGISTER,
    AUTH_LOGOUT,
    AUTH_GET_PROFILE,

    // Workspace Operations
    WORKSPACE_LIST,
    WORKSPACE_CREATE,
    WORKSPACE_JOIN,
    WORKSPACE_LEAVE,
    WORKSPACE_DELETE,
    WORKSPACE_GET_INFO,

    // Channel Operations
    CHANNEL_LIST,
    CHANNEL_CREATE,
    CHANNEL_JOIN,
    CHANNEL_LEAVE,
    CHANNEL_DELETE,
    CHANNEL_GET_HISTORY,

    // Messaging
    MESSAGE_SEND,
    MESSAGE_EDIT,
    MESSAGE_DELETE,
    MESSAGE_TYPING,

    // File Operations
    FILE_UPLOAD_START,
    FILE_UPLOAD_CHUNK,
    FILE_UPLOAD_COMPLETE,
    FILE_UPLOAD_FAILURE,
    FILE_DOWNLOAD,
    FILE_DELETE,

    // User Presence & Status
    USER_UPDATE_STATUS,
    USER_GET_ONLINE,
    USER_TYPING,

    // Role & Permissions (RBAC)
    ROLE_ASSIGN,
    ROLE_REVOKE,
    ROLE_GET_PERMISSIONS,

    // Invite System
    INVITE_CREATE,
    INVITE_VALIDATE,

    // Audit and Admin
    AUDIT_GET_LOG,
    AUDIT_EXPORT_PDF,
    AUDIT_EXPORT_CSV,

    // Heartbeat / Keep Alive
    HEARTBEAT_PING,
    HEARTBEAT_PONG
}