package voxlink.shared.dto;

import java.io.Serializable;

/**
 * Represents the type of communication channel in VoxLink.
 */
public enum ChannelType implements Serializable {


    TEXT("text", "💬", "Text Channel", true, true),
    ANNOUNCEMENT("announcement", "📢", "Announcement Channel", false, true),
    VOICE("voice", "🎙️", "Voice Channel", false, false);

    private final String code;
    private final String icon;
    private final String displayName;
    private final boolean membersCanSend;
    private final boolean supportsFiles;

    ChannelType(String code, String icon, String displayName, boolean membersCanSend, boolean supportsFiles) {
        this.code = code;
        this.icon = icon;
        this.displayName = displayName;
        this.membersCanSend = membersCanSend;
        this.supportsFiles = supportsFiles;
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

    public boolean isMembersCanSend() {
        return membersCanSend;
    }

    public boolean isSupportsFiles() {
        return supportsFiles;
    }

    public static ChannelType fromCode(String code) {
        for (ChannelType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return TEXT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}