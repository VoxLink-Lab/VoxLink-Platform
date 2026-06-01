package voxlink.client.src.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class ClientMain extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        ClientMain.primaryStage = primaryStage;
        
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/voxlink/client/src/resources/fxml/Login.fxml")));
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/voxlink/client/src/resources/css/login.css")).toExternalForm());
        
        primaryStage.setTitle("VoxLink");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        javafx.application.Application.launch(ClientMain.class, args);
    }
}
