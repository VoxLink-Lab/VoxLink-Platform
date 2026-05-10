package voxlink.server.src.main.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {
    public static void initialize() throws SQLException {
        Connection connection = DBConnection.connect();

        // Check connection
        if(connection == null) {
            System.out.println("Error while connecting!!");
            return;
        }

        Statement stmt = connection.createStatement();

        String usersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,                
                    profile_picture VARCHAR(255),
                    status ENUM('ONLINE', 'OFFLINE', 'AWAY') DEFAULT 'OFFLINE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String workspacesTable = """
                CREATE TABLE IF NOT EXISTS workspaces (
                    workspace_id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL,
                    description TEXT,
                    owner_id INT NOT NULL,
                    invite_code VARCHAR(20) UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        
                    CONSTRAINT fk_workspace_owner
                    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
                );
                """;

        String channelsTable = """
                CREATE TABLE IF NOT EXISTS channels (
                    channel_id INT PRIMARY KEY AUTO_INCREMENT,
                    workspace_id INT NOT NULL,
                    name VARCHAR(50) NOT NULL,
                    type ENUM('TEXT', 'VOICE') DEFAULT 'TEXT',
                    is_private BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    channel_profile_picture VARCHAR(255),
    
                    CONSTRAINT fk_channel_workspace
                    FOREIGN KEY (workspace_id) REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
    
                    CONSTRAINT unique_channel_per_workspace UNIQUE(workspace_id, name)
                );""";

        String rolesTable = """
                CREATE TABLE IF NOT EXISTS roles (
                    role_id INT PRIMARY KEY AUTO_INCREMENT,
                    workspace_id INT NOT NULL,
                    role_name VARCHAR(50) NOT NULL,
                    can_manage_channels BOOLEAN DEFAULT FALSE, 
                    can_kick_users BOOLEAN DEFAULT FALSE,
                    can_delete_messages BOOLEAN DEFAULT FALSE,
    
                    CONSTRAINT fk_role_workspace
                    FOREIGN KEY (workspace_id) REFERENCES workspaces(workspace_id) ON DELETE CASCADE
                );""";

        String workspaceMembersTable = """
                CREATE TABLE IF NOT EXISTS workspace_members (
                    member_id INT PRIMARY KEY AUTO_INCREMENT,
                    workspace_id INT NOT NULL,
                    user_id INT NOT NULL,
                    role_id INT,
                    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
                    CONSTRAINT fk_member_workspace
                    FOREIGN KEY (workspace_id) REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
    
                    CONSTRAINT fk_member_user
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
                    CONSTRAINT fk_member_role 
                    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE SET NULL,
    
                    CONSTRAINT unique_workspace_user
                    UNIQUE(workspace_id, user_id)
                );""";

        String messagesTable = """
                CREATE TABLE IF NOT EXISTS messages (
                    message_id INT PRIMARY KEY AUTO_INCREMENT,
                    channel_id INT,
                    sender_id INT NOT NULL,
                    receiver_id INT,
                    content TEXT NOT NULL,
                    edited BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
                    CONSTRAINT fk_message_channel
                    FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
    
                    CONSTRAINT fk_message_sender
                    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
                    
                    CONSTRAINT fk_message_reciever
                    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE
                );""";

        String attachmentsTable = """
                CREATE TABLE IF NOT EXISTS attachments (
                    attachment_id INT PRIMARY KEY AUTO_INCREMENT,
                    message_id INT NOT NULL,
                    file_name VARCHAR(255) NOT NULL,
                    file_path VARCHAR(255) NOT NULL,
                    file_size BIGINT,
                    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
                    CONSTRAINT fk_attachment_message
                    FOREIGN KEY (message_id) REFERENCES messages(message_id) ON DELETE CASCADE
                );""";

        // Create database tables
        stmt.executeUpdate(usersTable);
        System.out.println("Users table created successfully!!");

        stmt.executeUpdate(workspacesTable);
        System.out.println("Workspaces table created successfully!!");

        stmt.executeUpdate(channelsTable);
        System.out.println("Channels table created successfully!!");
        stmt.executeUpdate(rolesTable);

        stmt.executeUpdate(workspaceMembersTable);
        System.out.println("Workspace_Members table created successfully!!");

        stmt.executeUpdate(messagesTable);
        System.out.println("Messages table created successfully!!");

        stmt.executeUpdate(attachmentsTable);
        System.out.println("Attachments table created successfully!!");

    }
}
