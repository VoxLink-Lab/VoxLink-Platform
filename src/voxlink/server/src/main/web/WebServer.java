package voxlink.server.src.main.web;

import com.sun.net.httpserver.HttpServer;
import voxlink.server.src.main.config.ServerConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * WebServer provides an embedded HTTP server for the VoxLink web portal.
 */
public class WebServer {

    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());

    private HttpServer server;
    private boolean isRunning;

    private final InviteHandler inviteHandler;
    private final StatsHandler statsHandler;
    private final StaticHandler staticHandler;

    public WebServer() {
        this.inviteHandler = new InviteHandler();
        this.statsHandler = new StatsHandler();
        this.staticHandler = new StaticHandler();
    }

    // Start the web server
    public boolean start(int port) {
        try {
            // Create HTTP server
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Configure routes
            configureRoutes();

            // Use virtual threads or cached thread pool for request handling
            server.setExecutor(Executors.newCachedThreadPool());

            // Start server
            server.start();
            isRunning = true;

            System.out.println("=========================================");
            System.out.println("VoxLink Web Portal Started!");
            System.out.println("Listening on port: " + port);
            System.out.println("Invite URL: http://localhost:" + port + "/invite/{code}");
            System.out.println("Stats URL: http://localhost:" + port + "/stats");
            System.out.println("=========================================");
            LOGGER.info("Web server started on port " + port);

            return true;

        } catch (IOException e) {
            System.err.println("[WebServer] Failed to start on port " + port + ": " + e.getMessage());
            LOGGER.severe("Failed to start web server: " + e.getMessage());
            return false;
        }
    }

    // Configure HTTP routes
    private void configureRoutes() {
        // Invite routes
        server.createContext("/invite/", inviteHandler::handleInvite);

        // Statistics routes
        server.createContext("/stats", statsHandler::handleStats);
        server.createContext("/api/stats", statsHandler::handleApiStats);

        // Static files (CSS, JS, images)
        server.createContext("/css/", staticHandler::handleStatic);
        server.createContext("/js/", staticHandler::handleStatic);
        server.createContext("/images/", staticHandler::handleStatic);

        // Dashboard
        server.createContext("/dashboard", staticHandler::handleDashboard);

        // Default route - serve index page
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                staticHandler.handleIndex(exchange);
            } else {
                // 404 Not Found
                String response = "<html><body><h1>404 - Page Not Found</h1></body></html>";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.getResponseBody().close();
            }
        });
    }

    // Stop the web server
    public void stop() {
        if (server != null) {
            server.stop(0);
            isRunning = false;
            System.out.println("[WebServer] Stopped");
            LOGGER.info("Web server stopped");
        }
    }

    // Check if server is running
    public boolean isRunning() {
        return isRunning;
    }

    // Get server port
    public int getPort() {
        return server != null ? server.getAddress().getPort() : -1;
    }

    // Main method for standalone web server startup
    public static void main(String[] args) {
        WebServer webServer = new WebServer();

        int port = ServerConfig.WEB_PORTAL_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + port);
            }
        }

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[WebServer] Shutting down...");
            webServer.stop();
        }));

        webServer.start(port);

        // Keep main thread alive
        while (webServer.isRunning()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}