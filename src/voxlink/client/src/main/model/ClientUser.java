package voxlink.client.src.main.model;

import javafx.beans.property.*;
import voxlink.shared.dto.UserDTO;
import voxlink.shared.dto.UserStatus;

public class ClientUser {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty displayName = new SimpleStringProperty();
    private final ObjectProperty<UserStatus> status = new SimpleObjectProperty<>();

    public ClientUser(UserDTO dto) {
        this.id.set(dto.getId());
        this.username.set(dto.getUsername());
        this.displayName.set(dto.getDisplayName());
        this.status.set(dto.getStatus());
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getUsername() { return username.get(); }
    public StringProperty usernameProperty() { return username; }

    public String getDisplayName() { return displayName.get(); }
    public StringProperty displayNameProperty() { return displayName; }

    public UserStatus getStatus() { return status.get(); }
    public ObjectProperty<UserStatus> statusProperty() { return status; }
    public void setStatus(UserStatus status) { this.status.set(status); }
}
