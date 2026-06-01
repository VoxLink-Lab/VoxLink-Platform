package voxlink.server.src.main;

/**
 * @deprecated Use {@link ServerMain} for full startup (schema + sockets).
 */
@Deprecated
public class ServerLauncher {
    public static void main(String[] args) {
        ServerMain.main(args);
    }
}
