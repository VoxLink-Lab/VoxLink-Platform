package voxlink.client.src.main.ui;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainWindow {

    private final Stage stage;
    private final BorderPane root;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.root = new BorderPane();

        Scene scene = new Scene(root, 1200, 800);

        this.stage.setTitle("VoxLink");
        this.stage.setScene(scene);
        this.stage.setMinWidth(800);
        this.stage.setMinHeight(600);
    }

    public void show() {
        stage.show();
    }
}
