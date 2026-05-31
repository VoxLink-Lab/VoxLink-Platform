package voxlink.server.src.test;

import voxlink.server.src.main.database.DBConnection;
import voxlink.server.src.main.database.SchemaInitializer;
import voxlink.server.src.main.repository.*;
import voxlink.shared.dto.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * DatabaseTest class to verify all repository operations
 * Run this class to test database connectivity and CRUD operations
 */
public class DatabaseTest {

    private static UserRepository userRepo;
    private static WorkspaceRepository workspaceRepo;
    private static ChannelRepository channelRepo;
    private static MessageRepository messageRepo;
    private static RoleRepository roleRepo;
    private static InviteRepository inviteRepo;
    private static AuditLogRepository auditLogRepo;
    private static FileAttachmentRepository fileRepo;

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("VoxLink Database Test Suite");
        System.out.println("=========================================\n");

        // Initialize repositories
        userRepo = new UserRepository();
        workspaceRepo = new WorkspaceRepository();
        channelRepo = new ChannelRepository();
        messageRepo = new MessageRepository();
        roleRepo = new RoleRepository();
        inviteRepo = new InviteRepository();
        auditLogRepo = new AuditLogRepository();
        fileRepo = new FileAttachmentRepository();

        // Run all tests
        boolean allTestsPassed = true;

        allTestsPassed &= testDatabaseConnection();
        allTestsPassed &= testSchemaInitialization();
        allTestsPassed &= testUserCRUD();
        allTestsPassed &= testWorkspaceCRUD();
        allTestsPassed &= testChannelCRUD();
        allTestsPassed &= testRoleCRUD();
        allTestsPassed &= testMessageCRUD();
        allTestsPassed &= testInviteCRUD();
        allTestsPassed &= testAuditLogCRUD();
        allTestsPassed &= testFileAttachmentCRUD();

        // Print summary
        System.out.println("\n=========================================");
        if (allTestsPassed) {
            System.out.println("✅ ALL TESTS PASSED!");
        } else {
            System.out.println("❌ SOME TESTS FAILED - Check logs above");
        }
        System.out.println("=========================================");
    }

    // Test 1: Database Connection
    private static boolean testDatabaseConnection() {
        System.out.println("📋 Test 1: Database Connection");
        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("   ✅ Connected to database successfully\n");
                return true;
            } else {
                System.out.println("   ❌ Failed to connect to database\n");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("   ❌ Database connection error: " + e.getMessage() + "\n");
            return false;
        }
    }

    // Test 2: Schema Initialization
    private static boolean testSchemaInitialization() {
        System.out.println("📋 Test 2: Schema Initialization");
        try {
            SchemaInitializer.initialize();
            System.out.println("   ✅ Schema initialized successfully\n");
            return true;
        } catch (SQLException e) {
            System.out.println("   ❌ Schema initialization failed: " + e.getMessage() + "\n");
            return false;
        }
    }

    // Test 3: User CRUD Operations
    private static boolean testUserCRUD() {
        System.out.println("📋 Test 3: User CRUD Operations");

        // Test Create User
        System.out.println("   → Creating test user...");
        UserDTO user = userRepo.createUser("testuser", "test@example.com", "hashedpassword123", "Test User");
        if (user == null) {
            System.out.println("   ❌ Failed to create user\n");
            return false;
        }
        System.out.println("   ✅ User created with ID: " + user.getId());

        // Test Find User by ID
        System.out.println("   → Finding user by ID...");
        UserDTO foundUser = userRepo.getUserById(user.getId());
        if (foundUser == null) {
            System.out.println("   ❌ Failed to find user by ID\n");
            return false;
        }
        System.out.println("   ✅ User found: " + foundUser.getUsername());

        // Test Find User by Username
        System.out.println("   → Finding user by username...");
        UserDTO foundByUsername = userRepo.getUserByUsername("testuser");
        if (foundByUsername == null) {
            System.out.println("   ❌ Failed to find user by username\n");
            return false;
        }
        System.out.println("   ✅ User found by username");

        // Test Update User Status
        System.out.println("   → Updating user status...");
        boolean statusUpdated = userRepo.updateUserStatus(user.getId(), UserStatus.ONLINE);
        if (!statusUpdated) {
            System.out.println("   ❌ Failed to update user status\n");
            return false;
        }
        System.out.println("   ✅ User status updated to ONLINE");

        // Test Authentication
        System.out.println("   → Testing authentication...");
        UserDTO authUser = userRepo.authenticate("testuser", "hashedpassword123");
        if (authUser == null) {
            System.out.println("   ❌ Authentication failed\n");
            return false;
        }
        System.out.println("   ✅ Authentication successful");

        // Test Get Online Users
        System.out.println("   → Getting online users...");
        List<UserDTO> onlineUsers = userRepo.getOnlineUsers();
        System.out.println("   ✅ Found " + onlineUsers.size() + " online users");

        System.out.println("   ✅ User CRUD tests passed!\n");
        return true;
    }

    // Test 4: Workspace CRUD Operations
    private static boolean testWorkspaceCRUD() {
        System.out.println("📋 Test 4: Workspace CRUD Operations");

        // Get test user ID
        UserDTO user = userRepo.getUserByUsername("testuser");
        if (user == null) {
            System.out.println("   ❌ Test user not found. Run User CRUD test first.\n");
            return false;
        }

        // Test Create Workspace
        System.out.println("   → Creating test workspace...");
        WorkspaceDTO workspace = workspaceRepo.createWorkspace("Test Workspace", "This is a test workspace", user.getId(), true);
        if (workspace == null) {
            System.out.println("   ❌ Failed to create workspace\n");
            return false;
        }
        System.out.println("   ✅ Workspace created with ID: " + workspace.getId());

        // Test Find Workspace by ID
        System.out.println("   → Finding workspace by ID...");
        WorkspaceDTO foundWorkspace = workspaceRepo.getWorkspaceById(workspace.getId());
        if (foundWorkspace == null) {
            System.out.println("   ❌ Failed to find workspace by ID\n");
            return false;
        }
        System.out.println("   ✅ Workspace found: " + foundWorkspace.getName());

        // Test Get Workspaces by User
        System.out.println("   → Getting workspaces for user...");
        List<WorkspaceDTO> userWorkspaces = workspaceRepo.getWorkspacesByUser(user.getId());
        System.out.println("   ✅ User is in " + userWorkspaces.size() + " workspace(s)");

        // Test Check Member
        System.out.println("   → Checking workspace membership...");
        boolean isMember = workspaceRepo.isMemberOfWorkspace(workspace.getId(), user.getId());
        System.out.println("   ✅ User is member: " + isMember);

        System.out.println("   ✅ Workspace CRUD tests passed!\n");
        return true;
    }

    // Test 5: Channel CRUD Operations
    private static boolean testChannelCRUD() {
        System.out.println("📋 Test 5: Channel CRUD Operations");

        // Get test user and workspace
        UserDTO user = userRepo.getUserByUsername("testuser");
        WorkspaceDTO workspace = workspaceRepo.getWorkspacesByUser(user.getId()).get(0);

        if (user == null || workspace == null) {
            System.out.println("   ❌ Test user or workspace not found.\n");
            return false;
        }

        // Test Create Channel
        System.out.println("   → Creating test channel...");
        ChannelDTO channel = channelRepo.createChannel("general", "General discussion channel",
                workspace.getId(), ChannelType.TEXT, false, user.getId());
        if (channel == null) {
            System.out.println("   ❌ Failed to create channel\n");
            return false;
        }
        System.out.println("   ✅ Channel created with ID: " + channel.getId());

        // Test Get Channels by Workspace
        System.out.println("   → Getting channels for workspace...");
        List<ChannelDTO> channels = channelRepo.getChannelsByWorkspace(workspace.getId());
        System.out.println("   ✅ Found " + channels.size() + " channel(s)");

        // Test Add Member to Channel
        System.out.println("   → Adding member to channel...");
        boolean added = channelRepo.addMemberToChannel(channel.getId(), user.getId());
        System.out.println("   ✅ Member added: " + added);

        // Test Check Member
        System.out.println("   → Checking channel membership...");
        boolean isMember = channelRepo.isMemberOfChannel(channel.getId(), user.getId());
        System.out.println("   ✅ Is member: " + isMember);

        System.out.println("   ✅ Channel CRUD tests passed!\n");
        return true;
    }

    // Test 6: Role CRUD Operations
    private static boolean testRoleCRUD() {
        System.out.println("📋 Test 6: Role CRUD Operations");

        // Get test user and workspace
        UserDTO user = userRepo.getUserByUsername("testuser");
        WorkspaceDTO workspace = workspaceRepo.getWorkspacesByUser(user.getId()).get(0);

        if (user == null || workspace == null) {
            System.out.println("   ❌ Test user or workspace not found.\n");
            return false;
        }

        // Test Create Default Roles
        System.out.println("   → Creating default roles...");
        roleRepo.createDefaultRoles(workspace.getId());
        System.out.println("   ✅ Default roles created");

        // Test Get Role by Name
        System.out.println("   → Getting ADMIN role...");
        RoleDTO adminRole = roleRepo.getRoleByName(workspace.getId(), "ADMIN");
        if (adminRole == null) {
            System.out.println("   ❌ Failed to get ADMIN role\n");
            return false;
        }
        System.out.println("   ✅ Found ADMIN role with priority: " + adminRole.getPriority());

        // Test Assign Role to User
        System.out.println("   → Assigning ADMIN role to user...");
        boolean assigned = roleRepo.assignRoleToUser(user.getId(), adminRole.getId(), workspace.getId(), user.getId());
        System.out.println("   ✅ Role assigned: " + assigned);

        // Test Get User Roles
        System.out.println("   → Getting user roles...");
        Set<RoleDTO> userRoles = roleRepo.getUserRoles(user.getId(), workspace.getId());
        System.out.println("   ✅ User has " + userRoles.size() + " role(s)");

        // Test Has Permission
        System.out.println("   → Checking user permissions...");
        boolean hasPermission = roleRepo.hasPermission(user.getId(), workspace.getId(), RoleDTO.Permissions.DELETE_WORKSPACE);
        System.out.println("   ✅ Has DELETE_WORKSPACE permission: " + hasPermission);

        // Test Is Admin
        System.out.println("   → Checking if user is admin...");
        boolean isAdmin = roleRepo.isAdmin(user.getId(), workspace.getId());
        System.out.println("   ✅ Is admin: " + isAdmin);

        System.out.println("   ✅ Role CRUD tests passed!\n");
        return true;
    }

    // Test 7: Message CRUD Operations
    private static boolean testMessageCRUD() {
        System.out.println("📋 Test 7: Message CRUD Operations");

        // Get test user and channel
        UserDTO user = userRepo.getUserByUsername("testuser");
        List<ChannelDTO> channels = channelRepo.getChannelsByWorkspace(
                workspaceRepo.getWorkspacesByUser(user.getId()).get(0).getId()
        );

        if (user == null || channels.isEmpty()) {
            System.out.println("   ❌ Test user or channel not found.\n");
            return false;
        }

        ChannelDTO channel = channels.get(0);

        // Test Send Message
        System.out.println("   → Sending test message...");
        MessageDTO message = messageRepo.sendMessage("Hello, this is a test message!",
                channel.getId(), user.getId(), MessageType.TEXT, null);
        if (message == null) {
            System.out.println("   ❌ Failed to send message\n");
            return false;
        }
        System.out.println("   ✅ Message sent with ID: " + message.getId());

        // Test Get Message History
        System.out.println("   → Getting message history...");
        List<MessageDTO> history = messageRepo.getRecentMessages(channel.getId());
        System.out.println("   ✅ Found " + history.size() + " message(s)");

        // Test Edit Message
        System.out.println("   → Editing message...");
        boolean edited = messageRepo.editMessage(message.getId(), "Hello, this is an EDITED test message!");
        System.out.println("   ✅ Message edited: " + edited);

        // Test Mark Message as Read
        System.out.println("   → Marking message as read...");
        boolean markedRead = messageRepo.markMessageAsRead(message.getId(), user.getId());
        System.out.println("   ✅ Marked as read: " + markedRead);

        // Test Delete Message
        System.out.println("   → Deleting message...");
        boolean deleted = messageRepo.deleteMessage(message.getId());
        System.out.println("   ✅ Message deleted: " + deleted);

        System.out.println("   ✅ Message CRUD tests passed!\n");
        return true;
    }

    // Test 8: Invite CRUD Operations
    private static boolean testInviteCRUD() {
        System.out.println("📋 Test 8: Invite CRUD Operations");

        // Get test user and workspace
        UserDTO user = userRepo.getUserByUsername("testuser");
        WorkspaceDTO workspace = workspaceRepo.getWorkspacesByUser(user.getId()).get(0);

        if (user == null || workspace == null) {
            System.out.println("   ❌ Test user or workspace not found.\n");
            return false;
        }

        // Test Create One-Time Invite
        System.out.println("   → Creating one-time invite...");
        InviteDTO invite = inviteRepo.createOneTimeInvite("TEST123", workspace.getId(), user.getId());
        if (invite == null) {
            System.out.println("   ❌ Failed to create invite\n");
            return false;
        }
        System.out.println("   ✅ Invite created with code: " + invite.getInviteCode());

        // Test Validate Invite
        System.out.println("   → Validating invite...");
        InviteDTO validated = inviteRepo.validateInviteCode("TEST123");
        if (validated == null) {
            System.out.println("   ❌ Invite validation failed\n");
            return false;
        }
        System.out.println("   ✅ Invite is valid");

        // Test Get Active Invites
        System.out.println("   → Getting active invites for workspace...");
        List<InviteDTO> activeInvites = inviteRepo.getActiveInvitesByWorkspace(workspace.getId());
        System.out.println("   ✅ Found " + activeInvites.size() + " active invite(s)");

        // Test Use Invite
        System.out.println("   → Using invite...");
        boolean used = inviteRepo.useInvite("TEST123");
        System.out.println("   ✅ Invite used: " + used);

        // Test Deactivate Invite
        System.out.println("   → Deactivating invite...");
        boolean deactivated = inviteRepo.deactivateInvite(invite.getId());
        System.out.println("   ✅ Invite deactivated: " + deactivated);

        System.out.println("   ✅ Invite CRUD tests passed!\n");
        return true;
    }

    // Test 9: Audit Log CRUD Operations
    private static boolean testAuditLogCRUD() {
        System.out.println("📋 Test 9: Audit Log CRUD Operations");

        // Get test user and workspace
        UserDTO user = userRepo.getUserByUsername("testuser");
        WorkspaceDTO workspace = workspaceRepo.getWorkspacesByUser(user.getId()).get(0);

        if (user == null || workspace == null) {
            System.out.println("   ❌ Test user or workspace not found.\n");
            return false;
        }

        // Test Create Audit Log Entry
        System.out.println("   → Creating audit log entry...");
        AuditLogEntryDTO log = auditLogRepo.logAction(AuditActionType.USER_LOGIN, "User logged in successfully",
                user.getId(), workspace.getId(), "127.0.0.1");
        if (log == null) {
            System.out.println("   ❌ Failed to create audit log\n");
            return false;
        }
        System.out.println("   ✅ Audit log created with ID: " + log.getId());

        // Test Get Logs by Workspace
        System.out.println("   → Getting logs for workspace...");
        List<AuditLogEntryDTO> logs = auditLogRepo.getLogsByWorkspace(workspace.getId(), 10, 0);
        System.out.println("   ✅ Found " + logs.size() + " log entry(s)");

        // Test Get Log Count
        System.out.println("   → Getting log count...");
        int count = auditLogRepo.getLogCount(workspace.getId());
        System.out.println("   ✅ Total logs in workspace: " + count);

        // Test CSV Export
        System.out.println("   → Testing CSV export...");
        String csv = auditLogRepo.exportToCSV(logs);
        System.out.println("   ✅ CSV export generated (" + csv.length() + " characters)");

        System.out.println("   ✅ Audit Log CRUD tests passed!\n");
        return true;
    }

    // Test 10: File Attachment CRUD Operations
    private static boolean testFileAttachmentCRUD() {
        System.out.println("📋 Test 10: File Attachment CRUD Operations");

        // Get test user, workspace, and channel
        UserDTO user = userRepo.getUserByUsername("testuser");
        WorkspaceDTO workspace = workspaceRepo.getWorkspacesByUser(user.getId()).get(0);
        List<ChannelDTO> channels = channelRepo.getChannelsByWorkspace(workspace.getId());

        if (user == null || workspace == null || channels.isEmpty()) {
            System.out.println("   ❌ Test user, workspace, or channel not found.\n");
            return false;
        }

        ChannelDTO channel = channels.get(0);

        // Test Create File Attachment
        System.out.println("   → Creating file attachment record...");
        FileAttachmentDTO file = fileRepo.createFileAttachment(
                "testfile.txt", "/uploads/testfile.txt", 1024, "text/plain",
                "abc123hash", null, channel.getId(), workspace.getId(),
                user.getId(), null, null, null
        );
        if (file == null) {
            System.out.println("   ❌ Failed to create file attachment\n");
            return false;
        }
        System.out.println("   ✅ File attachment created with ID: " + file.getId());

        // Test Get File by ID
        System.out.println("   → Getting file by ID...");
        FileAttachmentDTO foundFile = fileRepo.getFileAttachmentById(file.getId());
        if (foundFile == null) {
            System.out.println("   ❌ Failed to find file\n");
            return false;
        }
        System.out.println("   ✅ File found: " + foundFile.getFileName());

        // Test Get Files by Channel
        System.out.println("   → Getting files in channel...");
        List<FileAttachmentDTO> channelFiles = fileRepo.getFilesByChannelId(channel.getId(), 10);
        System.out.println("   ✅ Found " + channelFiles.size() + " file(s) in channel");

        // Test Increment Download Count
        System.out.println("   → Incrementing download count...");
        boolean incremented = fileRepo.incrementDownloadCount(file.getId());
        System.out.println("   ✅ Download count incremented: " + incremented);

        // Test Get Total Storage Used
        System.out.println("   → Getting total storage used...");
        long storageUsed = fileRepo.getTotalStorageUsedByWorkspace(workspace.getId());
        System.out.println("   ✅ Storage used: " + storageUsed + " bytes");

        // Test Delete File
        System.out.println("   → Deleting file...");
        boolean deleted = fileRepo.deleteFileAttachment(file.getId());
        System.out.println("   ✅ File deleted: " + deleted);

        System.out.println("   ✅ File Attachment CRUD tests passed!\n");
        return true;
    }
}