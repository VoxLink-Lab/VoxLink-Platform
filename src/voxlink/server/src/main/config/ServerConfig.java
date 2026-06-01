package voxlink.server.src.main.config;

public class ServerConfig {
    // Network Settings
    public static final int PORT = 8888;
    public static final int SERVER_PORT = 8888;
    public static final int MAX_CONNECTIONS = 100;
    public static final int SOCKET_TIMEOUT_MS = 30000;
    // Database Settings
    public static final String DB_URL = "jdbc:mysql://localhost:3306/voxlink_db";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "";
    // Application Settings
    public static final String APP_NAME = "VoxLink Platform";
    public static final String VERSION = "1.0.0";
    // RMI settings
    public static final String RMI_SERVICE_NAME = "VoxlinkRemoteService";
    public static final int RMI_PORT = 1099;
    // File Storage Configuration
    public static final String FILE_STORAGE_PATH = "./server_files/";
    public static final String AVATAR_STORAGE_PATH = "./server_files/avatars/";
    public static final String THUMBNAIL_PATH = "./server_files/thumbnails/";
    public static final long MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024;
    // Web Portal Configuration
    public static final int WEB_PORTAL_PORT = 8080;
    public static final String WEB_PORTAL_BASE_URL = "http://localhost:8080";
}
