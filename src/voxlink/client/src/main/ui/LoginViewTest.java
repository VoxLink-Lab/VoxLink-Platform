package voxlink.client.src.main.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class LoginViewTest extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root =
                FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/voxlink/client/src/resources/fxml" +
                        "/Login.fxml")));
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/voxlink/client/src/resources/css" +
                "/login.css")).toExternalForm());
        primaryStage.setTitle("VoxLink - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}