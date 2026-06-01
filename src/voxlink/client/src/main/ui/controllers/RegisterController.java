package voxlink.client.src.main.ui.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import voxlink.client.src.main.model.UserModel;

import java.io.IOException;

public class RegisterController {

    @FXML private ImageView confirmIcon;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private ImageView emailIcon;
    @FXML private Label errorLabel;
    @FXML private Hyperlink loginLink;
    @FXML private PasswordField passwordField;
    @FXML private ImageView passwordIcon;
    @FXML private Button registerButton;
    @FXML private TextField usernameField;
    @FXML private ImageView usernameIcon;

    private UserModel userModel;

    @FXML
    public void initialize() {
        userModel = UserModel.getInstance();
    }

    @FXML
    void onConfirmIconClick(MouseEvent event) {}

    @FXML
    void onEmailIconClick(MouseEvent event) {}

    @FXML
    void onGoToLogin(ActionEvent event) {
        openLoginView();
    }

    @FXML
    void onPasswordIconClick(MouseEvent event) {}

    @FXML
    void onRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        registerButton.setDisable(true);
        registerButton.setText("Registering...");

        userModel.register(username, password, email, username, result -> {
            Platform.runLater(() -> {
                registerButton.setDisable(false);
                registerButton.setText("Register");

                if (result.isSuccess()) {
                    openLoginView();
                } else {
                    String err = result.getErrorMessage();
                    showError(err != null && !err.isEmpty() ? err : "Registration failed.");
                }
            });
        });
    }

    @FXML
    void onUsernameIconClick(MouseEvent event) {}

    private void openLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/voxlink/client/src/resources/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/voxlink/client/src/resources/css/login.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("VoxLink - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load login view.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        });
        delay.play();
    }
}
