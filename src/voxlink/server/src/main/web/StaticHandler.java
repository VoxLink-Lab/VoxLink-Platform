package voxlink.server.src.main.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * StaticHandler serves static files (HTML, CSS, JS, images) from the classpath.
 */
public class StaticHandler {

    // Handle static file requests
    public void handleStatic(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // Default to index.html for root
        if (path.equals("/")) {
            path = "/index.html";
        }

        // Build resource path
        String resourcePath = "/web" + path;

        // Load resource from classpath
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            send404(exchange, path);
            return;
        }

        String mimeType = getMimeType(path);
        byte[] content = inputStream.readAllBytes();
        inputStream.close();

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private void send404(HttpExchange exchange, String path) throws IOException {
        String response = "404 - File not found: " + path;
        exchange.sendResponseHeaders(404, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private String getMimeType(String path) {
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}