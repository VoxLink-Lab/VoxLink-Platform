package voxlink.shared.dto;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a role that can be assigned to a user within a workspace.
 */
public class RoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Core role information
    private int id;
    private String name;
    private String description;
    private int workspaceId;

    // Permission flags
    private Set<String> permissions;

    // Hierarchy
    private int priority;

    // Metadata
    private boolean isDefault;
    private boolean isSystemRole;

    // Default constructor
    public RoleDTO() {
        this.permissions = new HashSet<>();
        this.isDefault = false;
        this.isSystemRole = false;
        this.priority = 10;
    }

    // Constructor for basic role creation
    public RoleDTO(int id, String name, int workspaceId) {
        this.id = id;
        this.name = name;
        this.workspaceId = workspaceId;
    }

    // Constructor for system roles
    public RoleDTO(int id, String name, int workspaceId, int priority, boolean isSystemRole) {
        this(id, name, workspaceId);
        this.priority = priority;
        this.isSystemRole = isSystemRole;
        this.setDefaultPermissions(name);
    }

    // Default permission based on role name
    private void setDefaultPermissions(String roleName) {
        switch (roleName.toUpperCase()) {
            case "ADMIN":
                permissions.addAll(Permissions.getAllPermissions());
                this.priority = 100;
                break;
            case "MODERATOR":
                permissions.addAll(Permissions.getModeratorPermissions());
                this.priority = 50;
                break;
            default:
                permissions.addAll(Permissions.getMemberPermissions());
                this.priority = 10;
        }
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isSystemRole() {
        return isSystemRole;
    }

    public void setSystemRole(boolean systemRole) {
        isSystemRole = systemRole;
    }


    // Check if this role has specific permissions
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    // Add permission to this role
    public void addPermission(String permission) {
        this.permissions.add(permission);
    }

    // Remove permission from this role
    public void removePermission(String permission) {
        this.permissions.remove(permission);
    }

    // Check if this role can perform an action based on priority
    public boolean canPerform(int requiredPriority) {
        return this.priority >= requiredPriority;
    }

   // Check if this role is higher than another role
    public boolean isHigherThan(RoleDTO other) {
        return this.priority > other.priority;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RoleDTO roleDTO = (RoleDTO) obj;
        return id == roleDTO.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "RoleDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", workspaceId=" + workspaceId +
                ", priority=" + priority +
                ", permissions=" + permissions.size() +
                '}';
    }

    /**
     * Inner class defining all available permission keys in VoxLink
     */
    public static class Permissions {

        // Channel permissions
        public static final String CREATE_CHANNEL = "channel.create";
        public static final String DELETE_CHANNEL = "channel.delete";
        public static final String EDIT_CHANNEL = "channel.edit";
        public static final String JOIN_CHANNEL = "channel.join";

        // Message permissions
        public static final String SEND_MESSAGE = "message.send";
        public static final String EDIT_MESSAGE = "message.edit";
        public static final String DELETE_MESSAGE = "message.delete";
        public static final String PIN_MESSAGE = "message.pin";

        // Member management
        public static final String KICK_MEMBER = "member.kick";
        public static final String BAN_MEMBER = "member.ban";
        public static final String ASSIGN_ROLE = "role.assign";
        public static final String REVOKE_ROLE = "role.revoke";

        // Workspace management
        public static final String EDIT_WORKSPACE = "workspace.edit";
        public static final String DELETE_WORKSPACE = "workspace.delete";
        public static final String CREATE_INVITE = "invite.create";

        // File permissions
        public static final String UPLOAD_FILE = "file.upload";
        public static final String DOWNLOAD_FILE = "file.download";
        public static final String DELETE_FILE = "file.delete";

        // Admin permissions
        public static final String VIEW_AUDIT_LOG = "audit.view";
        public static final String EXPORT_AUDIT_LOG = "audit.export";

        // Get all available permissions
        public static Set<String> getAllPermissions() {
            Set<String> all = new HashSet<>();
            all.add(CREATE_CHANNEL);
            all.add(DELETE_CHANNEL);
            all.add(EDIT_CHANNEL);
            all.add(JOIN_CHANNEL);
            all.add(SEND_MESSAGE);
            all.add(EDIT_MESSAGE);
            all.add(DELETE_MESSAGE);
            all.add(PIN_MESSAGE);
            all.add(KICK_MEMBER);
            all.add(BAN_MEMBER);
            all.add(ASSIGN_ROLE);
            all.add(REVOKE_ROLE);
            all.add(EDIT_WORKSPACE);
            all.add(DELETE_WORKSPACE);
            all.add(CREATE_INVITE);
            all.add(UPLOAD_FILE);
            all.add(DOWNLOAD_FILE);
            all.add(DELETE_FILE);
            all.add(VIEW_AUDIT_LOG);
            all.add(EXPORT_AUDIT_LOG);
            return all;
        }

        // Get moderator permissions
        public static Set<String> getModeratorPermissions() {
            Set<String> mod = new HashSet<>();
            mod.add(JOIN_CHANNEL);
            mod.add(SEND_MESSAGE);
            mod.add(EDIT_MESSAGE);
            mod.add(DELETE_MESSAGE);
            mod.add(PIN_MESSAGE);
            mod.add(KICK_MEMBER);
            mod.add(CREATE_INVITE);
            mod.add(UPLOAD_FILE);
            mod.add(DOWNLOAD_FILE);
            return mod;
        }

        // Standard member permission
        public static Set<String> getMemberPermissions() {
            Set<String> member = new HashSet<>();
            member.add(JOIN_CHANNEL);
            member.add(SEND_MESSAGE);
            member.add(EDIT_MESSAGE);
            member.add(DELETE_MESSAGE);
            member.add(DOWNLOAD_FILE);
            member.add(UPLOAD_FILE);
            return member;
        }
    }
}