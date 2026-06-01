package voxlink.client.src.main.ui.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import voxlink.client.src.main.model.UserModel;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.shared.dto.UserDTO;

import java.io.IOException;

public class LoginController {

    @FXML
    private Label errorLabel;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Button loginButton;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField usernameField;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private Label usernameErrorLabel;

    @FXML
    private ToggleButton toggleButton;

    private UserModel userModel;

    @FXML
    public void initialize() {
        userModel = UserModel.getInstance();

        // Establish connection to the server
        ServerConnection connection = ServerConnection.getInstance();
        if (!connection.isConnected()) {
            System.out.println("[LoginController] Connecting to server...");
            boolean connected = connection.connect("localhost", 8888);
            if (connected) {
                System.out.println("[LoginController] Connected successfully");
            } else {
                System.out.println("[LoginController] Connection failed!");
                showGeneralError("Cannot connect to server. Please make sure the server is running.");
            }
        }

        // Set error labels invisible and add style class
        configureErrorLabel(errorLabel);
        configureErrorLabel(usernameErrorLabel);
        configureErrorLabel(passwordErrorLabel);

        // Clear error when user starts typing
        usernameField.textProperty().addListener((obs, old, val) -> clearFieldError(usernameErrorLabel));
        passwordField.textProperty().addListener((obs, old, val) -> clearFieldError(passwordErrorLabel));

        // Allow Enter key to trigger login
        passwordField.setOnAction(event -> onLogin(null));
        usernameField.setOnAction(event -> onLogin(null));

    }

    private void configureErrorLabel(Label label) {
        if (label != null) {
            label.setVisible(false);
            label.getStyleClass().add("error-label");
        }
    }

    @FXML
    void onLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();;

        // Validation
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

        // Disable button and show loading state
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");

        // Call backend
        userModel.login(username, password, result -> {
            Platform.runLater(() -> {
                loginButton.setDisable(false);
                loginButton.setText("Login");

                if (result.isSuccess()) {
                    // Login successful
                    UserDTO user = result.getUser();
                    System.out.println("Login successful: " + user.getUsername());
                    openMainView();
                } else {
                    // Login failed
                    String error = result.getErrorMessage();
                    if (error == null || error.isEmpty()) {
                        error = "Invalid username or password";
                    }
                    showGeneralError(error);
                }
            });
        });
    }

    @FXML
    void onGoToRegister(ActionEvent event) {
        openRegisterView();
    }

    @FXML
    void onForgotPassword(ActionEvent event) {
        showGeneralError("Contact your administrator to reset your password");
    }

    private void openMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/voxlink/client/src/resources/fxml/MainView" +
                    ".fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 700);
            scene.getStylesheets().add(getClass().getResource("/voxlink/client/src/resources/css/main.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("VoxLink");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showGeneralError("Failed to load main view: " + e.getMessage());
        }
    }

    private void openRegisterView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/voxlink/client/src/resources/fxml/RegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/voxlink/client/src/resources/css/register.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("VoxLink - Register");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showGeneralError("Failed to load register view: " + e.getMessage());
        }
    }

    private void showFieldError(String message, Label field) {
        field.setText(message);
        field.setVisible(true);
        if (!field.getStyleClass().contains("error-label")) {
            field.getStyleClass().add("error-label");
        }
    }

    private void showGeneralError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> {
            errorLabel.setVisible(false);
            errorLabel.setText("");
        });
        delay.play();
    }

    private void clearFieldError(Label field) {
        field.setText("");
        field.setVisible(false);
    }
}