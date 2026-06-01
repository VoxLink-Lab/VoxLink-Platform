package voxlink.client.src.main.ui.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import voxlink.client.src.main.model.UserModel;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.UserDTO;

public class SettingsController {

    @FXML private Label avatarLabel;
    @FXML private Label displayNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label usernameDetailedLabel;
    @FXML private Label usernameLabel;

    private UserStore userStore;

    @FXML
    public void initialize() {
        userStore = UserStore.getInstance();
        populateUserData();
    }

    private void populateUserData() {
        UserDTO user = userStore.getCurrentUser();
        if (user != null) {
            String username = user.getUsername();
            String display = user.getDisplayName();
            String email = user.getEmail();

            usernameLabel.setText(display != null && !display.isEmpty() ? display : username);
            usernameDetailedLabel.setText(username);
            displayNameLabel.setText(display != null && !display.isEmpty() ? display : username);
            emailLabel.setText(email != null && !email.isEmpty() ? email : "No email linked");

            if (username != null && !username.isEmpty()) {
                avatarLabel.setText(username.substring(0, 1).toUpperCase());
            }
        }
    }

    @FXML
    void onClose(ActionEvent event) {
        // Close the settings window
        Stage stage = (Stage) avatarLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    void onLogOut(ActionEvent event) {
        // Log out the user
        UserModel.getInstance().logout();

        javafx.application.Platform.runLater(() -> {
            // Close the settings modal
            onClose(event);

            // Switch main stage back to login
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/voxlink/client/src/resources/fxml/Login.fxml"));
                javafx.scene.Parent root = loader.load();
                // Get the primary stage from the main client class
                Stage stage = voxlink.client.src.main.ClientMain.getPrimaryStage();
                javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 600);
                scene.getStylesheets().add(getClass().getResource("/voxlink/client/src/resources/css/login.css").toExternalForm());
                stage.setScene(scene);
                stage.setTitle("VoxLink - Login");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
