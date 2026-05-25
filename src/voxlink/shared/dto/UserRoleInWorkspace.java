package voxlink.shared.dto;

import java.io.Serializable;

/**
 * Represents the role a user has within a specific workspace.
 */
public enum UserRoleInWorkspace implements Serializable {

    ADMIN("admin", "👑", "Administrator", 100),
    MODERATOR("mod", "🛡️", "Moderator", 50),
    MEMBER("member", "👤", "Member", 10);

    private final String code;
    private final String icon;
    private final String displayName;
    private final int permissionLevel;

    UserRoleInWorkspace(String code, String icon, String displayName, int permissionLevel) {
        this.code = code;
        this.icon = icon;
        this.displayName = displayName;
        this.permissionLevel = permissionLevel;
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

    public int getPermissionLevel() {
        return permissionLevel;
    }

    // Check if a user can perform a required action
    public boolean can(int requiredLevel) {
        return this.permissionLevel >= requiredLevel;
    }

    // Check if a user is at least a moderator
    public boolean isAtLeastModerator() {
        return this == MODERATOR || this == ADMIN;
    }

    // Check if user is an admin
    public boolean isAdmin() {
        return this == ADMIN;
    }

    public static UserRoleInWorkspace fromCode(String code) {
        for (UserRoleInWorkspace role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return MEMBER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}