package voxlink.shared.dto;

import java.io.Serializable;

/**
 * Represents the type of message displayed in a channel.
 */
public enum MessageType implements Serializable {

    TEXT("text", "💬"),
    FILE("file", "📎"),
    SYSTEM("system", "🤖"),
    REPLY("reply", "↩️"),
    EDITED("edited", "✏️"),
    DIRECT_MESSAGE("dm", "🔒");

    private final String code;
    private final String icon;

    MessageType(String code, String icon) {
        this.code = code;
        this.icon = icon;
    }

    public String getCode() {
        return code;
    }

    public String getIcon() {
        return icon;
    }

    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return TEXT;
    }

    public boolean isUserGenerated() {
        return this == TEXT || this == FILE || this == REPLY;
    }

    @Override
    public String toString() {
        return code;
    }
}