package voxlink.client.src.main.ui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import voxlink.client.src.main.model.WorkspaceModel;

public class CreateWorkspaceController {

    @FXML private Button createBtn;
    @FXML private Label errorLabel;
    @FXML private TextField inviteCodeField;
    @FXML private Button joinBtn;
    @FXML private CheckBox publicCheck;
    @FXML private TextField serverDescField;
    @FXML private TextField serverNameField;

    private WorkspaceModel workspaceModel;

    @FXML
    public void initialize() {
        workspaceModel = WorkspaceModel.getInstance();
    }

    @FXML
    void onCancel(ActionEvent event) {
        closeDialog();
    }

    @FXML
    void onCreate(ActionEvent event) {
        String name = serverNameField.getText().trim();
        String desc = serverDescField.getText().trim();
        boolean isPublic = publicCheck.isSelected();

        if (name.isEmpty()) {
            showError("Server name cannot be empty.");
            return;
        }

        createBtn.setDisable(true);
        createBtn.setText("Creating...");

        workspaceModel.createWorkspace(name, desc, isPublic, result -> {
            Platform.runLater(() -> {
                createBtn.setDisable(false);
                createBtn.setText("Create");

                if (result.isSuccess()) {
                    closeDialog();
                } else {
                    showError(result.getErrorMessage());
                }
            });
        });
    }

    @FXML
    void onJoin(ActionEvent event) {
        String code = inviteCodeField.getText().trim();

        if (code.isEmpty()) {
            showError("Please enter an invite code.");
            return;
        }

        joinBtn.setDisable(true);
        joinBtn.setText("Joining...");

        workspaceModel.joinWorkspace(code, result -> {
            Platform.runLater(() -> {
                joinBtn.setDisable(false);
                joinBtn.setText("Join Server");

                if (result.isSuccess()) {
                    closeDialog();
                } else {
                    showError(result.getErrorMessage());
                }
            });
        });
    }

    private void showError(String message) {
        errorLabel.setText(message != null ? message : "An error occurred");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void closeDialog() {
        Stage stage = (Stage) createBtn.getScene().getWindow();
        stage.close();
    }
}
