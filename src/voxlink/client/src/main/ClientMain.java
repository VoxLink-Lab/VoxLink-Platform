package voxlink.client.src.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.util.ViewLoader;
import voxlink.shared.util.Constants;

/**
 * JavaFX entry point for the VoxLink desktop client.
 */
public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setOnCloseRequest(event -> {
            ServerConnection.getInstance().disconnect();
            Platform.exit();
        });

        ViewLoader.openLogin(primaryStage);
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) {
        String host = System.getProperty("voxlink.host", Constants.DEFAULT_SERVER_HOST);
        int port = Integer.getInteger("voxlink.port", Constants.DEFAULT_SERVER_PORT);
        System.setProperty("voxlink.host", host);
        System.setProperty("voxlink.port", String.valueOf(port));
        launch(args);
    }
}
