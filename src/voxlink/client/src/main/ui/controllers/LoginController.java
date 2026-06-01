package voxlink.client.src.main.ui.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import voxlink.client.src.main.model.UserModel;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.util.ViewLoader;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.util.Constants;

public class LoginController {

    @FXML private Label errorLabel;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginButton;
    @FXML private PasswordField passwordField;
    @FXML private TextField usernameField;
    @FXML private Label passwordErrorLabel;
    @FXML private Label usernameErrorLabel;
    @FXML private ToggleButton toggleButton;

    private UserModel userModel;
    private TextField visiblePasswordField;

    @FXML
    public void initialize() {
        userModel = UserModel.getInstance();
        configureErrorLabel(errorLabel);
        configureErrorLabel(usernameErrorLabel);
        configureErrorLabel(passwordErrorLabel);

        connectToServer();

        usernameField.textProperty().addListener((obs, old, val) -> clearFieldError(usernameErrorLabel));
        passwordField.textProperty().addListener((obs, old, val) -> clearFieldError(passwordErrorLabel));

        passwordField.setOnAction(event -> onLogin(null));
        usernameField.setOnAction(event -> onLogin(null));

        setupPasswordToggle();
    }

    private void connectToServer() {
        String host = System.getProperty("voxlink.host", Constants.DEFAULT_SERVER_HOST);
        int port = Integer.parseInt(System.getProperty("voxlink.port", String.valueOf(Constants.DEFAULT_SERVER_PORT)));

        ServerConnection connection = ServerConnection.getInstance();
        if (!connection.isConnected()) {
            boolean connected = connection.connect(host, port);
            if (!connected) {
                showGeneralError("Cannot connect to " + host + ":" + port + ". Start the VoxLink server first.");
            }
        }
    }

    private void setupPasswordToggle() {
        if (toggleButton == null) {
            return;
        }
        visiblePasswordField = new TextField();
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.getStyleClass().addAll(passwordField.getStyleClass());
        visiblePasswordField.setPromptText(passwordField.getPromptText());
        visiblePasswordField.setPrefColumnCount(passwordField.getPrefColumnCount());

        if (passwordField.getParent() instanceof javafx.scene.layout.HBox parent) {
            int index = parent.getChildren().indexOf(passwordField);
            parent.getChildren().add(index + 1, visiblePasswordField);
            javafx.scene.layout.HBox.setHgrow(visiblePasswordField, javafx.scene.layout.Priority.ALWAYS);
        }

        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        toggleButton.setOnAction(e -> {
            boolean show = visiblePasswordField.isVisible();
            visiblePasswordField.setVisible(!show);
            visiblePasswordField.setManaged(!show);
            passwordField.setVisible(show);
            passwordField.setManaged(show);
            toggleButton.setText(show ? "👁" : "🙈");
        });
        toggleButton.setText("👁");
    }

    @FXML
    void onLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            showFieldError("Please enter your username or email", usernameErrorLabel);
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showFieldError("Please enter your password", passwordErrorLabel);
            passwordField.requestFocus();
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Logging in...");

        userModel.login(username, password, result -> Platform.runLater(() -> {
            loginButton.setDisable(false);
            loginButton.setText("Login");

            if (result.isSuccess()) {
                UserDTO user = result.getUser();
                System.out.println("[UI] Login successful: " + user.getUsername());
                Stage stage = (Stage) loginButton.getScene().getWindow();
                ViewLoader.openMain(stage);
            } else {
                String error = result.getErrorMessage();
                if (error == null || error.isBlank()) {
                    error = "Invalid username or password";
                }
                showGeneralError(error);
            }
        }));
    }

    @FXML
    void onGoToRegister(ActionEvent event) {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        ViewLoader.openRegister(stage);
    }

    @FXML
    void onForgotPassword(ActionEvent event) {
        showGeneralError("Contact your administrator to reset your password.");
    }

    private void configureErrorLabel(Label label) {
        if (label != null) {
            label.setVisible(false);
            label.setManaged(false);
            if (!label.getStyleClass().contains("error-label")) {
                label.getStyleClass().add("error-label");
            }
        }
    }

    private void showFieldError(String message, Label field) {
        field.setText(message);
        field.setVisible(true);
        field.setManaged(true);
    }

    private void showGeneralError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(event -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            errorLabel.setText("");
        });
        delay.play();
    }

    private void clearFieldError(Label field) {
        field.setText("");
        field.setVisible(false);
        field.setManaged(false);
    }
}
