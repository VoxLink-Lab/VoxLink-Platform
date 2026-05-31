package voxlink.client.src.main.model;

import javafx.beans.property.*;
import voxlink.shared.dto.WorkspaceDTO;

public class ClientWorkspace {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty iconUrl = new SimpleStringProperty();
    private final IntegerProperty memberCount = new SimpleIntegerProperty();

    public ClientWorkspace(WorkspaceDTO dto) {
        this.id.set(dto.getId());
        this.name.set(dto.getName());
        this.description.set(dto.getDescription());
        this.iconUrl.set(dto.getIconUrl());
        this.memberCount.set(dto.getMemberCount());
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }

    public String getIconUrl() { return iconUrl.get(); }
    public StringProperty iconUrlProperty() { return iconUrl; }

    public int getMemberCount() { return memberCount.get(); }
    public IntegerProperty memberCountProperty() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount.set(memberCount); }
}
