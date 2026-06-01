package voxlink.client.src.main.ui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import voxlink.client.src.main.model.ChannelModel;
import voxlink.client.src.main.model.MessageModel;
import voxlink.client.src.main.model.UserModel;
import voxlink.client.src.main.model.WorkspaceModel;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.MessageCache;
import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.ChannelDTO;
import voxlink.shared.dto.MessageDTO;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;
import voxlink.shared.dto.WorkspaceDTO;
import voxlink.shared.protocol.Packet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainViewController {

    @FXML private Label activeChannelLabel;
    @FXML private Button addTextChannelBtn;
    @FXML private Button attachBtn;
    @FXML private VBox channelSidebar;
    @FXML private VBox chatArea;
    @FXML private Button emojiBtn;
    @FXML private Label memberCountLabel;
    @FXML private VBox memberList;
    @FXML private VBox memberListPanel;
    @FXML private VBox messageFeed;
    @FXML private ScrollPane messageFeedScroll;
    @FXML private TextField messageInput;
    @FXML private HBox notification;
    @FXML private Label onlineCountLabel;
    @FXML private TextField searchField;
    @FXML private HBox searchIcon;
    @FXML private Label selfAvatarLabel;
    @FXML private Label selfNameLabel;
    @FXML private HBox selfPanel;
    @FXML private Label selfStatusLabel;
    @FXML private Button sendBtn;
    @FXML private Label serverNameLabel;
    @FXML private VBox serverRail;
    @FXML private HBox settings;
    @FXML private VBox textChannelList;
    @FXML private VBox voiceChannelList;
    @FXML private VBox workspaceIconContainer;

    // Models
    private UserModel userModel;
    private WorkspaceModel workspaceModel;
    private ChannelModel channelModel;
    private MessageModel messageModel;
    private AppState appState;
    private UserStore userStore;
    private MessageCache messageCache;

    // State tracking
    private WorkspaceDTO currentWorkspace;
    private ChannelDTO currentChannel;
    private Map<Integer, HBox> channelRows;
    private Map<Integer, StackPane> workspaceIcons;
    private Random random;

    // Colors for avatars
    private final Color[] avatarColors = {
            Color.web("#5B5EF8"), Color.web("#27AE60"), Color.web("#E67E22"),
            Color.web("#8E44AD"), Color.web("#E74C3C"), Color.web("#1ABC9C"),
            Color.web("#3498DB"), Color.web("#F1C40F"), Color.web("#2C3E50")
    };
    private final Map<String, Color> userAvatarColors = new HashMap<>();

    @FXML
    public void initialize() {
        // Initialize models
        userModel = UserModel.getInstance();
        workspaceModel = WorkspaceModel.getInstance();
        channelModel = ChannelModel.getInstance();
        messageModel = MessageModel.getInstance();
        appState = AppState.getInstance();
        userStore = UserStore.getInstance();
        messageCache = MessageCache.getInstance();
        channelRows = new HashMap<>();
        workspaceIcons = new HashMap<>();
        random = new Random();

        // Setup UI event handlers
        setupEventHandlers();

        // Load user data
        loadCurrentUser();

        // Load workspaces
        loadWorkspaces();

        // Setup message listener
        setupMessageListener();

        // Apply search functionality
        setupSearch();
    }

    private void setupEventHandlers() {
        // Search icon click
        searchIcon.setOnMouseClicked(e -> toggleSearch());

        // Settings click
        settings.setOnMouseClicked(e -> openSettings());

        // Notification click
        notification.setOnMouseClicked(e -> toggleNotifications());

        // Add text channel button
        addTextChannelBtn.setOnAction(this::onAddTextChannel);
    }

    private void loadCurrentUser() {
        UserDTO currentUser = userStore.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String initial = displayName != null && !displayName.isEmpty()
                    ? displayName.substring(0, 1).toUpperCase()
                    : currentUser.getUsername().substring(0, 1).toUpperCase();
            selfAvatarLabel.setText(initial);
            selfNameLabel.setText(displayName != null ? displayName : currentUser.getUsername());
            updateSelfStatus();
        }
    }

    private void updateSelfStatus() {
        UserStatus status = userStore.getCurrentStatus();
        switch (status) {
            case ONLINE:
                selfStatusLabel.setText("● Online");
                selfStatusLabel.setStyle("-fx-text-fill: #3bcf7e;");
                break;
            case IDLE:
                selfStatusLabel.setText("🟡 Idle");
                selfStatusLabel.setStyle("-fx-text-fill: #f0a030;");
                break;
            case DO_NOT_DISTURB:
                selfStatusLabel.setText("🔴 DND");
                selfStatusLabel.setStyle("-fx-text-fill: #ed4245;");
                break;
            default:
                selfStatusLabel.setText("⚫ Offline");
                selfStatusLabel.setStyle("-fx-text-fill: #55557a;");
        }
    }

    private void loadWorkspaces() {
        workspaceModel.fetchWorkspaces(result -> {
            Platform.runLater(() -> {
                if (result.isSuccess() && result.getWorkspaces() != null) {
                    for (WorkspaceDTO workspace : result.getWorkspaces()) {
                        addWorkspaceIcon(workspace);
                    }

                    if (!result.getWorkspaces().isEmpty()) {
                        selectWorkspace(result.getWorkspaces().get(0));
                    }
                }
            });
        });
    }

    private void addWorkspaceIcon(WorkspaceDTO workspace) {
        StackPane iconContainer = createWorkspaceIcon(workspace);
        workspaceIcons.put(workspace.getId(), iconContainer);
        workspaceIconContainer.getChildren().add(iconContainer);
    }

    private StackPane createWorkspaceIcon(WorkspaceDTO workspace) {
        StackPane container = new StackPane();
        container.setPrefSize(40, 40);
        container.setMaxSize(40, 40);
        container.getStyleClass().add("workspace-icon");
        container.setUserData(workspace);

        // Get initials
        String name = workspace.getName();
        String initials = name.length() > 1 ? name.substring(0, 2).toUpperCase() : name.substring(0, 1).toUpperCase();

        // Create colored circle
        Circle circle = new Circle(20);
        Color color = getWorkspaceColor(workspace.getId());
        circle.setFill(color);

        // Create label
        Label label = new Label(initials);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        container.getChildren().addAll(circle, label);

        container.setOnMouseClicked(e -> selectWorkspace(workspace));

        return container;
    }

    private Color getWorkspaceColor(int workspaceId) {
        Color[] colors = {
                Color.web("#5B5EF8"), Color.web("#27AE60"), Color.web("#E67E22"),
                Color.web("#8E44AD"), Color.web("#E74C3C"), Color.web("#1ABC9C"),
                Color.web("#3498DB"), Color.web("#F1C40F"), Color.web("#2C3E50")
        };
        return colors[Math.abs(workspaceId) % colors.length];
    }

    private void selectWorkspace(WorkspaceDTO workspace) {
        this.currentWorkspace = workspace;
        serverNameLabel.setText(workspace.getName());

        // Update active state on icons
        for (Map.Entry<Integer, StackPane> entry : workspaceIcons.entrySet()) {
            boolean isActive = entry.getKey() == workspace.getId();
            entry.getValue().getStyleClass().removeAll("workspace-icon-active", "workspace-icon");
            entry.getValue().getStyleClass().add(isActive ? "workspace-icon-active" : "workspace-icon");
        }

        // Load channels for this workspace
        loadChannels(workspace.getId());
    }

    private void loadChannels(int workspaceId) {
        channelModel.fetchChannels(workspaceId, result -> {
            Platform.runLater(() -> {
                if (result.isSuccess() && result.getChannels() != null) {
                    updateChannelList(result.getChannels());
                }
            });
        });
    }

    private void updateChannelList(List<ChannelDTO> channels) {
        // Clear existing lists
        textChannelList.getChildren().clear();
        voiceChannelList.getChildren().clear();
        channelRows.clear();

        for (ChannelDTO channel : channels) {
            HBox channelRow = createChannelRow(channel);
            channelRows.put(channel.getId(), channelRow);

            if (channel.getType().name().equals("VOICE")) {
                voiceChannelList.getChildren().add(channelRow);
            } else {
                textChannelList.getChildren().add(channelRow);
            }
        }

        // Auto-join and select first channel
        if (!channels.isEmpty()) {
            selectChannel(channels.get(0));
        }
    }

    private HBox createChannelRow(ChannelDTO channel) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(6);
        row.setPrefHeight(32);
        row.getStyleClass().add("channel-row");
        row.setUserData(channel);

        // Icon based on type
        Label iconLabel = new Label();
        switch (channel.getType().name()) {
            case "VOICE":
                iconLabel.setText("🔊");
                iconLabel.getStyleClass().add("channel-voice-icon");
                break;
            case "ANNOUNCEMENT":
                iconLabel.setText("📢");
                iconLabel.getStyleClass().add("channel-hash");
                break;
            default:
                iconLabel.setText("#");
                iconLabel.getStyleClass().add("channel-hash");
        }

        // Channel name
        Label nameLabel = new Label(channel.getName());
        nameLabel.getStyleClass().add("channel-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        row.getChildren().addAll(iconLabel, nameLabel);
        row.setPadding(new Insets(5, 8, 5, 8));

        row.setOnMouseClicked(e -> selectChannel(channel));

        return row;
    }

    private void selectChannel(ChannelDTO channel) {
        this.currentChannel = channel;
        activeChannelLabel.setText(channel.getName());

        // Update active state on channel rows
        for (Map.Entry<Integer, HBox> entry : channelRows.entrySet()) {
            boolean isActive = entry.getKey() == channel.getId();
            HBox row = entry.getValue();
            row.getStyleClass().removeAll("channel-row-active", "channel-row");
            row.getStyleClass().add(isActive ? "channel-row-active" : "channel-row");

            // Update name label style
            Label nameLabel = (Label) row.getChildren().get(1);
            nameLabel.getStyleClass().removeAll("channel-name-active", "channel-name");
            nameLabel.getStyleClass().add(isActive ? "channel-name-active" : "channel-name");
        }

        // Load message history
        loadMessageHistory(channel.getId());

        // Update message input prompt
        messageInput.setPromptText("Message #" + channel.getName());
    }

    private void loadMessageHistory(int channelId) {
        messageFeed.getChildren().clear();

        // Check cache first
        List<MessageDTO> cachedMessages = messageCache.getMessages(channelId);
        if (!cachedMessages.isEmpty()) {
            displayMessages(cachedMessages);
        }

        // Fetch from server
        channelModel.getMessageHistory(channelId, 50, 0, result -> {
            Platform.runLater(() -> {
                if (result.isSuccess() && result.getMessages() != null) {
                    displayMessages(result.getMessages());
                }
            });
        });
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

        // Scroll to bottom
        messageFeedScroll.setVvalue(1.0);
    }

    private void addDateDivider(String date) {
        HBox divider = new HBox();
        divider.setAlignment(Pos.CENTER);
        divider.setPrefHeight(28);
        divider.setMaxWidth(Double.MAX_VALUE);

        Label dateLabel = new Label(date);
        dateLabel.getStyleClass().add("date-divider-label");

        Region leftLine = new Region();
        leftLine.getStyleClass().add("date-divider-line");
        leftLine.setPrefWidth(100);
        HBox.setHgrow(leftLine, Priority.ALWAYS);

        Region rightLine = new Region();
        rightLine.getStyleClass().add("date-divider-line");
        rightLine.setPrefWidth(100);
        HBox.setHgrow(rightLine, Priority.ALWAYS);

        divider.getChildren().addAll(leftLine, dateLabel, rightLine);
        messageFeed.getChildren().add(divider);
    }

    private void addMessageToFeed(MessageDTO message) {
        HBox messageGroup = new HBox();
        messageGroup.setSpacing(12);
        messageGroup.setAlignment(Pos.TOP_LEFT);
        messageGroup.getStyleClass().add("msg-group");
        messageGroup.setMaxWidth(Double.MAX_VALUE);

        // Avatar
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(36, 36);
        avatarContainer.getStyleClass().add("msg-avatar");

        Color avatarColor = getUserAvatarColor(message.getSenderUsername());
        Circle avatarCircle = new Circle(18);
        avatarCircle.setFill(avatarColor);

        String initial = message.getSenderUsername().substring(0, 1).toUpperCase();
        Label initialLabel = new Label(initial);
        initialLabel.setTextFill(Color.WHITE);
        initialLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        avatarContainer.getChildren().addAll(avatarCircle, initialLabel);

        // Message content
        VBox contentBox = new VBox();
        contentBox.setSpacing(4);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        HBox header = new HBox();
        header.setSpacing(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label(message.getSenderDisplayName());
        authorLabel.getStyleClass().add("msg-author");

        Label timeLabel = new Label(formatTime(message.getSentAt()));
        timeLabel.getStyleClass().add("msg-time");

        header.getChildren().addAll(authorLabel, timeLabel);

        Label messageText = new Label(message.getContent());
        messageText.getStyleClass().add("msg-text");
        messageText.setWrapText(true);

        contentBox.getChildren().addAll(header, messageText);

        messageGroup.getChildren().addAll(avatarContainer, contentBox);
        messageFeed.getChildren().add(messageGroup);
    }

    private Color getUserAvatarColor(String username) {
        if (!userAvatarColors.containsKey(username)) {
            int index = Math.abs(username.hashCode()) % avatarColors.length;
            userAvatarColors.put(username, avatarColors[index]);
        }
        return userAvatarColors.get(username);
    }

    private void setupMessageListener() {
        ServerConnection.getInstance().addPacketListener(packet -> {
            if (packet.getResponseType() != null) {
                switch (packet.getResponseType()) {
                    case MESSAGE_BROADCAST:
                        handleNewMessage(packet);
                        break;
                    case USER_PRESENCE_BROADCAST:
                        handleUserPresence(packet);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void handleNewMessage(Packet packet) {
        MessageDTO message = (MessageDTO) packet.get("message");
        if (message != null && currentChannel != null && message.getChannelId() == currentChannel.getId()) {
            Platform.runLater(() -> {
                addMessageToFeed(message);
                messageFeedScroll.setVvalue(1.0);
            });
        }
    }

    private void handleUserPresence(Packet packet) {
        int userId = (int) packet.get("userId");
        String status = packet.get("status").toString();
        // Update member list status
        Platform.runLater(this::loadMembers);
    }

    private void loadMembers() {
        if (currentWorkspace != null) {
            memberCountLabel.setText(String.valueOf(currentWorkspace.getMemberCount()));
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                // Reset to all channels
                if (currentWorkspace != null) {
                    loadChannels(currentWorkspace.getId());
                }
            } else {
                // Filter channels by name
                for (HBox row : channelRows.values()) {
                    ChannelDTO channel = (ChannelDTO) row.getUserData();
                    boolean matches = channel.getName().toLowerCase().contains(newVal.toLowerCase());
                    row.setVisible(matches);
                    row.setManaged(matches);
                }
            }
        });
    }

    private void toggleSearch() {
        searchField.setVisible(!searchField.isVisible());
        searchField.setManaged(!searchField.isVisible());
        if (searchField.isVisible()) {
            searchField.requestFocus();
        } else {
            searchField.clear();
        }
    }

    private void openSettings() {
        System.out.println("Settings clicked. Logging out for now as a temporary feature.");
        userModel.logout();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/voxlink/client/src/resources/fxml/Login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) settings.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/voxlink/client/src/resources/css/login.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("VoxLink - Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleNotifications() {
        // Toggle notifications
        System.out.println("Toggle notifications");
    }

    private String formatDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        return dateTime.format(formatter);
    }

    private String formatTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
        return dateTime.format(formatter);
    }

    // ========== FXML Action Methods ==========

    @FXML
    void onAddServer(MouseEvent event) {
        System.out.println("TODO: Add server clicked - Need Create Server FXML");
    }

    @FXML
    void onAddTextChannel(ActionEvent event) {
        // Show create channel dialog
        System.out.println("Add text channel clicked");
    }

    @FXML
    void onAddVoiceChannel(ActionEvent event) {
        // Show create voice channel dialog
        System.out.println("Add voice channel clicked");
    }

    @FXML
    void onAttachFile(ActionEvent event) {
        // Open file chooser for file upload
        System.out.println("Attach file clicked");
    }

    @FXML
    void onChannelSelected(MouseEvent event) {
        HBox row = (HBox) event.getSource();
        ChannelDTO channel = (ChannelDTO) row.getUserData();
        if (channel != null) {
            selectChannel(channel);
        }
    }

    @FXML
    void onEmoji(ActionEvent event) {
        // Show emoji picker
        System.out.println("Emoji button clicked");
    }

    @FXML
    void onSendMessage(ActionEvent event) {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || currentChannel == null) {
            return;
        }

        messageInput.clear();

        messageModel.sendMessage(content, currentChannel.getId(), null, result -> {
            Platform.runLater(() -> {
                if (!result.isSuccess()) {
                    // Show error
                    System.out.println("Failed to send message: " + result.getErrorMessage());
                }
            });
        });
    }

    @FXML
    void onVoiceChannelSelected(MouseEvent event) {
        HBox row = (HBox) event.getSource();
        ChannelDTO channel = (ChannelDTO) row.getUserData();
        if (channel != null) {
            selectChannel(channel);
            // Additional voice channel logic could go here
        }
    }
}