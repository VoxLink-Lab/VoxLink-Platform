package voxlink.server.src.main.web;

import com.sun.net.httpserver.HttpExchange;
import voxlink.server.src.main.config.ServerConfig;
import voxlink.shared.dto.InviteDTO;
import voxlink.shared.dto.WorkspaceDTO;
import voxlink.shared.rmi.RemoteService;

import java.io.IOException;
import java.io.OutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.nio.charset.StandardCharsets;

/**
 * InviteHandler processes invite link requests from the web portal.
 */
public class InviteHandler {

    private RemoteService remoteService;

    public InviteHandler() {
        connectToRMI();
    }

    // Connect to RMI service
    private void connectToRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", ServerConfig.RMI_PORT);
            remoteService = (RemoteService) registry.lookup(ServerConfig.RMI_SERVICE_NAME);
            System.out.println("[InviteHandler] Connected to RMI service");
        } catch (Exception e) {
            System.err.println("[InviteHandler] Failed to connect to RMI: " + e.getMessage());
        }
    }

    // Handle invite request
    public void handleInvite(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String inviteCode = extractInviteCode(path);

        if (inviteCode == null || inviteCode.isEmpty()) {
            sendErrorPage(exchange, 400, "Invalid invite code");
            return;
        }

        // Validate invite code via RMI
        WorkspaceDTO workspace = null;
        InviteDTO invite = null;
        try {
            if (remoteService != null) {
                invite = remoteService.validateInviteCode(inviteCode);
                workspace = remoteService.getWorkspaceById(invite.getWorkspaceId());
            }
        } catch (Exception e) {
            System.err.println("[InviteHandler] RMI error: " + e.getMessage());
            sendErrorPage(exchange, 500, "Server error. Please try again later.");
            return;
        }

        if (workspace == null) {
            sendErrorPage(exchange, 404, "Invite link is invalid or has expired");
            return;
        }

        // Send invite acceptance page
        sendInvitePage(exchange, workspace, inviteCode);
    }

    // Extract invite code from URL path
    private String extractInviteCode(String path) {
        String prefix = "/invite/";
        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        return null;
    }

    // Send the invite acceptance page
    private void sendInvitePage(HttpExchange exchange, WorkspaceDTO workspace, String inviteCode) throws IOException {
        String html = generateInvitePageHTML(workspace, inviteCode);
        byte[] response = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // Send error page
    private void sendErrorPage(HttpExchange exchange, int statusCode, String message) throws IOException {
        String html = generateErrorPageHTML(statusCode, message);
        byte[] response = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // Generate HTML for invite page
    private String generateInvitePageHTML(WorkspaceDTO workspace, String inviteCode) {
        String workspaceName = workspace.getName();
        String workspaceDescription = workspace.getDescription() != null ? workspace.getDescription() : "";
        int memberCount = workspace.getMemberCount();
        String iconHtml = workspace.getIconUrl() != null ?
                "<img src='" + workspace.getIconUrl() + "' class='workspace-icon' alt='Icon'>" :
                "<div class='workspace-icon-placeholder'>" + workspaceName.substring(0, 1).toUpperCase() + "</div>";

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Join %s - VoxLink</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        background: linear-gradient(135deg, #5865F2 0%, #4752C4 100%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                    }
                    .container {
                        background: white;
                        border-radius: 16px;
                        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
                        width: 100%;
                        max-width: 500px;
                        margin: 20px;
                        overflow: hidden;
                    }
                    .header {
                        background: #F8F9FA;
                        padding: 40px 30px;
                        text-align: center;
                        border-bottom: 1px solid #E9ECEF;
                    }
                    .workspace-icon {
                        width: 80px;
                        height: 80px;
                        border-radius: 20px;
                        object-fit: cover;
                        margin-bottom: 20px;
                    }
                    .workspace-icon-placeholder {
                        width: 80px;
                        height: 80px;
                        border-radius: 20px;
                        background: #5865F2;
                        color: white;
                        font-size: 48px;
                        font-weight: bold;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px auto;
                    }
                    h1 {
                        font-size: 28px;
                        color: #1E1F22;
                        margin-bottom: 10px;
                    }
                    .workspace-name {
                        color: #5865F2;
                    }
                    .description {
                        color: #6C6F78;
                        margin-bottom: 15px;
                    }
                    .member-count {
                        display: inline-block;
                        background: #E9ECEF;
                        padding: 5px 12px;
                        border-radius: 20px;
                        font-size: 14px;
                        color: #4E5058;
                    }
                    .content {
                        padding: 30px;
                        text-align: center;
                    }
                    .info-box {
                        background: #F2F3F5;
                        border-radius: 12px;
                        padding: 20px;
                        margin-bottom: 25px;
                        text-align: left;
                    }
                    .info-box p {
                        margin: 8px 0;
                        color: #4E5058;
                    }
                    .info-box strong {
                        color: #1E1F22;
                    }
                    .btn {
                        display: inline-block;
                        background: #5865F2;
                        color: white;
                        border: none;
                        padding: 14px 32px;
                        font-size: 16px;
                        font-weight: 600;
                        border-radius: 40px;
                        cursor: pointer;
                        text-decoration: none;
                        transition: background 0.2s;
                    }
                    .btn:hover {
                        background: #4752C4;
                    }
                    .login-link {
                        margin-top: 20px;
                        color: #6C6F78;
                        font-size: 14px;
                    }
                    .login-link a {
                        color: #5865F2;
                        text-decoration: none;
                    }
                    .login-link a:hover {
                        text-decoration: underline;
                    }
                    .footer {
                        background: #F8F9FA;
                        padding: 20px;
                        text-align: center;
                        border-top: 1px solid #E9ECEF;
                        font-size: 12px;
                        color: #6C6F78;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        %s
                        <h1>Join <span class="workspace-name">%s</span></h1>
                        <p class="description">%s</p>
                        <span class="member-count">👥 %d members</span>
                    </div>
                    <div class="content">
                        <div class="info-box">
                            <p><strong>📌 You've been invited to join this workspace</strong></p>
                            <p>After joining, you'll be able to:</p>
                            <p>• 💬 Chat in text channels</p>
                            <p>• 📁 Share files and images</p>
                            <p>• 👥 Collaborate with team members</p>
                        </div>
                        <a href="#" class="btn" id="joinBtn">Accept Invite & Join</a>
                        <div class="login-link">
                            Already have an account? <a href="#" id="loginLink">Log in</a>
                        </div>
                    </div>
                    <div class="footer">
                        VoxLink - Distributed Collaborative Workspace
                    </div>
                </div>
                <script>
                    const inviteCode = '%s';
                    const workspaceId = %d;
                    
                    document.getElementById('joinBtn').addEventListener('click', async (e) => {
                        e.preventDefault();
                        const btn = e.target;
                        btn.textContent = 'Joining...';
                        btn.style.opacity = '0.7';
                        
                        // Check if user is logged in (you would implement this with localStorage or cookie)
                        const token = localStorage.getItem('voxlink_token');
                        
                        if (token) {
                            // User is logged in, join directly via API
                            try {
                                const response = await fetch('/api/join', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({ inviteCode: inviteCode, token: token })
                                });
                                const result = await response.json();
                                if (result.success) {
                                    window.location.href = '/dashboard';
                                } else {
                                    alert('Failed to join: ' + result.message);
                                    btn.textContent = 'Accept Invite & Join';
                                    btn.style.opacity = '1';
                                }
                            } catch (err) {
                                alert('Error joining workspace');
                                btn.textContent = 'Accept Invite & Join';
                                btn.style.opacity = '1';
                            }
                        } else {
                            // Not logged in, redirect to login with invite code
                            window.location.href = '/login?invite=' + inviteCode;
                        }
                    });
                    
                    document.getElementById('loginLink').addEventListener('click', (e) => {
                        e.preventDefault();
                        window.location.href = '/login?invite=' + inviteCode;
                    });
                </script>
            </body>
            </html>
            """,
                workspaceName, iconHtml, workspaceName, workspaceDescription, memberCount, inviteCode, workspace.getId()
        );
    }

    // Generate HTML for error page
    private String generateErrorPageHTML(int statusCode, String message) {
        String title = statusCode == 404 ? "Invite Not Found" : "Error";
        String emoji = statusCode == 404 ? "🔗" : "⚠️";

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s - VoxLink</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        background: linear-gradient(135deg, #5865F2 0%, #4752C4 100%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                    }
                    .container {
                        background: white;
                        border-radius: 16px;
                        padding: 50px 40px;
                        text-align: center;
                        max-width: 450px;
                        margin: 20px;
                    }
                    .emoji {
                        font-size: 64px;
                        margin-bottom: 20px;
                    }
                    h1 {
                        font-size: 28px;
                        color: #1E1F22;
                        margin-bottom: 10px;
                    }
                    p {
                        color: #6C6F78;
                        margin-bottom: 30px;
                    }
                    .btn {
                        display: inline-block;
                        background: #5865F2;
                        color: white;
                        padding: 12px 28px;
                        border-radius: 40px;
                        text-decoration: none;
                        font-weight: 600;
                    }
                    .btn:hover {
                        background: #4752C4;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="emoji">%s</div>
                    <h1>%s</h1>
                    <p>%s</p>
                    <a href="/" class="btn">Go to Home</a>
                </div>
            </body>
            </html>
            """, title, emoji, title, message);
    }
}