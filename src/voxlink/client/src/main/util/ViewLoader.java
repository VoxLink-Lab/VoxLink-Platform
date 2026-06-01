package voxlink.client.src.main.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import voxlink.shared.util.Constants;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Loads FXML views and applies consistent styling.
 */
public final class ViewLoader {

    private static final String FXML_BASE = "/fxml/";
    private static final String CSS_BASE = "/css/";

    private ViewLoader() {
    }

    public static Parent load(String fxmlFileName) throws IOException {
        URL url = ViewLoader.class.getResource(FXML_BASE + fxmlFileName);
        if (url == null) {
            throw new IOException("FXML not found: " + FXML_BASE + fxmlFileName);
        }
        return new FXMLLoader(url).load();
    }

    public static void applyStylesheet(Scene scene, String cssFileName) {
        URL url = ViewLoader.class.getResource(CSS_BASE + cssFileName);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    public static void showScene(Stage stage, Parent root, String title, int width, int height, String cssFileName) {
        Scene scene = new Scene(root, width, height);
        applyStylesheet(scene, cssFileName);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMinWidth(Math.min(width, 720));
        stage.setMinHeight(Math.min(height, 480));
        stage.show();
    }

    public static void openLogin(Stage stage) {
        try {
            Parent root = load("Login.fxml");
            showScene(stage, root, Constants.APP_NAME + " — Sign In",
                    Constants.UI_LOGIN_WIDTH, Constants.UI_LOGIN_HEIGHT, "login.css");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load login view", e);
        }
    }

    public static void openRegister(Stage stage) {
        try {
            Parent root = load("RegisterView.fxml");
            showScene(stage, root, Constants.APP_NAME + " — Register",
                    Constants.UI_REGISTER_WIDTH, Constants.UI_REGISTER_HEIGHT, "register.css");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load register view", e);
        }
    }

    public static void openMain(Stage stage) {
        try {
            Parent root = load("MainView.fxml");
            showScene(stage, root, Constants.APP_NAME,
                    Constants.UI_MAIN_WIDTH, Constants.UI_MAIN_HEIGHT, "main.css");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load main view", e);
        }
    }

    public static URL resourceUrl(String path) {
        return Objects.requireNonNull(ViewLoader.class.getResource(path), "Resource not found: " + path);
    }
}
