package voxlink.server.src.main.web;

import com.sun.net.httpserver.HttpExchange;
import voxlink.server.src.main.config.ServerConfig;
import voxlink.shared.dto.ServerStatsDTO;
import voxlink.shared.rmi.RemoteService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * StatsHandler processes server statistics requests from the web portal.
 */
public class StatsHandler {

    private RemoteService remoteService;

    public StatsHandler() {
        connectToRMI();
    }

    private void connectToRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", ServerConfig.RMI_PORT);
            remoteService = (RemoteService) registry.lookup(ServerConfig.RMI_SERVICE_NAME);
            System.out.println("[StatsHandler] Connected to RMI service");
        } catch (Exception e) {
            System.err.println("[StatsHandler] RMI connection failed: " + e.getMessage());
        }
    }

    // JSON API endpoint for server statistics
    public void handleApiStats(HttpExchange exchange) throws IOException {
        ServerStatsDTO stats = fetchStats();

        if (stats == null) {
            String errorJson = "{\"error\": \"Unable to fetch server statistics\"}";
            sendJsonResponse(exchange, 500, errorJson);
            return;
        }

        String json = statsToJson(stats);
        sendJsonResponse(exchange, 200, json);
    }

    private ServerStatsDTO fetchStats() {
        try {
            if (remoteService != null) {
                return remoteService.getServerStats();
            }
        } catch (Exception e) {
            System.err.println("[StatsHandler] RMI error: " + e.getMessage());
        }
        return null;
    }

    private String statsToJson(ServerStatsDTO stats) {
        return String.format("""
            {
                "totalUsers": %d,
                "onlineUsers": %d,
                "idleUsers": %d,
                "dndUsers": %d,
                "totalWorkspaces": %d,
                "publicWorkspaces": %d,
                "privateWorkspaces": %d,
                "totalChannels": %d,
                "textChannels": %d,
                "voiceChannels": %d,
                "announcementChannels": %d,
                "totalMessagesToday": %d,
                "totalMessagesAllTime": %d,
                "messagesPerMinute": %.1f,
                "totalFiles": %d,
                "totalStorageUsed": %d,
                "formattedStorage": "%s",
                "activeConnections": %d,
                "peakConnectionsToday": %d,
                "onlinePercentage": %.1f
            }
            """,
                stats.getTotalUsers(),
                stats.getOnlineUsers(),
                stats.getIdleUsers(),
                stats.getDndUsers(),
                stats.getTotalWorkspaces(),
                stats.getPublicWorkspaces(),
                stats.getPrivateWorkspaces(),
                stats.getTotalChannels(),
                stats.getTextChannels(),
                stats.getVoiceChannels(),
                stats.getAnnouncementChannels(),
                stats.getTotalMessagesToday(),
                stats.getTotalMessagesAllTime(),
                stats.getMessagesPerMinute(),
                stats.getTotalFiles(),
                stats.getTotalStorageUsedBytes(),
                stats.getFormattedStorageUsed(),
                stats.getActiveConnections(),
                stats.getPeakConnectionsToday(),
                stats.getOnlinePercentage()
        );
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
}