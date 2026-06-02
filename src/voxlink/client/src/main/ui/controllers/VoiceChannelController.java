package voxlink.client.src.main.ui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import voxlink.shared.dto.ChannelDTO;
import voxlink.shared.dto.UserDTO;

import java.util.List;

public class VoiceChannelController {

    @FXML private Label channelNameLabel;
    @FXML private Button deafenBtn;
    @FXML private Button disconnectBtn;
    @FXML private Button muteBtn;
    @FXML private Button screenBtn;
    @FXML private Button videoBtn;
    @FXML private FlowPane voiceGrid;

    private ChannelDTO currentChannel;
    private voxlink.client.src.main.model.VoiceModel voiceModel;
    
    // Callback to tell MainViewController to close this view
    private Runnable onDisconnectCallback;

    @FXML
    public void initialize() {
        voiceModel = voxlink.client.src.main.model.VoiceModel.getInstance();
    }

    public void setChannel(ChannelDTO channel) {
        this.currentChannel = channel;
        if (channel != null) {
            channelNameLabel.setText(channel.getName() + " (Voice)");
            
            // Connect to voice channel
            voiceModel.joinVoiceChannel(channel.getId(), success -> {
                Platform.runLater(() -> {
                    if (success) {
                        voiceGrid.getChildren().clear();
                        addUserCard(voxlink.client.src.main.state.UserStore.getInstance().getCurrentUser().getUsername(), true);
                    } else {
                        System.err.println("Failed to join voice channel.");
                    }
                });
            });
        }
    }
    
    public void setOnDisconnectCallback(Runnable callback) {
        this.onDisconnectCallback = callback;
    }

    private void addUserCard(String name, boolean isSpeaking) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #202225; -fx-background-radius: 8px; -fx-padding: 20px; -fx-min-width: 200px; -fx-min-height: 250px;");
        
        StackPane avatarPane = new StackPane();
        
        Circle bg = new Circle(40);
        bg.setFill(Color.web("#5865f2"));
        
        // Green border if speaking
        if (isSpeaking) {
            bg.setStroke(Color.web("#3ba55c"));
            bg.setStrokeWidth(4);
        }
        
        Label initial = new Label(name.substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
        
        avatarPane.getChildren().addAll(bg, initial);
        
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        card.getChildren().addAll(avatarPane, nameLabel);
        voiceGrid.getChildren().add(card);
    }

    @FXML
    void onDisconnect(ActionEvent event) {
        voiceModel.leaveVoiceChannel(success -> {
            Platform.runLater(() -> {
                if (onDisconnectCallback != null) {
                    onDisconnectCallback.run();
                }
            });
        });
    }

    @FXML
    void onMute(ActionEvent event) {
        voxlink.client.src.main.media.AudioManager audio = voxlink.client.src.main.media.AudioManager.getInstance();
        audio.setMuted(!audio.isMuted());
        if (audio.isMuted()) {
            muteBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white;");
        } else {
            muteBtn.setStyle("-fx-background-color: #4f545c; -fx-text-fill: white;");
        }
    }

    @FXML
    void onDeafen(ActionEvent event) {
        voxlink.client.src.main.media.AudioManager audio = voxlink.client.src.main.media.AudioManager.getInstance();
        audio.setDeafened(!audio.isDeafened());
        if (audio.isDeafened()) {
            deafenBtn.setStyle("-fx-background-color: #ed4245; -fx-text-fill: white;");
        } else {
            deafenBtn.setStyle("-fx-background-color: #4f545c; -fx-text-fill: white;");
        }
    }
}
