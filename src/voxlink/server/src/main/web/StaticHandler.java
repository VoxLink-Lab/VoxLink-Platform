package voxlink.server.src.main.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * StaticHandler serves static files (CSS, JS, images) and HTML pages for the VoxLink web portal.
 */
public class StaticHandler {

    // Handle static file requests (CSS, JS, images)
    public void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String mimeType = getMimeType(path);

        // Try to load file from classpath resources
        InputStream inputStream = getClass().getResourceAsStream("/web" + path);

        if (inputStream == null) {
            // Try to load from filesystem
            java.io.File file = new java.io.File("web" + path);
            if (file.exists() && file.canRead()) {
                inputStream = new java.io.FileInputStream(file);
            }
        }

        if (inputStream == null) {
            // 404 Not Found
            String response = "File not found: " + path;
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        // Read file content
        byte[] content = inputStream.readAllBytes();
        inputStream.close();

        // Send response
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    // Handle dashboard page
    public void handleDashboard(HttpExchange exchange) throws IOException {
        String html = generateDashboardHTML();
        byte[] response = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // Handle index page (home page)
    public void handleIndex(HttpExchange exchange) throws IOException {
        String html = generateIndexHTML();
        byte[] response = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // Get MIME type based on file extension
    private String getMimeType(String path) {
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".html")) return "text/html";
        return "application/octet-stream";
    }

    // Generate dashboard HTML
    private String generateDashboardHTML() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Dashboard - VoxLink</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        background: #1E1F22;
                        min-height: 100vh;
                    }
                    .navbar {
                        background: #2B2D31;
                        padding: 16px 24px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 1px solid #1E1F22;
                    }
                    .logo {
                        font-size: 20px;
                        font-weight: bold;
                        color: #5865F2;
                    }
                    .nav-links a {
                        color: #DBDEE1;
                        text-decoration: none;
                        margin-left: 24px;
                        transition: color 0.2s;
                    }
                    .nav-links a:hover {
                        color: #5865F2;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 40px auto;
                        padding: 0 20px;
                    }
                    .welcome-card {
                        background: #2B2D31;
                        border-radius: 16px;
                        padding: 30px;
                        margin-bottom: 30px;
                        text-align: center;
                    }
                    .welcome-card h1 {
                        color: white;
                        margin-bottom: 10px;
                    }
                    .welcome-card p {
                        color: #B5BAC1;
                    }
                    .stats-card {
                        background: #2B2D31;
                        border-radius: 16px;
                        padding: 30px;
                        margin-bottom: 30px;
                    }
                    .stats-card h2 {
                        color: white;
                        margin-bottom: 20px;
                        font-size: 20px;
                    }
                    .stats-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 20px;
                    }
                    .stat-item {
                        text-align: center;
                        padding: 20px;
                        background: #1E1F22;
                        border-radius: 12px;
                    }
                    .stat-number {
                        font-size: 32px;
                        font-weight: bold;
                        color: #5865F2;
                    }
                    .stat-label {
                        color: #B5BAC1;
                        margin-top: 8px;
                        font-size: 14px;
                    }
                    .workspaces-list {
                        background: #2B2D31;
                        border-radius: 16px;
                        padding: 30px;
                    }
                    .workspaces-list h2 {
                        color: white;
                        margin-bottom: 20px;
                        font-size: 20px;
                    }
                    .workspace-item {
                        background: #1E1F22;
                        border-radius: 12px;
                        padding: 16px;
                        margin-bottom: 12px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .workspace-info h3 {
                        color: white;
                        margin-bottom: 4px;
                    }
                    .workspace-info p {
                        color: #B5BAC1;
                        font-size: 12px;
                    }
                    .join-btn {
                        background: #5865F2;
                        color: white;
                        border: none;
                        padding: 8px 20px;
                        border-radius: 40px;
                        cursor: pointer;
                        font-size: 14px;
                        transition: background 0.2s;
                    }
                    .join-btn:hover {
                        background: #4752C4;
                    }
                    .footer {
                        text-align: center;
                        padding: 20px;
                        color: #80848E;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <nav class="navbar">
                    <div class="logo">VoxLink</div>
                    <div class="nav-links">
                        <a href="/">Home</a>
                        <a href="/stats">Statistics</a>
                        <a href="#" id="logoutBtn">Logout</a>
                    </div>
                </nav>
                <div class="container">
                    <div class="welcome-card">
                        <h1>Welcome back, <span id="username">User</span>!</h1>
                        <p>Manage your workspaces and account settings from here</p>
                    </div>

                    <div class="stats-card">
                        <h2>📊 Your Activity</h2>
                        <div class="stats-grid">
                            <div class="stat-item">
                                <div class="stat-number" id="workspaceCount">0</div>
                                <div class="stat-label">Workspaces Joined</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-number" id="channelCount">0</div>
                                <div class="stat-label">Channels</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-number" id="messageCount">0</div>
                                <div class="stat-label">Messages Sent</div>
                            </div>
                        </div>
                    </div>

                    <div class="workspaces-list">
                        <h2>🏢 Your Workspaces</h2>
                        <div id="workspacesContainer">
                            <p style="color: #B5BAC1; text-align: center;">Loading workspaces...</p>
                        </div>
                    </div>
                </div>
                <div class="footer">
                    <p>VoxLink - Distributed Collaborative Workspace</p>
                </div>
                <script>
                    // Get token from localStorage
                    const token = localStorage.getItem('voxlink_token');
                    const username = localStorage.getItem('voxlink_username');
                    if (!token) {
                        window.location.href = '/login';
                    }
                    document.getElementById('username').textContent = username || 'User';
                    // Fetch user workspaces
                    async function fetchWorkspaces() {
                        try {
                            const response = await fetch('/api/user/workspaces', {
                                headers: { 'Authorization': 'Bearer ' + token }
                            });
                            const data = await response.json();
                            if (data.success) {
                                document.getElementById('workspaceCount').textContent = data.workspaces.length;
                                renderWorkspaces(data.workspaces);
                            }
                        } catch (err) {
                            console.error('Failed to fetch workspaces:', err);
                        }
                    }
                    function renderWorkspaces(workspaces) {
                        const container = document.getElementById('workspacesContainer');
                        if (workspaces.length === 0) {
                            container.innerHTML = '<p style="color: #B5BAC1; text-align: center;">No workspaces yet. Join one using an invite link!</p>';
                            return;
                        }
                        container.innerHTML = workspaces.map(ws => `
                            <div class="workspace-item">
                                <div class="workspace-info">
                                    <h3>${escapeHtml(ws.name)}</h3>
                                    <p>${escapeHtml(ws.description) || 'No description'}</p>
                                </div>
                                <button class="join-btn" onclick="openWorkspace(${ws.id})">Open</button>
                            </div>
                        `).join('');
                    }
                    function openWorkspace(workspaceId) {
                        // Redirect to desktop app or show workspace details
                        alert('Opening workspace ' + workspaceId + ' in VoxLink desktop app');
                    }
                    function escapeHtml(text) {
                        if (!text) return '';
                        const div = document.createElement('div');
                        div.textContent = text;
                        return div.innerHTML;
                    }
                    document.getElementById('logoutBtn').addEventListener('click', (e) => {
                        e.preventDefault();
                        localStorage.removeItem('voxlink_token');
                        localStorage.removeItem('voxlink_username');
                        window.location.href = '/login';
                    });
                    // Load data
                    fetchWorkspaces();
                </script>
            </body>
            </html>
        """;
    }

    // Generate index HTML (home page)
    private String generateIndexHTML() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>VoxLink - Distributed Collaborative Workspace</title>
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
                    }
                    .navbar {
                        background: rgba(0, 0, 0, 0.2);
                        padding: 16px 24px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .logo {
                        font-size: 24px;
                        font-weight: bold;
                        color: white;
                    }
                    .nav-links a {
                        color: white;
                        text-decoration: none;
                        margin-left: 24px;
                        opacity: 0.9;
                        transition: opacity 0.2s;
                    }
                    .nav-links a:hover {
                        opacity: 1;
                    }
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                        padding: 60px 20px;
                        text-align: center;
                    }
                    .hero h1 {
                        font-size: 48px;
                        color: white;
                        margin-bottom: 20px;
                    }
                    .hero p {
                        font-size: 20px;
                        color: rgba(255, 255, 255, 0.9);
                        margin-bottom: 40px;
                    }
                    .btn {
                        display: inline-block;
                        background: white;
                        color: #5865F2;
                        padding: 14px 32px;
                        border-radius: 40px;
                        text-decoration: none;
                        font-weight: 600;
                        margin: 0 10px;
                        transition: transform 0.2s;
                    }
                    .btn-outline {
                        background: transparent;
                        color: white;
                        border: 2px solid white;
                    }
                    .btn:hover {
                        transform: translateY(-2px);
                    }
                    .features {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                        gap: 30px;
                        margin-top: 80px;
                    }
                    .feature {
                        background: rgba(255, 255, 255, 0.1);
                        backdrop-filter: blur(10px);
                        padding: 30px;
                        border-radius: 16px;
                        color: white;
                    }
                    .feature h3 {
                        margin-bottom: 15px;
                    }
                    .footer {
                        text-align: center;
                        padding: 40px;
                        color: rgba(255, 255, 255, 0.7);
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <nav class="navbar">
                    <div class="logo">VoxLink</div>
                    <div class="nav-links">
                        <a href="/stats">Statistics</a>
                        <a href="/login">Login</a>
                    </div>
                </nav>
                <div class="container">
                    <div class="hero">
                        <h1>Welcome to VoxLink</h1>
                        <p>A modern, distributed collaborative workspace for teams</p>
                        <a href="/login" class="btn">Get Started</a>
                        <a href="/stats" class="btn btn-outline">View Stats</a>
                    </div>
                    <div class="features">
                        <div class="feature">
                            <h3>💬 Real-time Chat</h3>
                            <p>Instant messaging with typing indicators and read receipts</p>
                        </div>
                        <div class="feature">
                            <h3>📁 File Sharing</h3>
                            <p>Upload and share files up to 100MB with automatic thumbnails</p>
                        </div>
                        <div class="feature">
                            <h3>👥 Team Collaboration</h3>
                            <p>Organize your team with workspaces, channels, and roles</p>
                        </div>
                    </div>
                </div>
                <div class="footer">
                    <p>VoxLink - Distributed Collaborative Workspace | © 2026</p>
                </div>
            </body>
            </html>
        """;
    }
}