package voxlink.client.src.main.ui.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import voxlink.client.src.main.model.FriendModel;
import voxlink.client.src.main.state.AppState;

public class DirectMessageController {

    @FXML private Button addFriendButton;
    @FXML private VBox friendsListContainer;
    @FXML private VBox emptyStateContainer;
    @FXML private VBox dmListContainer;
    @FXML private Label noDmsLabel;
    @FXML private Button addDmButton;

    private FriendModel friendModel;
    private AppState appState;

    @FXML
    public void initialize() {
        friendModel = FriendModel.getInstance();
        appState = AppState.getInstance();

        setupListeners();
        
        // Fetch initial data
        friendModel.fetchFriends(success -> {
            if (success) {
                javafx.application.Platform.runLater(this::renderFriendsList);
            }
        });

        friendModel.fetchDirectMessages(success -> {
            if (success) {
                javafx.application.Platform.runLater(this::renderDmList);
            }
        });

        // Add friend dialog
        addFriendButton.setOnAction(e -> handleAddFriend());
    }

    private void setupListeners() {
        appState.addListener(new AppState.StateAdapter() {
            @Override
            public void onFriendsChanged(java.util.List<voxlink.shared.dto.UserDTO> friends) {
                javafx.application.Platform.runLater(() -> renderFriendsList());
            }

            @Override
            public void onDirectMessagesChanged(java.util.List<voxlink.shared.dto.ChannelDTO> dms) {
                javafx.application.Platform.runLater(() -> renderDmList());
            }
        });
    }

    private void renderFriendsList() {
        java.util.List<voxlink.shared.dto.UserDTO> friends = appState.getFriends();
        
        if (friends == null || friends.isEmpty()) {
            emptyStateContainer.setVisible(true);
            emptyStateContainer.setManaged(true);
            friendsListContainer.setVisible(false);
            friendsListContainer.setManaged(false);
            return;
        }

        emptyStateContainer.setVisible(false);
        emptyStateContainer.setManaged(false);
        friendsListContainer.setVisible(true);
        friendsListContainer.setManaged(true);
        
        friendsListContainer.getChildren().clear();

        for (voxlink.shared.dto.UserDTO friend : friends) {
            javafx.scene.layout.HBox friendRow = new javafx.scene.layout.HBox(10);
            friendRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            friendRow.setStyle("-fx-padding: 10; -fx-background-color: #2f3136; -fx-background-radius: 4px;");

            // Avatar placeholder
            javafx.scene.shape.Circle avatar = new javafx.scene.shape.Circle(16, javafx.scene.paint.Color.web("#5865F2"));
            
            // Info
            javafx.scene.layout.VBox infoBox = new javafx.scene.layout.VBox(2);
            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(friend.getUsername());
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            javafx.scene.control.Label statusLabel = new javafx.scene.control.Label(friend.getStatus() != null ? friend.getStatus().toString() : "Offline");
            statusLabel.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 11px;");
            infoBox.getChildren().addAll(nameLabel, statusLabel);

            // Actions
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            Button msgBtn = new Button("💬");
            msgBtn.setStyle("-fx-background-color: #202225; -fx-text-fill: #b9bbbe; -fx-background-radius: 50%; -fx-min-width: 36px; -fx-min-height: 36px;");
            msgBtn.setOnAction(e -> {
                friendModel.createDirectMessage(friend.getId(), dm -> {
                    if (dm != null) {
                        System.out.println("DM created/opened with " + friend.getUsername());
                        // TODO: Switch view to DM channel
                    }
                });
            });

            friendRow.getChildren().addAll(avatar, infoBox, spacer, msgBtn);
            friendsListContainer.getChildren().add(friendRow);
        }
    }

    private void renderDmList() {
        java.util.List<voxlink.shared.dto.ChannelDTO> dms = appState.getDirectMessages();
        
        if (dms == null || dms.isEmpty()) {
            noDmsLabel.setVisible(true);
            noDmsLabel.setManaged(true);
            // clear others
            dmListContainer.getChildren().removeIf(node -> node != noDmsLabel);
            return;
        }

        noDmsLabel.setVisible(false);
        noDmsLabel.setManaged(false);
        dmListContainer.getChildren().clear();

        for (voxlink.shared.dto.ChannelDTO dm : dms) {
            javafx.scene.layout.HBox dmRow = new javafx.scene.layout.HBox(8);
            dmRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            dmRow.setStyle("-fx-padding: 8; -fx-background-radius: 4px; -fx-cursor: hand;");
            
            javafx.scene.shape.Circle avatar = new javafx.scene.shape.Circle(12, javafx.scene.paint.Color.web("#5865F2"));
            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label("User " + dm.getName()); // In a real app we'd resolve the other user's name
            nameLabel.setStyle("-fx-text-fill: #8e9297;");
            
            dmRow.getChildren().addAll(avatar, nameLabel);
            dmListContainer.getChildren().add(dmRow);
        }
    }

    private void handleAddFriend() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Add Friend");
        dialog.setHeaderText("Add a friend on VoxLink");
        dialog.setContentText("Enter username:");

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            if (!username.trim().isEmpty()) {
                friendModel.addFriend(username.trim(), success -> {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                success ? javafx.scene.control.Alert.AlertType.INFORMATION : javafx.scene.control.Alert.AlertType.ERROR
                        );
                        alert.setTitle("Friend Request");
                        alert.setHeaderText(null);
                        alert.setContentText(success ? "Friend request sent!" : "Failed to send friend request. User may not exist.");
                        alert.showAndWait();
                    });
                });
            }
        });
    }
}
