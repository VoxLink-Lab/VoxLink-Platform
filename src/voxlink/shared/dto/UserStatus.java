package voxlink.shared.dto;

import java.io.Serializable;

/**
 * Represents the current presence state of a user.
 */
public enum UserStatus implements Serializable {

    ONLINE("online", "🟢", "Online"),
    IDLE("idle", "🟡", "Idle"),
    DO_NOT_DISTURB("dnd", "🔴", "Do Not Disturb"),
    OFFLINE("offline", "⚫", "Offline"),
    AWAY("away", "🌙", "Away"),
    CONNECTING("connecting", "🔄", "Connecting");

    private final String code;
    private final String icon;
    private final String displayName;

    UserStatus(String code, String icon, String displayName) {
        this.code = code;
        this.icon = icon;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Get UserStatus from string code
    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return OFFLINE;
    }

    public boolean isActive() {
        return this == ONLINE || this == IDLE;
    }

    public boolean isAvailable() {
        return this == ONLINE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}