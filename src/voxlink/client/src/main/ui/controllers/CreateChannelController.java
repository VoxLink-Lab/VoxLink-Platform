package voxlink.client.src.main.ui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import voxlink.client.src.main.model.ChannelModel;
import voxlink.client.src.main.state.AppState;
import voxlink.shared.dto.WorkspaceDTO;

public class CreateChannelController {

    @FXML private TextField channelNameField;
    @FXML private ToggleGroup channelTypeGroup;
    @FXML private Button createBtn;
    @FXML private Label errorLabel;
    @FXML private CheckBox privateCheck;
    @FXML private RadioButton textTypeRadio;
    @FXML private RadioButton voiceTypeRadio;

    private WorkspaceDTO currentWorkspace;

    @FXML
    public void initialize() {
        // Only allow alphanumeric and dashes for channel name
        channelNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("[a-zA-Z0-9-]*")) {
                channelNameField.setText(oldValue);
            }
        });
        
        currentWorkspace = AppState.getInstance().getCurrentWorkspace();
    }

    @FXML
    void onCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void onCreate(ActionEvent event) {
        String name = channelNameField.getText().trim();
        
        if (name.isEmpty()) {
            showError("Channel name cannot be empty");
            return;
        }
        
        if (currentWorkspace == null) {
            showError("No active workspace selected");
            return;
        }
        
        String type = textTypeRadio.isSelected() ? "TEXT" : "VOICE";
        boolean isPrivate = privateCheck.isSelected();
        
        createBtn.setDisable(true);
        createBtn.setText("Creating...");
        errorLabel.setVisible(false);

        ChannelModel.getInstance().createChannel(name, "", currentWorkspace.getId(), type, isPrivate, result -> {
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    closeDialog();
                } else {
                    showError(result.getErrorMessage());
                    createBtn.setDisable(false);
                    createBtn.setText("Create Channel");
                }
            });
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void closeDialog() {
        Stage stage = (Stage) channelNameField.getScene().getWindow();
        stage.close();
    }
}
