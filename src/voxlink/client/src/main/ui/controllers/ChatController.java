package voxlink.client.src.main.ui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import voxlink.client.src.main.network.ServerConnection;
import voxlink.client.src.main.state.AppState;
import voxlink.client.src.main.state.MessageCache;
import voxlink.shared.dto.MessageDTO;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;

public class ChatController {

    private final VBox root;
    private final ListView<String> messageListView;
    private final TextField messageInput;
    private final Button sendButton;
    private final ObservableList<String> displayMessages;

    private final AppState appState;
    private final MessageCache messageCache;

    public ChatController() {
        this.appState = AppState.getInstance();
        this.messageCache = MessageCache.getInstance();
        this.displayMessages = FXCollections.observableArrayList();

        this.root = new VBox();
        this.root.setStyle("-fx-background-color: #1e1e2e;");

        this.messageListView = new ListView<>(displayMessages);
        this.messageListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-text-fill: white;");
        VBox.setVgrow(messageListView, Priority.ALWAYS);

        HBox inputBox = new HBox(10);
        inputBox.setStyle("-fx-padding: 10; -fx-background-color: #313244;");
        inputBox.setAlignment(Pos.CENTER);

        this.messageInput = new TextField();
        this.messageInput.setPromptText("Type a message...");
        this.messageInput.setStyle("-fx-background-color: #45475a; -fx-text-fill: white; -fx-prompt-text-fill: #a6adc8; -fx-padding: 10; -fx-background-radius: 20;");
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        this.sendButton = new Button("Send");
        this.sendButton.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: #11111b; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");

        inputBox.getChildren().addAll(messageInput, sendButton);
        root.getChildren().addAll(messageListView, inputBox);

        setupListeners();
    }

    private void setupListeners() {
        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());

        appState.addListener(new AppState.StateAdapter() {
            @Override
            public void onCurrentChannelChanged(voxlink.shared.dto.ChannelDTO channel) {
                refreshMessages();
            }
        });
    }

    private void refreshMessages() {
        displayMessages.clear();
        if (appState.getCurrentChannel() != null) {
            for (MessageDTO msg : messageCache.getMessages(appState.getCurrentChannel().getId())) {
                displayMessages.add(msg.getSenderUsername() + ": " + msg.getContent());
            }
        }
    }

    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || appState.getCurrentChannel() == null) {
            return;
        }

        Packet packet = new Packet(RequestType.MESSAGE_SEND);
        packet.put("channelId", appState.getCurrentChannel().getId());
        packet.put("content", content);

        ServerConnection.getInstance().sendPacket(packet);
        messageInput.clear();
    }

    public VBox getView() {
        return root;
    }
}
