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
            server = HttpServer.create(new InetSocketAddress(port), 0);
            configureRoutes();
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            isRunning = true;

            System.out.println("=========================================");
            System.out.println("VoxLink Web Portal Started!");
            System.out.println("Listening on port: " + port);
            System.out.println("=========================================");
            LOGGER.info("Web server started on port " + port);

            return true;

        } catch (IOException e) {
            System.err.println("[WebServer] Failed to start: " + e.getMessage());
            return false;
        }
    }

    // Configure HTTP routes
    private void configureRoutes() {
        // Static files (HTML, CSS, JS)
        server.createContext("/", staticHandler::handleStatic);

        // API endpoints
        server.createContext("/api/stats", statsHandler::handleApiStats);
        server.createContext("/api/validate-invite", inviteHandler::handleApiValidate);
        server.createContext("/api/join", inviteHandler::handleApiJoin);
    }

    // Stop the web server
    public void stop() {
        if (server != null) {
            server.stop(0);
            isRunning = false;
            System.out.println("[WebServer] Stopped");
        }
    }

    public boolean isRunning() { return isRunning; }

    public static void main(String[] args) {
        WebServer webServer = new WebServer();
        int port = ServerConfig.WEB_PORTAL_PORT;

        Runtime.getRuntime().addShutdownHook(new Thread(webServer::stop));
        webServer.start(port);

        while (webServer.isRunning()) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
    }
}