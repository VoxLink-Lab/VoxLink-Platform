package voxlink.client.src.main.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;

public class UserProfileController {

    @FXML private Circle avatarCircle;
    @FXML private Label avatarInitial;
    @FXML private Label displayNameLabel;
    @FXML private Circle statusDot;
    @FXML private Label usernameLabel;

    @FXML
    public void initialize() {
        UserDTO user = UserStore.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
            displayNameLabel.setText(displayName);
            usernameLabel.setText(user.getUsername());
            
            String initial = displayName.substring(0, 1).toUpperCase();
            avatarInitial.setText(initial);
            
            // Set random color for avatar based on hash
            Color[] colors = {
                Color.web("#5B5EF8"), Color.web("#27AE60"), Color.web("#E67E22"),
                Color.web("#8E44AD"), Color.web("#E74C3C"), Color.web("#1ABC9C"),
                Color.web("#3498DB"), Color.web("#F1C40F"), Color.web("#2C3E50")
            };
            int index = Math.abs(user.getUsername().hashCode()) % colors.length;
            avatarCircle.setFill(colors[index]);
            
            // Set status color
            UserStatus status = UserStore.getInstance().getCurrentStatus();
            if (status == null) status = UserStatus.OFFLINE;
            
            switch (status) {
                case ONLINE:
                    statusDot.setFill(Color.web("#3ba55c")); // Green
                    break;
                case IDLE:
                    statusDot.setFill(Color.web("#faa61a")); // Yellow
                    break;
                case DO_NOT_DISTURB:
                    statusDot.setFill(Color.web("#ed4245")); // Red
                    break;
                default:
                    statusDot.setFill(Color.web("#747f8d")); // Grey
            }
        }
    }
}
