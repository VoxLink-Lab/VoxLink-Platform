package voxlink.server.src.main.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.List;

import voxlink.server.src.main.model.Channel;
import voxlink.server.src.main.model.Message;
import voxlink.server.src.main.model.User;
import voxlink.server.src.main.model.Workspace;
import voxlink.server.src.main.service.AuthService;
import voxlink.server.src.main.service.ChannelService;
import voxlink.server.src.main.service.MessageService;
import voxlink.server.src.main.service.WorkspaceService;

public class MessageDispatcher {

    private final AuthService authService;
    private final WorkspaceService workspaceService;
    private final ChannelService channelService;
    private final MessageService messageService;
    private final Gson gson;

    public MessageDispatcher() {
        this.authService = new AuthService();
        this.workspaceService = new WorkspaceService();
        this.channelService = new ChannelService();
        this.messageService = new MessageService();
        this.gson = new Gson(); // Initialize Gson for JSON parsing/formatting
    }

    /**
     * Parses the incoming client request and dispatches it to the appropriate service.
     * @param request The raw JSON string from the client
     * @return The JSON response to send back to the client
     */
    public String dispatch(String request) {
        try {
            // Parse the incoming request as a JSON Object
            JsonObject jsonRequest = JsonParser.parseString(request).getAsJsonObject();
            
            // Extract the action the client wants to perform
            if (!jsonRequest.has("action")) {
                return createErrorResponse("Missing 'action' field in request.");
            }
            
            String action = jsonRequest.get("action").getAsString();
            JsonObject response = new JsonObject();
            response.addProperty("action", action + "_RESPONSE"); // Let client know what we are responding to

            // Route the request to the correct handler based on the action
            switch (action.toUpperCase()) {
                case "LOGIN":
                    return handleLogin(jsonRequest, response);
                case "REGISTER":
                    return handleRegister(jsonRequest, response);
                case "CREATE_WORKSPACE":
                    return handleCreateWorkspace(jsonRequest, response);
                case "JOIN_WORKSPACE":
                    return handleJoinWorkspace(jsonRequest, response);
                case "GET_WORKSPACES":
                    return handleGetWorkspaces(jsonRequest, response);
                case "CREATE_CHANNEL":
                    return handleCreateChannel(jsonRequest, response);
                case "GET_CHANNELS":
                    return handleGetChannels(jsonRequest, response);
                case "SEND_MESSAGE":
                    return handleSendMessage(jsonRequest, response);
                case "GET_MESSAGES":
                    return handleGetMessages(jsonRequest, response);
                default:
                    return createErrorResponse("Unknown action: " + action);
            }
            
        } catch (JsonSyntaxException e) {
            return createErrorResponse("Invalid JSON format.");
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Internal server error: " + e.getMessage());
        }
    }
    
    // ==========================================
    // AUTHENTICATION
    // ==========================================

    private String handleLogin(JsonObject jsonRequest, JsonObject response) {
        if (!jsonRequest.has("username") || !jsonRequest.has("password")) {
            return createErrorResponse("LOGIN requires 'username' and 'password'.");
        }
        
        String username = jsonRequest.get("username").getAsString();
        String password = jsonRequest.get("password").getAsString();
        
        User user = authService.loginUser(username, password);
        
        if (user != null) {
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Login successful");
            response.add("user", gson.toJsonTree(user)); 
            response.getAsJsonObject("user").remove("passwordHash");
        } else {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Invalid username or password");
        }
        
        return gson.toJson(response);
    }

    private String handleRegister(JsonObject jsonRequest, JsonObject response) {
        if (!jsonRequest.has("username") || !jsonRequest.has("email") || !jsonRequest.has("password")) {
            return createErrorResponse("REGISTER requires 'username', 'email', and 'password'.");
        }
        
        String username = jsonRequest.get("username").getAsString();
        String email = jsonRequest.get("email").getAsString();
        String password = jsonRequest.get("password").getAsString();
        
        User newUser = authService.registerUser(username, email, password);
        
        if (newUser != null) {
            response.addProperty("status", "SUCCESS");
            response.addProperty("message", "Registration successful");
            response.add("user", gson.toJsonTree(newUser));
            response.getAsJsonObject("user").remove("passwordHash");
        } else {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Registration failed. Username or email may already exist.");
        }
        
        return gson.toJson(response);
    }

    // ==========================================
    // WORKSPACES
    // ==========================================

    private String handleCreateWorkspace(JsonObject jsonRequest, JsonObject response) {
        if (!jsonRequest.has("name") || !jsonRequest.has("description") || !jsonRequest.has("ownerId")) {
            return createErrorResponse("CREATE_WORKSPACE requires 'name', 'description', and 'ownerId'.");
        }
        String name = jsonRequest.get("name").getAsString();
        String description = jsonRequest.get("description").getAsString();
        int ownerId = jsonRequest.get("ownerId").getAsInt();

        Workspace workspace = workspaceService.createWorkspace(name, description, ownerId);
        if (workspace != null) {
            response.addProperty("status", "SUCCESS");
            response.add("workspace", gson.toJsonTree(workspace));
        } else {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Failed to create workspace.");
        }
        return gson.toJson(response);
    }

    private String handleJoinWorkspace(JsonObject jsonRequest, JsonObject response) {
        if (!jsonRequest.has("inviteCode")) {
            return createErrorResponse("JOIN_WORKSPACE requires 'inviteCode'.");
        }
        String inviteCode = jsonRequest.get("inviteCode").getAsString();
        Workspace workspace = workspaceService.joinWorkspace(inviteCode);
        if (workspace != null) {
            response.addProperty("status", "SUCCESS");
            response.add("workspace", gson.toJsonTree(workspace));
        } else {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Invalid invite code.");
        }
        return gson.toJson(response);
    }
    
    private String handleGetWorkspaces(JsonObject jsonRequest, JsonObject response) {
        if (!jsonRequest.has("userId")) {
            return createErrorResponse("GET_WORKSPACES requires 'userId'.");
        }
        int userId = jsonRequest.get("userId").getAsInt();
        List<Workspace> workspaces = workspaceService.getWorkspacesByOwner(userId);
        response.addProperty("status", "SUCCESS");
        response.add("workspaces", gson.toJsonTree(workspaces));
        return gson.toJson(response);
    }

    // ==========================================
    // CHANNELS
    // ==========================================

    private String handleCreateChannel(JsonObject jsonRequest, JsonObject response) {
        if (!jsonRequest.has("workspaceId") || !jsonRequest.has("name") || !jsonRequest.has("type") || !jsonRequest.has("isPrivate")) {
            return createErrorResponse("CREATE_CHANNEL requires 'workspaceId', 'name', 'type', and 'isPrivate'.");
        }
        int workspaceId = jsonRequest.get("workspaceId").getAsInt();
        String name = jsonRequest.get("name").getAsString();
        String type = jsonRequest.get("type").getAsString();
        boolean isPrivate = jsonRequest.get("isPrivate").getAsBoolean();

        Channel channel = channelService.createChannel(workspaceId, name, type, isPrivate);
        if (channel != null) {
            response.addProperty("status", "SUCCESS");
            response.add("channel", gson.toJsonTree(channel));
        } else {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Failed to create channel.");
        }
        return gson.toJson(response);
    }

    private String handleGetChannels(JsonObject jsonRequest, JsonObject response) {
        if (!jsonRequest.has("workspaceId")) {
            return createErrorResponse("GET_CHANNELS requires 'workspaceId'.");
        }
        int workspaceId = jsonRequest.get("workspaceId").getAsInt();
        List<Channel> channels = channelService.getWorkspaceChannels(workspaceId);
        response.addProperty("status", "SUCCESS");
        response.add("channels", gson.toJsonTree(channels));
        return gson.toJson(response);
    }

    // ==========================================
    // MESSAGES
    // ==========================================

    private String handleSendMessage(JsonObject jsonRequest, JsonObject response) {
        if (!jsonRequest.has("senderId") || !jsonRequest.has("content")) {
            return createErrorResponse("SEND_MESSAGE requires 'senderId' and 'content', plus either 'channelId' or 'receiverId'.");
        }
        int senderId = jsonRequest.get("senderId").getAsInt();
        String content = jsonRequest.get("content").getAsString();

        Message message = null;
        if (jsonRequest.has("channelId")) {
            int channelId = jsonRequest.get("channelId").getAsInt();
            message = messageService.sendChannelMessage(senderId, channelId, content);
        } else if (jsonRequest.has("receiverId")) {
            int receiverId = jsonRequest.get("receiverId").getAsInt();
            message = messageService.sendDirectMessage(senderId, receiverId, content);
        } else {
            return createErrorResponse("SEND_MESSAGE requires either 'channelId' or 'receiverId'.");
        }

        if (message != null) {
            response.addProperty("status", "SUCCESS");
            response.add("messageData", gson.toJsonTree(message));
        } else {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Failed to send message.");
        }
        return gson.toJson(response);
    }

    private String handleGetMessages(JsonObject jsonRequest, JsonObject response) {
        if (jsonRequest.has("channelId")) {
            int channelId = jsonRequest.get("channelId").getAsInt();
            List<Message> messages = messageService.getChannelMessages(channelId);
            response.addProperty("status", "SUCCESS");
            response.add("messages", gson.toJsonTree(messages));
            return gson.toJson(response);
        } else if (jsonRequest.has("user1Id") && jsonRequest.has("user2Id")) {
            int user1Id = jsonRequest.get("user1Id").getAsInt();
            int user2Id = jsonRequest.get("user2Id").getAsInt();
            List<Message> messages = messageService.getDirectMessages(user1Id, user2Id);
            response.addProperty("status", "SUCCESS");
            response.add("messages", gson.toJsonTree(messages));
            return gson.toJson(response);
        } else {
            return createErrorResponse("GET_MESSAGES requires 'channelId' or ('user1Id' and 'user2Id').");
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private String createErrorResponse(String errorMessage) {
        JsonObject error = new JsonObject();
        error.addProperty("status", "ERROR");
        error.addProperty("message", errorMessage);
        return new Gson().toJson(error);
    }
}


