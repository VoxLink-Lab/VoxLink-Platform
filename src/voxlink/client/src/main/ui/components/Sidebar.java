package voxlink.client.src.main.ui.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import voxlink.client.src.main.state.AppState;
import voxlink.shared.dto.WorkspaceDTO;

public class Sidebar {

    private final VBox root;
    private final ListView<WorkspaceDTO> workspaceListView;
    private final ObservableList<WorkspaceDTO> workspaces;
    private final AppState appState;

    public Sidebar() {
        this.appState = AppState.getInstance();
        this.workspaces = FXCollections.observableArrayList();

        this.root = new VBox();
        this.root.setStyle("-fx-background-color: #11111b;");
        this.root.setPadding(new Insets(10));
        this.root.setPrefWidth(80);

        this.workspaceListView = new ListView<>(workspaces);
        this.workspaceListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(workspaceListView, Priority.ALWAYS);

        this.root.getChildren().add(workspaceListView);

        setupListeners();
    }

    private void setupListeners() {
        appState.addListener(new AppState.StateAdapter() {
            @Override
            public void onWorkspacesChanged(java.util.List<WorkspaceDTO> newWorkspaces) {
                workspaces.setAll(newWorkspaces);
            }
        });

        workspaceListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                appState.setCurrentWorkspace(newVal);
            }
        });
    }

    public VBox getView() {
        return root;
    }
}
