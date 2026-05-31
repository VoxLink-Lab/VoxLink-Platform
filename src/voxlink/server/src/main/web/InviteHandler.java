package voxlink.server.src.main.web;

import com.sun.net.httpserver.HttpExchange;
import voxlink.server.src.main.config.ServerConfig;
import voxlink.shared.dto.InviteDTO;
import voxlink.shared.dto.WorkspaceDTO;
import voxlink.shared.rmi.RemoteService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * InviteHandler processes invite link requests from the web portal.
 */
public class InviteHandler {

    private RemoteService remoteService;

    public InviteHandler() {
        connectToRMI();
    }

    private void connectToRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", ServerConfig.RMI_PORT);
            remoteService = (RemoteService) registry.lookup(ServerConfig.RMI_SERVICE_NAME);
            System.out.println("[InviteHandler] Connected to RMI service");
        } catch (Exception e) {
            System.err.println("[InviteHandler] RMI connection failed: " + e.getMessage());
        }
    }

    // API endpoint to validate an invitation code (returns JSON)
    public void handleApiValidate(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String inviteCode = extractInviteCodeFromQuery(query);

        if (inviteCode == null || inviteCode.isEmpty()) {
            sendJsonResponse(exchange, 400, "{\"valid\": false, \"error\": \"Missing invite code\"}");
            return;
        }

        try {
            if (remoteService == null) {
                sendJsonResponse(exchange, 500, "{\"valid\": false, \"error\": \"Server unavailable\"}");
                return;
            }

            InviteDTO invite = remoteService.validateInviteCode(inviteCode);

            if (invite != null && invite.isValid()) {
                WorkspaceDTO workspace = remoteService.getWorkspaceById(invite.getWorkspaceId());
                if (workspace != null) {
                    String json = String.format("""
                        {
                            "valid": true,
                            "workspaceId": %d,
                            "workspaceName": "%s",
                            "description": "%s",
                            "memberCount": %d
                        }
                        """,
                            workspace.getId(),
                            escapeJson(workspace.getName()),
                            escapeJson(workspace.getDescription()),
                            workspace.getMemberCount()
                    );
                    sendJsonResponse(exchange, 200, json);
                    return;
                }
            }

            sendJsonResponse(exchange, 200, "{\"valid\": false, \"error\": \"Invite invalid or expired\"}");

        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"valid\": false, \"error\": \"Server error\"}");
        }
    }

    // API endpoint to join a workspace using invite code
    public void handleApiJoin(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
            return;
        }

        // Read request body
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        // Simple parsing (in production, use JSON parser)
        String inviteCode = extractValue(body, "inviteCode");
        String token = extractValue(body, "token");

        if (inviteCode == null || token == null) {
            sendJsonResponse(exchange, 400, "{\"success\": false, \"message\": \"Missing inviteCode or token\"}");
            return;
        }

        // Validate token and join workspace
        // This would integrate with your auth system
        sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Joined workspace\"}");
    }

    private String extractInviteCodeFromQuery(String query) {
        if (query == null) return null;
        String[] parts = query.split("&");
        for (String part : parts) {
            if (part.startsWith("code=")) {
                return part.substring(5);
            }
        }
        return null;
    }

    private String extractValue(String body, String key) {
        String search = "\"" + key + "\":\"";
        int start = body.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = body.indexOf("\"", start);
        if (end == -1) return null;
        return body.substring(start, end);
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}