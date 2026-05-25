package voxlink.shared.dto;

import java.io.Serializable;

/**
 * Represents the delivery and read state of a message.
 */
public enum MessageStatus implements Serializable {

    SENDING("sending", "⏳", "Sending..."),
    SENT("sent", "✓", "Sent"),
    DELIVERED("delivered", "✓✓", "Delivered"),
    READ("read", "✓✓", "Read"),
    EDITED("edited", "✏️", "Edited"),
    DELETED("deleted", "🗑️", "Deleted"),
    FAILED("failed", "❌", "Failed to send");

    private final String code;
    private final String icon;
    private final String displayText;

    MessageStatus(String code, String icon, String displayText) {
        this.code = code;
        this.icon = icon;
        this.displayText = displayText;
    }

    public String getCode() {
        return code;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayText() {
        return displayText;
    }

    public static MessageStatus fromCode(String code) {
        for (MessageStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return SENT;
    }

    // Check if message is in a terminal state
    public boolean isTerminal() {
        return this == DELIVERED || this == READ || this == EDITED || this == DELETED || this == FAILED;
    }

   // Check if message is successfully delivered
    public boolean isSuccessfullyDelivered() {
        return this == DELIVERED || this == READ;
    }

    @Override
    public String toString() {
        return displayText;
    }
}