package voxlink.client.src.main.ui.controllers;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AuthController {

    private final VBox root;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private final Label statusLabel;

    public AuthController() {
        root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 40; -fx-background-color: #1e1e2e;");

        Label titleLabel = new Label("VoxLink Login");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(300);
        usernameField.setStyle("-fx-background-color: #313244; -fx-text-fill: white; -fx-prompt-text-fill: #a6adc8; -fx-padding: 10; -fx-background-radius: 5;");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);
        passwordField.setStyle("-fx-background-color: #313244; -fx-text-fill: white; -fx-prompt-text-fill: #a6adc8; -fx-padding: 10; -fx-background-radius: 5;");

        loginButton = new Button("Login");
        loginButton.setMaxWidth(300);
        loginButton.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: #11111b; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5; -fx-cursor: hand;");

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #f38ba8;");

        root.getChildren().addAll(titleLabel, usernameField, passwordField, loginButton, statusLabel);
    }

    public VBox getView() {
        return root;
    }
}
