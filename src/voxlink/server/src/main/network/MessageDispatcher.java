package voxlink.server.src.main.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import voxlink.server.src.main.model.User;
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
                // TODO: Add Workspace/Channel/Message actions later!
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
            // Security: Strip the password hash before sending the user object back to the client
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
            // Security: Strip the password hash
            response.getAsJsonObject("user").remove("passwordHash");
        } else {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Registration failed. Username or email may already exist.");
        }
        
        return gson.toJson(response);
    }

    private String createErrorResponse(String errorMessage) {
        JsonObject error = new JsonObject();
        error.addProperty("status", "ERROR");
        error.addProperty("message", errorMessage);
        return new Gson().toJson(error);
    }
}


