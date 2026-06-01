package voxlink.client.src.main.ui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import voxlink.client.src.main.model.ChannelModel;
import voxlink.client.src.main.model.MessageModel;
import voxlink.client.src.main.model.UserModel;
import voxlink.client.src.main.model.WorkspaceModel;
import voxlink.client.src.main.network.FileUploader;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.MessageCache;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.ChannelDTO;
import voxlink.shared.dto.ChannelType;
import voxlink.shared.dto.MessageDTO;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;
import voxlink.shared.dto.WorkspaceDTO;
import voxlink.shared.protocol.Packet;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainViewController {

    @FXML private Label activeChannelLabel;
    @FXML private Button addTextChannelBtn;
    @FXML private Button attachBtn;
    @FXML private VBox channelSidebar;
    @FXML private VBox chatArea;
    @FXML private VBox messageFeed;
    @FXML private ScrollPane messageFeedScroll;
    @FXML private TextField messageInput;
    @FXML private HBox notification;
    @FXML private Label memberCountLabel;
    @FXML private VBox memberList;
    @FXML private Label onlineCountLabel;
    @FXML private TextField searchField;
    @FXML private HBox searchIcon;
    @FXML private Label selfAvatarLabel;
    @FXML private Label selfNameLabel;
    @FXML private HBox selfPanel;
    @FXML private HBox settings;
    @FXML private Label selfStatusLabel;
    @FXML private Label serverNameLabel;
    @FXML private VBox textChannelList;
    @FXML private VBox voiceChannelList;
    @FXML private VBox workspaceIconContainer;

    private UserModel userModel;
    private WorkspaceModel workspaceModel;
    private ChannelModel channelModel;
    private MessageModel messageModel;
    private AppState appState;
    private UserStore userStore;
    private MessageCache messageCache;

    private WorkspaceDTO currentWorkspace;
    private ChannelDTO currentChannel;
    private final Map<Integer, HBox> channelRows = new HashMap<>();
    private final Map<Integer, StackPane> workspaceIcons = new HashMap<>();
    private final Map<String, Color> userAvatarColors = new HashMap<>();

    private final Color[] avatarColors = {
            Color.web("#5B5EF8"), Color.web("#27AE60"), Color.web("#E67E22"),
            Color.web("#8E44AD"), Color.web("#E74C3C"), Color.web("#1ABC9C"),
            Color.web("#3498DB"), Color.web("#F1C40F"), Color.web("#2C3E50")
    };

    @FXML
    public void initialize() {
        userModel = UserModel.getInstance();
        workspaceModel = WorkspaceModel.getInstance();
        channelModel = ChannelModel.getInstance();
        messageModel = MessageModel.getInstance();
        appState = AppState.getInstance();
        userStore = UserStore.getInstance();
        messageCache = MessageCache.getInstance();

        setupEventHandlers();
        loadCurrentUser();
        loadWorkspaces();
        setupMessageListener();
        setupSearch();
        setupTypingListener();

        appState.addListener(new AppState.StateAdapter() {
            @Override
            public void onChannelMembersChanged(List<UserDTO> members) {
                Platform.runLater(() -> renderMembers(members));
            }
        });
    }

    private void setupEventHandlers() {
        searchIcon.setOnMouseClicked(e -> toggleSearch());
        notification.setOnMouseClicked(e -> showInfo("Notifications", "Mentions and replies will appear here in a future release."));
        settings.setOnMouseClicked(e -> openSettings());
    }

    private void setupTypingListener() {
        messageInput.textProperty().addListener((obs, old, text) -> {
            if (currentChannel != null && text != null && !text.isBlank()) {
                messageModel.startTyping(currentChannel.getId());
            }
        });
        messageInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && searchField.isVisible()) {
                searchField.setVisible(false);
                searchField.setManaged(false);
                searchField.clear();
            }
        });
    }

    private void loadCurrentUser() {
        UserDTO currentUser = userStore.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String displayName = Optional.ofNullable(currentUser.getDisplayName())
                .filter(s -> !s.isBlank())
                .orElse(currentUser.getUsername());
        String initial = displayName.substring(0, 1).toUpperCase();
        selfAvatarLabel.setText(initial);
        selfNameLabel.setText(displayName);
        updateSelfStatus();
    }

    private void updateSelfStatus() {
        UserStatus status = userStore.getCurrentStatus();
        switch (status) {
            case ONLINE -> {
                selfStatusLabel.setText("● Online");
                selfStatusLabel.setStyle("-fx-text-fill: #3bcf7e;");
            }
            case IDLE -> {
                selfStatusLabel.setText("● Idle");
                selfStatusLabel.setStyle("-fx-text-fill: #f0a030;");
            }
            case DO_NOT_DISTURB -> {
                selfStatusLabel.setText("● Do not disturb");
                selfStatusLabel.setStyle("-fx-text-fill: #ed4245;");
            }
            default -> {
                selfStatusLabel.setText("● Offline");
                selfStatusLabel.setStyle("-fx-text-fill: #8888aa;");
            }
        }
    }

    private void loadWorkspaces() {
        workspaceModel.fetchWorkspaces(result -> Platform.runLater(() -> {
            workspaceIconContainer.getChildren().clear();
            workspaceIcons.clear();

            if (!result.isSuccess()) {
                showError("Workspaces", result.getErrorMessage());
                return;
            }

            List<WorkspaceDTO> workspaces = result.getWorkspaces();
            if (workspaces == null || workspaces.isEmpty()) {
                serverNameLabel.setText("No workspaces yet");
                onlineCountLabel.setText("Create one with +");
                return;
            }

            for (WorkspaceDTO workspace : workspaces) {
                addWorkspaceIcon(workspace);
            }
            selectWorkspace(workspaces.get(0));
        }));
    }

    private void addWorkspaceIcon(WorkspaceDTO workspace) {
        StackPane icon = createWorkspaceIcon(workspace);
        workspaceIcons.put(workspace.getId(), icon);
        workspaceIconContainer.getChildren().add(icon);
    }

    private StackPane createWorkspaceIcon(WorkspaceDTO workspace) {
        StackPane container = new StackPane();
        container.setPrefSize(40, 40);
        container.setMaxSize(40, 40);
        container.getStyleClass().add("workspace-icon");
        container.setUserData(workspace);

        Circle circle = new Circle(20);
        circle.setFill(getWorkspaceColor(workspace.getId()));

        String name = workspace.getName();
        String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.substring(0, 1).toUpperCase();
        Label label = new Label(initials);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        container.getChildren().addAll(circle, label);
        container.setOnMouseClicked(e -> selectWorkspace(workspace));
        return container;
    }

    private Color getWorkspaceColor(int workspaceId) {
        return avatarColors[Math.abs(workspaceId) % avatarColors.length];
    }

    private void selectWorkspace(WorkspaceDTO workspace) {
        currentWorkspace = workspace;
        workspaceModel.setCurrentWorkspace(workspace);
        serverNameLabel.setText(workspace.getName());

        int members = workspace.getMemberCount() > 0 ? workspace.getMemberCount() : 1;
        onlineCountLabel.setText(members + " member" + (members == 1 ? "" : "s"));

        workspaceIcons.forEach((id, icon) -> {
            boolean active = id == workspace.getId();
            icon.getStyleClass().removeAll("workspace-icon-active", "workspace-icon");
            icon.getStyleClass().add(active ? "workspace-icon-active" : "workspace-icon");
        });

        loadChannels(workspace.getId());
        refreshOnlineMembers(workspace.getId());
    }

    private void loadChannels(int workspaceId) {
        channelModel.fetchChannels(workspaceId, result -> Platform.runLater(() -> {
            if (result.isSuccess() && result.getChannels() != null) {
                updateChannelList(result.getChannels());
            } else if (!result.isSuccess()) {
                showError("Channels", result.getErrorMessage());
            }
        }));
    }

    private void updateChannelList(List<ChannelDTO> channels) {
        textChannelList.getChildren().clear();
        voiceChannelList.getChildren().clear();
        channelRows.clear();

        ChannelDTO firstText = null;
        for (ChannelDTO channel : channels) {
            HBox row = createChannelRow(channel);
            channelRows.put(channel.getId(), row);
            if (channel.getType() == ChannelType.VOICE) {
                voiceChannelList.getChildren().add(row);
            } else {
                textChannelList.getChildren().add(row);
                if (firstText == null) {
                    firstText = channel;
                }
            }
        }

        if (firstText != null) {
            selectChannel(firstText);
        } else if (!channels.isEmpty()) {
            selectChannel(channels.get(0));
        } else {
            currentChannel = null;
            activeChannelLabel.setText("no channel");
            messageFeed.getChildren().clear();
        }
    }

    private HBox createChannelRow(ChannelDTO channel) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPrefHeight(32);
        row.getStyleClass().add("channel-row");
        row.setUserData(channel);
        row.setPadding(new Insets(5, 8, 5, 8));

        Label iconLabel = new Label(switch (channel.getType()) {
            case VOICE -> channel.getType().getIcon();
            case ANNOUNCEMENT -> channel.getType().getIcon();
            default -> "#";
        });
        iconLabel.getStyleClass().add(channel.getType() == ChannelType.VOICE ? "channel-voice-icon" : "channel-hash");

        Label nameLabel = new Label(channel.getName());
        nameLabel.getStyleClass().add("channel-name");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        row.getChildren().addAll(iconLabel, nameLabel);
        row.setOnMouseClicked(e -> selectChannel(channel));
        return row;
    }

    private void selectChannel(ChannelDTO channel) {
        currentChannel = channel;
        channelModel.setCurrentChannel(channel);
        activeChannelLabel.setText(channel.getName());
        messageInput.setPromptText("Message #" + channel.getName());

        channelRows.forEach((id, row) -> {
            boolean active = id == channel.getId();
            row.getStyleClass().removeAll("channel-row-active", "channel-row");
            row.getStyleClass().add(active ? "channel-row-active" : "channel-row");
            Label nameLabel = (Label) row.getChildren().get(1);
            nameLabel.getStyleClass().removeAll("channel-name-active", "channel-name");
            nameLabel.getStyleClass().add(active ? "channel-name-active" : "channel-name");
        });

        Runnable afterJoin = () -> Platform.runLater(() -> loadMessageHistory(channel.getId()));

        if (channel.isHasJoined()) {
            afterJoin.run();
        } else {
            channelModel.joinChannel(channel.getId(), result -> {
                if (result.isSuccess()) {
                    channel.setHasJoined(true);
                    afterJoin.run();
                } else {
                    Platform.runLater(() -> showError("Channel", result.getErrorMessage()));
                }
            });
        }
    }

    private void loadMessageHistory(int channelId) {
        messageFeed.getChildren().clear();

        List<MessageDTO> cached = messageCache.getMessages(channelId);
        if (!cached.isEmpty()) {
            displayMessages(cached);
        }

        channelModel.getMessageHistory(channelId, 50, 0, result -> Platform.runLater(() -> {
            if (result.isSuccess() && result.getMessages() != null) {
                displayMessages(result.getMessages());
            }
        }));
    }

    private void displayMessages(List<MessageDTO> messages) {
        messageFeed.getChildren().clear();
        String lastDate = "";
        for (MessageDTO message : messages) {
            String messageDate = formatDate(message.getSentAt());
            if (!messageDate.equals(lastDate)) {
                addDateDivider(messageDate);
                lastDate = messageDate;
            }
            addMessageToFeed(message);
        }
        Platform.runLater(() -> messageFeedScroll.setVvalue(1.0));
    }

    private void addDateDivider(String date) {
        HBox divider = new HBox();
        divider.setAlignment(Pos.CENTER);
        divider.setMaxWidth(Double.MAX_VALUE);

        Region left = new Region();
        left.getStyleClass().add("date-divider-line");
        HBox.setHgrow(left, Priority.ALWAYS);
        Region right = new Region();
        right.getStyleClass().add("date-divider-line");
        HBox.setHgrow(right, Priority.ALWAYS);

        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("date-divider-label");
        divider.getChildren().addAll(left, dateLabel, right);
        messageFeed.getChildren().add(divider);
    }

    private void addMessageToFeed(MessageDTO message) {
        HBox messageGroup = new HBox(12);
        messageGroup.setAlignment(Pos.TOP_LEFT);
        messageGroup.getStyleClass().add("msg-group");
        messageGroup.setMaxWidth(Double.MAX_VALUE);

        String username = message.getSenderUsername() != null ? message.getSenderUsername() : "user";
        StackPane avatar = buildAvatar(username, 36);

        VBox contentBox = new VBox(4);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        String display = message.getSenderDisplayName() != null ? message.getSenderDisplayName() : username;
        Label authorLabel = new Label(display);
        authorLabel.getStyleClass().add("msg-author");
        Label timeLabel = new Label(formatTime(message.getSentAt()));
        timeLabel.getStyleClass().add("msg-time");

        HBox header = new HBox(8, authorLabel, timeLabel);
        Label messageText = new Label(message.getContent());
        messageText.getStyleClass().add("msg-text");
        messageText.setWrapText(true);
        messageText.setMaxWidth(720);

        contentBox.getChildren().addAll(header, messageText);
        messageGroup.getChildren().addAll(avatar, contentBox);
        messageFeed.getChildren().add(messageGroup);
    }

    private StackPane buildAvatar(String username, double size) {
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(size, size);
        avatarContainer.getStyleClass().add("msg-avatar");

        Circle circle = new Circle(size / 2);
        circle.setFill(getUserAvatarColor(username));

        String initial = username.isEmpty() ? "?" : username.substring(0, 1).toUpperCase();
        Label initialLabel = new Label(initial);
        initialLabel.setTextFill(Color.WHITE);
        initialLabel.setStyle("-fx-font-weight: bold;");

        avatarContainer.getChildren().addAll(circle, initialLabel);
        return avatarContainer;
    }

    private Color getUserAvatarColor(String username) {
        return userAvatarColors.computeIfAbsent(username,
                u -> avatarColors[Math.abs(u.hashCode()) % avatarColors.length]);
    }

    private void setupMessageListener() {
        ServerConnection.getInstance().addPacketListener(this::onPacket);
    }

    private void onPacket(Packet packet) {
        if (packet.getResponseType() == null) {
            return;
        }
        switch (packet.getResponseType()) {
            case MESSAGE_BROADCAST -> {
                MessageDTO message = (MessageDTO) packet.get("message");
                if (message != null && currentChannel != null && message.getChannelId() == currentChannel.getId()) {
                    Platform.runLater(() -> {
                        addMessageToFeed(message);
                        messageFeedScroll.setVvalue(1.0);
                    });
                }
                messageModel.handleNewMessageBroadcast(packet);
            }
            case USER_PRESENCE_BROADCAST -> Platform.runLater(() -> {
                if (currentWorkspace != null) {
                    refreshOnlineMembers(currentWorkspace.getId());
                }
            });
            default -> { }
        }
    }

    private void refreshOnlineMembers(int workspaceId) {
        userModel.fetchOnlineUsers(workspaceId, result -> Platform.runLater(() -> {
            UserDTO self = userStore.getCurrentUser();
            if (result.isSuccess() && result.getUsers() != null && !result.getUsers().isEmpty()) {
                renderMembers(result.getUsers());
            } else if (self != null) {
                renderMembers(List.of(self));
            } else {
                memberList.getChildren().clear();
                memberCountLabel.setText("0");
            }
        }));
    }

    private void renderMembers(List<UserDTO> members) {
        memberList.getChildren().clear();
        int online = 0;
        for (UserDTO user : members) {
            if (user.getStatus() == UserStatus.ONLINE || user.getStatus() == UserStatus.IDLE) {
                online++;
            }
            memberList.getChildren().add(buildMemberRow(user));
        }
        memberCountLabel.setText(String.valueOf(Math.max(online, members.size())));
    }

    private HBox buildMemberRow(UserDTO user) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("member-row");
        row.setPadding(new Insets(4, 4, 4, 4));

        String name = Optional.ofNullable(user.getDisplayName())
                .filter(s -> !s.isBlank())
                .orElse(user.getUsername());

        StackPane avatar = buildAvatar(user.getUsername(), 36);
        Region statusDot = new Region();
        statusDot.setMaxSize(9, 9);
        statusDot.getStyleClass().add(statusStyleClass(user.getStatus()));
        StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
        avatar.getChildren().add(statusDot);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("member-name");

        row.getChildren().addAll(avatar, nameLabel);
        return row;
    }

    private String statusStyleClass(UserStatus status) {
        if (status == null) {
            return "member-status-offline";
        }
        return switch (status) {
            case ONLINE -> "member-status-online";
            case IDLE -> "member-status-idle";
            default -> "member-status-offline";
        };
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            channelRows.forEach((id, row) -> {
                ChannelDTO channel = (ChannelDTO) row.getUserData();
                boolean matches = query.isEmpty() || channel.getName().toLowerCase().contains(query);
                row.setVisible(matches);
                row.setManaged(matches);
            });
        });
    }

    private void toggleSearch() {
        boolean show = !searchField.isVisible();
        searchField.setVisible(show);
        searchField.setManaged(show);
        if (show) {
            searchField.requestFocus();
        } else {
            searchField.clear();
        }
    }

    private void openSettings() {
        ChoiceDialog<UserStatus> dialog = new ChoiceDialog<>(userStore.getCurrentStatus(), UserStatus.values());
        dialog.setTitle("Your status");
        dialog.setHeaderText("Set how others see you");
        dialog.setContentText("Status:");

        dialog.showAndWait().ifPresent(status -> userModel.updateStatus(status, ok -> Platform.runLater(this::updateSelfStatus)));
    }

    @FXML
    void onAddServer(MouseEvent event) {
        ChoiceDialog<String> choice = new ChoiceDialog<>("Create workspace", "Create workspace", "Join with invite code");
        choice.setTitle("Workspace");
        choice.setHeaderText("Add a workspace");
        choice.setContentText("Action:");

        Optional<String> action = choice.showAndWait();
        if (action.isEmpty()) {
            return;
        }

        if ("Join with invite code".equals(action.get())) {
            TextInputDialog inviteDialog = new TextInputDialog();
            inviteDialog.setTitle("Join workspace");
            inviteDialog.setHeaderText("Enter invite code");
            inviteDialog.setContentText("Code:");
            inviteDialog.showAndWait().ifPresent(code -> {
                if (!code.isBlank()) {
                    workspaceModel.joinWorkspace(code.trim(), result -> Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            addWorkspaceIcon(result.getWorkspace());
                            selectWorkspace(result.getWorkspace());
                        } else {
                            showError("Join workspace", result.getErrorMessage());
                        }
                    }));
                }
            });
        } else {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("Create workspace");
            nameDialog.setHeaderText("Name your new workspace");
            nameDialog.setContentText("Name:");
            nameDialog.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    workspaceModel.createWorkspace(name.trim(), "", true, result -> Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            addWorkspaceIcon(result.getWorkspace());
                            selectWorkspace(result.getWorkspace());
                        } else {
                            showError("Create workspace", result.getErrorMessage());
                        }
                    }));
                }
            });
        }
    }

    @FXML
    void onAddTextChannel(ActionEvent event) {
        promptCreateChannel("TEXT");
    }

    @FXML
    void onAddVoiceChannel(ActionEvent event) {
        promptCreateChannel("VOICE");
    }

    private void promptCreateChannel(String type) {
        if (currentWorkspace == null) {
            showError("Channel", "Select a workspace first.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New channel");
        dialog.setHeaderText("Create a " + type.toLowerCase() + " channel");
        dialog.setContentText("Name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank()) {
                channelModel.createChannel(name.trim(), null, currentWorkspace.getId(), type, false,
                        result -> Platform.runLater(() -> {
                            if (result.isSuccess()) {
                                loadChannels(currentWorkspace.getId());
                                selectChannel(result.getChannel());
                            } else {
                                showError("Channel", result.getErrorMessage());
                            }
                        }));
            }
        });
    }

    @FXML
    void onAttachFile(ActionEvent event) {
        if (currentChannel == null) {
            showError("Upload", "Select a channel first.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach a file");
        File file = chooser.showOpenDialog(messageInput.getScene().getWindow());
        if (file == null) {
            return;
        }

        attachBtn.setDisable(true);
        new FileUploader().uploadFile(file, currentChannel.getId(), 0)
                .whenComplete((attachment, error) -> Platform.runLater(() -> {
                    attachBtn.setDisable(false);
                    if (error != null) {
                        showError("Upload failed", error.getMessage());
                    } else {
                        showInfo("Upload complete", "File \"" + attachment.getFileName() + "\" uploaded.");
                    }
                }));
    }

    @FXML
    void onEmoji(ActionEvent event) {
        messageInput.appendText(" 😊");
        messageInput.requestFocus();
    }

    @FXML
    void onSendMessage(ActionEvent event) {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || currentChannel == null) {
            return;
        }
        messageInput.clear();

        messageModel.sendMessage(content, currentChannel.getId(), null, result -> Platform.runLater(() -> {
            if (!result.isSuccess()) {
                showError("Message", result.getErrorMessage());
            }
        }));
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown date";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : "Something went wrong.");
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
