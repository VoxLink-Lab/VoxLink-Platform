package voxlink.client.src.main.ui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import voxlink.client.src.main.model.UserModel;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.util.ViewLoader;
import voxlink.shared.util.Constants;

import java.util.regex.Pattern;

public class RegisterController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    @FXML private PasswordField passwordField;
    @FXML private TextField usernameField;

    private UserModel userModel;

    @FXML
    public void initialize() {
        userModel = UserModel.getInstance();
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        String host = System.getProperty("voxlink.host", Constants.DEFAULT_SERVER_HOST);
        int port = Integer.parseInt(System.getProperty("voxlink.port", String.valueOf(Constants.DEFAULT_SERVER_PORT)));
        ServerConnection connection = ServerConnection.getInstance();
        if (!connection.isConnected()) {
            connection.connect(host, port);
        }
    }

    @FXML
    void onRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Enter a valid email address.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        registerButton.setDisable(true);
        registerButton.setText("Creating account...");

        userModel.register(username, password, email, username, result -> Platform.runLater(() -> {
            registerButton.setDisable(false);
            registerButton.setText("Register");

            if (result.isSuccess()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Account created");
                alert.setHeaderText(null);
                alert.setContentText("You can sign in with your new account.");
                alert.showAndWait();
                onGoToLogin(null);
            } else {
                String message = result.getErrorMessage();
                showError(message != null && !message.isBlank() ? message : "Registration failed.");
            }
        }));
    }

    @FXML
    void onGoToLogin(ActionEvent event) {
        Stage stage = (Stage) registerButton.getScene().getWindow();
        ViewLoader.openLogin(stage);
    }

    @FXML
    void onUsernameIconClick(javafx.scene.input.MouseEvent event) {
        usernameField.requestFocus();
    }

    @FXML
    void onEmailIconClick(javafx.scene.input.MouseEvent event) {
        emailField.requestFocus();
    }

    @FXML
    void onPasswordIconClick(javafx.scene.input.MouseEvent event) {
        passwordField.requestFocus();
    }

    @FXML
    void onConfirmIconClick(javafx.scene.input.MouseEvent event) {
        confirmPasswordField.requestFocus();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
