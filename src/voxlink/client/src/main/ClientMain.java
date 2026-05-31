package voxlink.client.src.main;

import javafx.application.Application;
import javafx.stage.Stage;
import voxlink.client.src.main.ui.MainWindow;

public class ClientMain extends Application {

    private MainWindow mainWindow;

    @Override
    public void start(Stage primaryStage) {
        mainWindow = new MainWindow(primaryStage);
        mainWindow.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
