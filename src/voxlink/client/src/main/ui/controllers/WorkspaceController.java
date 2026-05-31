package voxlink.client.src.main.ui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import voxlink.client.src.main.state.AppState;
import voxlink.shared.dto.ChannelDTO;

public class WorkspaceController {

    private final VBox root;
    private final Label workspaceNameLabel;
    private final ListView<ChannelDTO> channelListView;
    private final ObservableList<ChannelDTO> currentChannels;

    private final AppState appState;

    public WorkspaceController() {
        this.appState = AppState.getInstance();
        this.currentChannels = FXCollections.observableArrayList();

        this.root = new VBox(10);
        this.root.setStyle("-fx-background-color: #181825;");
        this.root.setPadding(new Insets(10));
        this.root.setPrefWidth(200);

        this.workspaceNameLabel = new Label("No Workspace Selected");
        this.workspaceNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        this.channelListView = new ListView<>(currentChannels);
        this.channelListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-text-fill: #a6adc8;");
        VBox.setVgrow(channelListView, Priority.ALWAYS);

        this.root.getChildren().addAll(workspaceNameLabel, channelListView);

        setupListeners();
    }

    private void setupListeners() {
        appState.addListener(new AppState.StateAdapter() {
            @Override
            public void onCurrentWorkspaceChanged(voxlink.shared.dto.WorkspaceDTO workspace) {
                if (workspace != null) {
                    workspaceNameLabel.setText(workspace.getName());
                    currentChannels.setAll(workspace.getChannels());
                } else {
                    workspaceNameLabel.setText("No Workspace Selected");
                    currentChannels.clear();
                }
            }
        });

        channelListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                appState.setCurrentChannel(newVal);
            }
        });
    }

    public VBox getView() {
        return root;
    }
}
