package voxlink.shared.dto;

import java.io.Serializable;

/**
 * Represents the type of invitation link.
 */
public enum InviteType implements Serializable {

    PERMANENT("permanent", "♾️", "Never expires", -1, false),
    ONE_TIME("one_time", "1️⃣", "One-time use", 1, true),
    LIMITED("limited", "🔢", "Limited uses", 10, true),
    EXPIRING("expiring", "⏰", "Expires after time", -1, true),
    ADMIN_ONLY("admin_only", "👑", "Admin only", -1, false);

    private final String code;
    private final String icon;
    private final String description;
    private final int defaultMaxUses;
    private final boolean autoDeactivate;

    InviteType(String code, String icon, String description, int defaultMaxUses, boolean autoDeactivate) {
        this.code = code;
        this.icon = icon;
        this.description = description;
        this.defaultMaxUses = defaultMaxUses;
        this.autoDeactivate = autoDeactivate;
    }

    public String getCode() {
        return code;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultMaxUses() {
        return defaultMaxUses;
    }

    public boolean isAutoDeactivate() {
        return autoDeactivate;
    }

    public static InviteType fromCode(String code) {
        for (InviteType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return ONE_TIME;
    }

    // Check if invite link is temporary or not
    public boolean isTemporary() {
        return this != PERMANENT;
    }

    @Override
    public String toString() {
        return description;
    }
}