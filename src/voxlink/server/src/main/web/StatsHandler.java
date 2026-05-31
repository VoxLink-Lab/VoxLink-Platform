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

    // Connect to RMI service
    private void connectToRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", ServerConfig.RMI_PORT);
            remoteService = (RemoteService) registry.lookup(ServerConfig.RMI_SERVICE_NAME);
            System.out.println("[StatsHandler] Connected to RMI service");
        } catch (Exception e) {
            System.err.println("[StatsHandler] Failed to connect to RMI: " + e.getMessage());
        }
    }

    // Handle HTML stats page request
    public void handleStats(HttpExchange exchange) throws IOException {
        ServerStatsDTO stats = fetchStats();

        if (stats == null) {
            sendErrorPage(exchange, 500, "Unable to fetch server statistics");
            return;
        }

        String html = generateStatsPageHTML(stats);
        byte[] response = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // Handle JSON API request for stats
    public void handleApiStats(HttpExchange exchange) throws IOException {
        ServerStatsDTO stats = fetchStats();

        if (stats == null) {
            String errorJson = "{\"error\": \"Unable to fetch server statistics\"}";
            byte[] response = errorJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
            return;
        }

        String json = statsToJson(stats);
        byte[] response = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // Fetch statistics from RMI service
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

    // Convert ServerStatsDTO to JSON string
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
                "serverUptime": "%s",
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
                formatUptime(stats.getServerUptimeSince()),
                stats.getOnlinePercentage()
        );
    }

    // Format uptime for display
    private String formatUptime(java.time.LocalDateTime uptimeSince) {
        if (uptimeSince == null) {
            return "Unknown";
        }
        java.time.Duration duration = java.time.Duration.between(uptimeSince, java.time.LocalDateTime.now());
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) {
            return String.format("%d days, %d hours", days, hours);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }

    // Generate HTML for stats page
    private String generateStatsPageHTML(ServerStatsDTO stats) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Server Statistics - VoxLink</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        background: linear-gradient(135deg, #5865F2 0%%, #4752C4 100%%);
                        min-height: 100vh;
                        padding: 40px 20px;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                    }
                    .header {
                        text-align: center;
                        color: white;
                        margin-bottom: 40px;
                    }
                    h1 {
                        font-size: 36px;
                        margin-bottom: 10px;
                    }
                    .subtitle {
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .stats-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
                        gap: 20px;
                        margin-bottom: 30px;
                    }
                    .stat-card {
                        background: white;
                        border-radius: 16px;
                        padding: 24px;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        transition: transform 0.2s;
                    }
                    .stat-card:hover {
                        transform: translateY(-2px);
                    }
                    .stat-title {
                        font-size: 14px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        color: #6C6F78;
                        margin-bottom: 12px;
                    }
                    .stat-value {
                        font-size: 36px;
                        font-weight: bold;
                        color: #1E1F22;
                    }
                    .stat-unit {
                        font-size: 14px;
                        color: #6C6F78;
                        margin-left: 5px;
                    }
                    .stat-description {
                        font-size: 12px;
                        color: #6C6F78;
                        margin-top: 10px;
                    }
                    .online { color: #23A55A; }
                    .idle { color: #F0B232; }
                    .dnd { color: #ED4245; }
                    .workspace-section {
                        background: white;
                        border-radius: 16px;
                        padding: 24px;
                        margin-bottom: 20px;
                    }
                    .section-title {
                        font-size: 18px;
                        font-weight: 600;
                        color: #1E1F22;
                        margin-bottom: 20px;
                        padding-bottom: 10px;
                        border-bottom: 2px solid #E9ECEF;
                    }
                    .progress-bar {
                        background: #E9ECEF;
                        border-radius: 10px;
                        height: 8px;
                        overflow: hidden;
                    }
                    .progress-fill {
                        background: #5865F2;
                        height: 100%%;
                        border-radius: 10px;
                        width: %d%%;
                    }
                    .footer {
                        text-align: center;
                        color: white;
                        margin-top: 40px;
                        font-size: 12px;
                        opacity: 0.8;
                    }
                    .refresh-btn {
                        display: block;
                        margin: 20px auto 0;
                        background: rgba(255, 255, 255, 0.2);
                        color: white;
                        border: 1px solid rgba(255, 255, 255, 0.3);
                        padding: 10px 24px;
                        border-radius: 40px;
                        cursor: pointer;
                        font-size: 14px;
                        transition: background 0.2s;
                    }
                    .refresh-btn:hover {
                        background: rgba(255, 255, 255, 0.3);
                    }
                    @media (max-width: 768px) {
                        .stat-value { font-size: 28px; }
                        h1 { font-size: 28px; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📊 VoxLink Server Statistics</h1>
                        <p class="subtitle">Live real-time server metrics</p>
                    </div>
                    <div class="stats-grid">
                        <div class="stat-card">
                            <div class="stat-title">👥 TOTAL USERS</div>
                            <div class="stat-value">%d</div>
                            <div class="stat-description">Registered accounts</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-title">🟢 ONLINE USERS</div>
                            <div class="stat-value online">%d</div>
                            <div class="stat-description">Currently active</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-title">🟡 IDLE / DND</div>
                            <div class="stat-value">
                                <span class="idle">%d</span> / <span class="dnd">%d</span>
                            </div>
                            <div class="stat-description">Away / Do Not Disturb</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-title">🏢 WORKSPACES</div>
                            <div class="stat-value">%d</div>
                            <div class="stat-description">Total servers</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-title">💬 MESSAGES TODAY</div>
                            <div class="stat-value">%d</div>
                            <div class="stat-description">%.1f per minute</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-title">📁 STORAGE USED</div>
                            <div class="stat-value">%s</div>
                            <div class="stat-description">Total file storage</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-title">🔌 ACTIVE CONNECTIONS</div>
                            <div class="stat-value">%d</div>
                            <div class="stat-description">Peak today: %d</div>
                        </div>
                        <div class="stat-card">
                            <div class="stat-title">⏱️ SERVER UPTIME</div>
                            <div class="stat-value">%s</div>
                            <div class="stat-description">Since last restart</div>
                        </div>
                    </div>
                    <div class="workspace-section">
                        <div class="section-title">📈 Online Activity</div>
                        <div class="stat-value">%d%%</div>
                        <div class="progress-bar" style="margin-top: 10px;">
                            <div class="progress-fill" style="width: %d%%;"></div>
                        </div>
                        <div class="stat-description" style="margin-top: 10px;">
                            %.1f%% of total users are currently online
                        </div>
                    </div>
                    <button class="refresh-btn" onclick="refreshStats()">🔄 Refresh Statistics</button>
                    <div class="footer">
                        <p>Data updates every 30 seconds | VoxLink Distributed Collaborative Workspace</p>
                        <p style="margin-top: 5px;">Last updated: <span id="timestamp">%s</span></p>
                    </div>
                </div>
                <script>
                    function refreshStats() {
                        location.reload();
                    }
                    // Auto-refresh every 30 seconds
                    setInterval(() => {
                        location.reload();
                    }, 30000);
                    // Update timestamp
                    document.getElementById('timestamp').textContent = new Date().toLocaleString();
                </script>
            </body>
            </html>
            """,
                (int) stats.getOnlinePercentage(),
                stats.getTotalUsers(),
                stats.getOnlineUsers(),
                stats.getIdleUsers(),
                stats.getDndUsers(),
                stats.getTotalWorkspaces(),
                stats.getTotalMessagesToday(),
                stats.getMessagesPerMinute(),
                stats.getFormattedStorageUsed(),
                stats.getActiveConnections(),
                stats.getPeakConnectionsToday(),
                formatUptime(stats.getServerUptimeSince()),
                (int) stats.getOnlinePercentage(),
                (int) stats.getOnlinePercentage(),
                stats.getOnlinePercentage(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    // Send error page
    private void sendErrorPage(HttpExchange exchange, int statusCode, String message) throws IOException {
        String html = String.format("""
            <!DOCTYPE html>
            <html>
            <head><title>Error - VoxLink</title>
            <style>
                body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #5865F2; color: white; }
                .container { background: white; color: #1E1F22; padding: 40px; border-radius: 16px; max-width: 500px; margin: 0 auto; }
                h1 { color: #ED4245; }
            </style>
            </head>
            <body>
                <div class="container">
                    <h1>⚠️ Error %d</h1>
                    <p>%s</p>
                    <a href="/stats" style="color: #5865F2;">← Back to Statistics</a>
                </div>
            </body>
            </html>
            """, statusCode, message);

        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}