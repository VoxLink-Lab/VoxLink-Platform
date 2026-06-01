package voxlink.server.src.main.network;

import voxlink.shared.dto.UserStatus;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

/**
 * PacketProcessor routes incoming packets to the appropriate handler methods based on the RequestType.
 */
public class PacketProcessor {

    private final ClientHandler clientHandler;

    public PacketProcessor(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    // Process an incoming packet and return the response
    public Packet process(Packet packet) {
        RequestType type = packet.getRequestType();

        if (type == null) {
            return createErrorResponse("Invalid request type");
        }

        return switch (type) {
            // --- WORKSPACE OPERATIONS ---
            case WORKSPACE_LIST -> clientHandler.handleGetWorkspaces();
            case WORKSPACE_JOIN -> handleWorkspaceJoin(packet);
            case WORKSPACE_LEAVE -> handleWorkspaceLeave(packet);
            case WORKSPACE_CREATE -> handleWorkspaceCreate(packet);
            case WORKSPACE_GET_INFO -> handleWorkspaceInfo(packet);

            // --- CHANNEL OPERATIONS ---
            case CHANNEL_LIST -> handleChannelList(packet);
            case CHANNEL_JOIN -> handleChannelJoin(packet);
            case CHANNEL_LEAVE -> handleChannelLeave(packet);
            case CHANNEL_CREATE -> handleChannelCreate(packet);
            case CHANNEL_GET_HISTORY -> handleChannelHistory(packet);

            // --- MESSAGING OPERATIONS ---
            case MESSAGE_SEND -> handleMessageSend(packet);
            case MESSAGE_EDIT -> handleMessageEdit(packet);
            case MESSAGE_DELETE -> handleMessageDelete(packet);
            case MESSAGE_TYPING -> handleTypingIndicator(packet);

            // --- USER OPERATIONS ---
            case USER_UPDATE_STATUS -> handleUpdateStatus(packet);
            case USER_GET_ONLINE -> handleGetOnlineUsers(packet);

            // --- FILE OPERATIONS ---
            case FILE_DOWNLOAD -> handleFileDownload(packet);
            case FILE_DELETE -> handleFileDelete(packet);

            // --- ROLE OPERATIONS ----
            case ROLE_GET_PERMISSIONS -> handleGetPermissions(packet);

            // --- INVITE OPERATIONS ---
            case INVITE_CREATE -> handleInviteCreate(packet);
            case INVITE_VALIDATE -> handleInviteValidate(packet);

            // --- HEARTBEAT ---
            case HEARTBEAT_PING -> handleHeartbeat();

            default -> {
                System.err.println("[PacketProcessor] Unhandled request type: " + type);
                yield createErrorResponse("Unhandled request type: " + type);
            }
        };
    }

    // --- WORKSPACE HANDLERS ---

    private Packet handleWorkspaceJoin(Packet packet) {
        String inviteCode = packet.get("inviteCode").toString();

        if (inviteCode == null) {
            return createErrorResponse("Invite code required");
        }

        return clientHandler.handleJoinWorkspace(inviteCode);
    }

    private Packet handleWorkspaceLeave(Packet packet) {
        Integer workspaceId = (Integer) packet.get("workspaceId");

        if (workspaceId == null) {
            return createErrorResponse("Workspace ID required");
        }

        return clientHandler.handleLeaveWorkspace(workspaceId);
    }

    private Packet handleWorkspaceCreate(Packet packet) {
        String name = packet.get("name").toString();
        String description = packet.get("description").toString();
        Boolean isPublic = (Boolean) packet.get("isPublic");

        if (name == null) {
            return createErrorResponse("Workspace name required");
        }

        boolean publicStatus = isPublic != null && isPublic;

        return clientHandler.handleCreateWorkspace(name, description, publicStatus);
    }

    private Packet handleWorkspaceInfo(Packet packet) {
        Integer workspaceId = (Integer) packet.get("workspaceId");

        if (workspaceId == null) {
            return createErrorResponse("Workspace ID required");
        }

        // TODO: Implement workspace info retrieval
        Packet response = new Packet(ResponseType.WORKSPACE_INFO_DATA);
        response.error("Workspace info not yet implemented");
        return response;
    }

    // --- CHANNEL HANDLERS ---

    private Packet handleChannelList(Packet packet) {
        Integer workspaceId = (Integer) packet.get("workspaceId");

        if (workspaceId == null) {
            return createErrorResponse("Workspace ID required");
        }

        return clientHandler.handleGetChannels(workspaceId);
    }

    private Packet handleChannelJoin(Packet packet) {
        Integer channelId = (Integer) packet.get("channelId");

        if (channelId == null) {
            return createErrorResponse("Channel ID required");
        }

        return clientHandler.handleJoinChannel(channelId);
    }

    private Packet handleChannelLeave(Packet packet) {
        Integer channelId = (Integer) packet.get("channelId");

        if (channelId == null) {
            return createErrorResponse("Channel ID required");
        }

        return clientHandler.handleLeaveChannel(channelId);
    }

    private Packet handleChannelCreate(Packet packet) {
        String name = packet.get("name").toString();
        String description = packet.get("description").toString();
        Integer workspaceId = (Integer) packet.get("workspaceId");
        String channelType = packet.get("channelType").toString();
        Boolean isPrivate = (Boolean) packet.get("isPrivate");

        if (name == null || workspaceId == null) {
            return createErrorResponse("Channel name and workspace ID required");
        }

        boolean privateStatus = isPrivate != null && isPrivate;
        String type = channelType != null ? channelType : "TEXT";

        return clientHandler.handleCreateChannel(name, description, workspaceId, type, privateStatus);
    }

    private Packet handleChannelHistory(Packet packet) {
        Integer channelId = (Integer) packet.get("channelId");
        Integer limit = (Integer) packet.get("limit");
        Integer offset = (Integer) packet.get("offset");

        if (channelId == null) {
            return createErrorResponse("Channel ID required");
        }

        int msgLimit = limit != null ? limit : 50;
        int msgOffset = offset != null ? offset : 0;

        return clientHandler.handleGetMessageHistory(channelId, msgLimit, msgOffset);
    }

    // --- MESSAGE HANDLERS ---

    private Packet handleMessageSend(Packet packet) {
        Integer channelId = (Integer) packet.get("channelId");
        String content = packet.get("content").toString();
        Integer replyToId = (Integer) packet.get("replyToMessageId");

        if (channelId == null || content == null) {
            return createErrorResponse("Channel ID and content required");
        }

        return clientHandler.handleSendMessage(channelId, content, replyToId);
    }

    private Packet handleMessageEdit(Packet packet) {
        Integer messageId = (Integer) packet.get("messageId");
        String content = packet.get("content").toString();

        if (messageId == null || content == null) {
            return createErrorResponse("Message ID and content required");
        }

        // TODO: Implement message editing
        Packet response = new Packet(ResponseType.MESSAGE_EDIT_FAILURE);
        response.error("Message editing not yet implemented");
        return response;
    }

    private Packet handleMessageDelete(Packet packet) {
        Integer messageId = (Integer) packet.get("messageId");

        if (messageId == null) {
            return createErrorResponse("Message ID required");
        }

        // TODO: Implement message deletion
        Packet response = new Packet(ResponseType.MESSAGE_DELETE_FAILURE);
        response.error("Message deletion not yet implemented");
        return response;
    }

    private Packet handleTypingIndicator(Packet packet) {
        Integer channelId = (Integer) packet.get("channelId");
        Boolean isTyping = (Boolean) packet.get("isTyping");

        if (channelId == null) {
            return createErrorResponse("Channel ID required");
        }

        boolean typing = isTyping != null && isTyping;

        return clientHandler.handleTypingIndicator(channelId, typing);
    }

    // --- USER HANDLERS ---

    private Packet handleUpdateStatus(Packet packet) {
        String statusStr = packet.get("status").toString();

        if (statusStr == null) {
            return createErrorResponse("Status required");
        }

        try {
            UserStatus status = UserStatus.valueOf(statusStr.toUpperCase());
            return clientHandler.handleUpdateStatus(status);
        } catch (IllegalArgumentException e) {
            return createErrorResponse("Invalid status: " + statusStr);
        }
    }

    private Packet handleGetOnlineUsers(Packet packet) {
        // TODO: Implement getting online users
        Packet response = new Packet(ResponseType.USER_ONLINE_LIST_DATA);
        response.put("users", new java.util.ArrayList<>());
        response.success();
        return response;
    }

    // --- FILE HANDLERS ---

    private Packet handleFileDownload(Packet packet) {
        Integer fileId = (Integer) packet.get("fileId");

        if (fileId == null) {
            return createErrorResponse("File ID required");
        }

        // TODO: Implement file download
        Packet response = new Packet(ResponseType.FILE_DOWNLOAD_FAILURE);
        response.error("File download not yet implemented");
        return response;
    }

    private Packet handleFileDelete(Packet packet) {
        Integer fileId = (Integer) packet.get("fileId");

        if (fileId == null) {
            return createErrorResponse("File ID required");
        }

        // TODO: Implement file deletion
        Packet response = new Packet(ResponseType.FILE_DELETE_FAILURE);
        response.error("File deletion not yet implemented");
        return response;
    }

    // --- ROLE HANDLERS ---

    private Packet handleGetPermissions(Packet packet) {
        Integer workspaceId = (Integer) packet.get("workspaceId");

        if (workspaceId == null) {
            return createErrorResponse("Workspace ID required");
        }

        // TODO: Implement permission retrieval
        Packet response = new Packet(ResponseType.ROLE_PERMISSIONS_DATA);
        response.put("permissions", new java.util.HashSet<>());
        response.success();
        return response;
    }

    // --- INVITE HANDLERS ---

    private Packet handleInviteCreate(Packet packet) {
        Integer workspaceId = (Integer) packet.get("workspaceId");
        Integer expiresInDays = (Integer) packet.get("expiresInDays");
        Integer maxUses = (Integer) packet.get("maxUses");

        if (workspaceId == null) {
            return createErrorResponse("Workspace ID required");
        }

        int days = expiresInDays != null ? expiresInDays : 7;
        int uses = maxUses != null ? maxUses : 1;

        // TODO: Implement invite creation
        Packet response = new Packet(ResponseType.INVITE_CREATE_FAILURE);
        response.error("Invite creation not yet implemented");
        return response;
    }

    private Packet handleInviteValidate(Packet packet) {
        String inviteCode = packet.get("inviteCode").toString();

        if (inviteCode == null) {
            return createErrorResponse("Invite code required");
        }

        // TODO: Implement invite validation
        Packet response = new Packet(ResponseType.INVITE_VALIDATE_FAILURE);
        response.error("Invite validation not yet implemented");
        return response;
    }

    // --- HEARTBEAT HANDLER ---

    private Packet handleHeartbeat() {
        Packet response = new Packet(ResponseType.HEARTBEAT_PONG);
        response.success();
        return response;
    }

    // --- UTILITY METHOD ---

    private Packet createErrorResponse(String errorMessage) {
        Packet response = new Packet(ResponseType.ERROR_GENERAL);
        response.error(errorMessage);
        return response;
    }
}