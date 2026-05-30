package voxlink.server.src.main.config;

public class ServerConfig {
    // Network Settings
    public static final int PORT = 8080;
    public static final int MAX_CONNECTIONS = 100;
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
}
