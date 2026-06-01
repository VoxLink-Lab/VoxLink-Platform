package voxlink.server.src.main.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
    public static void initialize() throws SQLException {
        Connection connection = DBConnection.getConnection();

        // Check connection
        if(connection == null) {
            System.out.println("Error while connecting!!");
            return;
        }

        Statement stmt = connection.createStatement();

        String usersTable = """
              CREATE TABLE IF NOT EXISTS users (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  username VARCHAR(50) UNIQUE NOT NULL,
                  password_hash VARCHAR(255) NOT NULL,
                  email VARCHAR(100) UNIQUE NOT NULL,
                  display_name VARCHAR(100),
                  avatar_url VARCHAR(500),
                  status VARCHAR(20) DEFAULT 'OFFLINE',
                  custom_status VARCHAR(100),
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  is_active BOOLEAN DEFAULT TRUE,
                  INDEX idx_username (username),
                  INDEX idx_status (status),
                  INDEX idx_last_active (last_active_at)
              );
              """;
              
        try {
            stmt.executeUpdate("ALTER TABLE users ADD COLUMN display_name VARCHAR(100)");
            System.out.println("Migrated: Added display_name to users table.");
        } catch (SQLException e) {
            // Ignore if column already exists
        }

        String workspacesTable = """
              CREATE TABLE IF NOT EXISTS workspaces (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  name VARCHAR(100) NOT NULL,
                  description TEXT,
                  icon_url VARCHAR(500),
                  banner_url VARCHAR(500),
                  owner_id INT NOT NULL,
                  is_public BOOLEAN DEFAULT FALSE,
                  default_channel_name VARCHAR(100) DEFAULT 'general',
                  invite_code VARCHAR(100) UNIQUE,
                  invite_uses_so_far INT,
                  invite_expires_at TIMESTAMP NULL,
                  max_invite_uses INT DEFAULT -1,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
                  INDEX idx_owner (owner_id),
                  INDEX idx_is_public (is_public),
                  INDEX idx_invite_code (invite_code)
              );
              """;

        String workspaceMembersTable = """
              CREATE TABLE IF NOT EXISTS workspace_members (
                  workspace_id INT NOT NULL,
                  user_id INT NOT NULL,
                  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (workspace_id, user_id),
                  FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
                  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                  INDEX idx_user (user_id),
                  INDEX idx_workspace (workspace_id)
              );
              """;

        String channelsTable = """
              CREATE TABLE IF NOT EXISTS channels (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  name VARCHAR(100) NOT NULL,
                  description TEXT,
                  workspace_id INT NOT NULL,
                  type VARCHAR(20) DEFAULT 'TEXT',
                  is_private BOOLEAN DEFAULT FALSE,
                  is_archived BOOLEAN DEFAULT FALSE,
                  created_by INT,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
                  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
                  INDEX idx_workspace (workspace_id),
                  INDEX idx_type (type),
                  UNIQUE KEY uk_workspace_channel (workspace_id, name)
              );
              """;

        String channelMembersTable = """
                CREATE TABLE IF NOT EXISTS channel_members (
                    channel_id INT NOT NULL,
                    user_id INT NOT NULL,
                    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (channel_id, user_id),
                    FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_user (user_id),
                    INDEX idx_channel (channel_id)
                );
                """;

        String rolesTable = """
              CREATE TABLE IF NOT EXISTS roles (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  name VARCHAR(50) NOT NULL,
                  description TEXT,
                  workspace_id INT NOT NULL,
                  priority INT DEFAULT 10,
                  is_default BOOLEAN DEFAULT FALSE,
                  is_system_role BOOLEAN DEFAULT FALSE,
                  permissions TEXT,
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
                  UNIQUE KEY uk_workspace_role (workspace_id, name),
                  INDEX idx_workspace (workspace_id),
                  INDEX idx_priority (priority)
              );
              """;

        String userRolesTable = """
                CREATE TABLE IF NOT EXISTS user_roles (
                    user_id INT NOT NULL,
                    role_id INT NOT NULL,
                    workspace_id INT NOT NULL,
                    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    assigned_by INT,
                    PRIMARY KEY (user_id, role_id, workspace_id),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
                    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
                    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_user (user_id),
                    INDEX idx_role (role_id),
                    INDEX idx_workspace (workspace_id)
                );
                """;

        String messagesTable = """
              CREATE TABLE IF NOT EXISTS messages (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  content TEXT,
                  channel_id INT NOT NULL,
                  sender_id INT,
                  type VARCHAR(20) DEFAULT 'TEXT',
                  reply_to_message_id INT NULL,
                  sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  edited_at TIMESTAMP NULL,
                  deleted_at TIMESTAMP NULL,
                  is_deleted BOOLEAN DEFAULT FALSE,
                  FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE,
                  FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL,
                  FOREIGN KEY (reply_to_message_id) REFERENCES messages(id) ON DELETE SET NULL,
                  INDEX idx_channel (channel_id),
                  INDEX idx_sender (sender_id),
                  INDEX idx_sent_at (sent_at),
                  INDEX idx_channel_sent_at (channel_id, sent_at)
              );
              """;

        String fileAttachmentsTable = """
              CREATE TABLE IF NOT EXISTS file_attachments (
                  id INT PRIMARY KEY AUTO_INCREMENT,
                  file_name VARCHAR(255) NOT NULL,
                  file_path VARCHAR(500) NOT NULL,
                  file_size BIGINT NOT NULL,
                  file_type VARCHAR(100),
                  file_hash VARCHAR(64),
                  message_id INT NULL,
                  channel_id INT NOT NULL,
                  workspace_id INT NOT NULL,
                  uploaded_by INT NOT NULL,
                  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  download_count INT DEFAULT 0,
                  thumbnail_path VARCHAR(500),
                  image_width INT,
                  image_height INT,
                  status VARCHAR(20) DEFAULT 'AVAILABLE',
                  FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL,
                  FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE,
                  INDEX idx_message (message_id),
                  INDEX idx_channel (channel_id),
                  INDEX idx_workspace (workspace_id),
                  INDEX idx_uploaded_by (uploaded_by)
              );
              """;

        String invitesTable = """
                CREATE TABLE IF NOT EXISTS invites (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    invite_code VARCHAR(100) UNIQUE NOT NULL,
                    workspace_id INT NOT NULL,
                    created_by INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NULL,
                    max_uses INT DEFAULT 1,
                    uses_so_far INT DEFAULT 0,
                    is_active BOOLEAN DEFAULT TRUE,
                    invited_user_id INT NULL,
                    target_channel_name VARCHAR(100),
                    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
                    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (invited_user_id) REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_invite_code (invite_code),
                    INDEX idx_workspace (workspace_id),
                    INDEX idx_active (is_active)
                );
                """;

        String auditLogsTable = """
                CREATE TABLE IF NOT EXISTS audit_log (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    action_type VARCHAR(50) NOT NULL,
                    description TEXT,
                    actor_id INT NOT NULL,
                    target_user_id INT NULL,
                    target_channel_id INT NULL,
                    target_workspace_id INT NULL,
                    workspace_id INT NOT NULL,
                    ip_address VARCHAR(45),
                    additional_data TEXT,
                    was_successful BOOLEAN DEFAULT TRUE,
                    failure_reason TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_action_type (action_type),
                    INDEX idx_actor (actor_id),
                    INDEX idx_workspace (workspace_id),
                    INDEX idx_timestamp (timestamp)
               );
               """;

        String messageReadReceiptsTable = """
                CREATE TABLE IF NOT EXISTS message_read_receipts (
                    message_id INT NOT NULL,
                    user_id INT NOT NULL,
                    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (message_id, user_id),
                    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_user (user_id),
                    INDEX idx_read_at (read_at)
                );
                """;

        // Execute in correct order (respecting foreign key dependencies)
        stmt.executeUpdate(usersTable);
        System.out.println("users table created successfully!!");

        stmt.executeUpdate(workspacesTable);
        System.out.println("workspaces table created successfully!!");

        stmt.executeUpdate(workspaceMembersTable);
        System.out.println("workspace_Members table created successfully!!");

        stmt.executeUpdate(rolesTable);
        System.out.println("Roles table created successfully!!");

        stmt.executeUpdate(channelsTable);
        System.out.println("channels table created successfully!!");

        stmt.executeUpdate(channelMembersTable);
        System.out.println("channel_members table created successfully!!");

        stmt.executeUpdate(userRolesTable);
        System.out.println("user_roles table created successfully!!");

        stmt.executeUpdate(messagesTable);
        System.out.println("messages table created successfully!!");

        stmt.executeUpdate(fileAttachmentsTable);
        System.out.println("file_attachments table created successfully!!");

        stmt.executeUpdate(invitesTable);
        System.out.println("invites table created successfully!!");

        stmt.executeUpdate(auditLogsTable);
        System.out.println("audit_log table created successfully!!");

        stmt.executeUpdate(messageReadReceiptsTable);
        System.out.println("message_read_receipts table created successfully!!");

        System.out.println("All database tables initialized successfully!");
    }
}