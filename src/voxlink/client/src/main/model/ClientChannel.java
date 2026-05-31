package voxlink.client.src.main.model;

import javafx.beans.property.*;
import voxlink.shared.dto.ChannelDTO;

public class ClientChannel {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final BooleanProperty isPrivate = new SimpleBooleanProperty();
    private final IntegerProperty unreadCount = new SimpleIntegerProperty();

    public ClientChannel(ChannelDTO dto) {
        this.id.set(dto.getId());
        this.name.set(dto.getName());
        this.description.set(dto.getDescription());
        this.isPrivate.set(dto.isPrivate());
        this.unreadCount.set(dto.getUnreadCount());
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }

    public boolean getIsPrivate() { return isPrivate.get(); }
    public BooleanProperty isPrivateProperty() { return isPrivate; }

    public int getUnreadCount() { return unreadCount.get(); }
    public IntegerProperty unreadCountProperty() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount.set(unreadCount); }
}
