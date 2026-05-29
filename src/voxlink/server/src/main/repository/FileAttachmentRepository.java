package voxlink.server.src.main.repository;

import voxlink.server.src.main.database.DBConnection;
import voxlink.shared.dto.FileAttachmentDTO;
import voxlink.shared.dto.FileStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * FileAttachmentRepository handles all database operations for file attachments.
 */
public class FileAttachmentRepository {

    // Create a new file attachment record
    public FileAttachmentDTO createFileAttachment(String fileName, String filePath, long fileSize,
                                                  String fileType, String fileHash, Integer messageId,
                                                  int channelId, int workspaceId, int uploadedBy,
                                                  String thumbnailPath, Integer imageWidth, Integer imageHeight) {
        String sql = "INSERT INTO file_attachments (file_name, file_path, file_size, file_type, file_hash, " +
                "message_id, channel_id, workspace_id, uploaded_by, uploaded_at, thumbnail_path, " +
                "image_width, image_height, download_count, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, fileName);
            stmt.setString(2, filePath);
            stmt.setLong(3, fileSize);
            stmt.setString(4, fileType);
            stmt.setString(5, fileHash);

            if (messageId != null) {
                stmt.setInt(6, messageId);
            } else {
                stmt.setNull(6, java.sql.Types.INTEGER);
            }

            stmt.setInt(7, channelId);
            stmt.setInt(8, workspaceId);
            stmt.setInt(9, uploadedBy);
            stmt.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            stmt.setString(11, thumbnailPath);

            if (imageWidth != null) {
                stmt.setInt(12, imageWidth);
            } else {
                stmt.setNull(12, java.sql.Types.INTEGER);
            }

            if (imageHeight != null) {
                stmt.setInt(13, imageHeight);
            } else {
                stmt.setNull(13, java.sql.Types.INTEGER);
            }

            stmt.setInt(14, 0); // download_count starts at 0
            stmt.setString(15, FileStatus.AVAILABLE.name());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int fileId = generatedKeys.getInt(1);
                        System.out.println("[Database] Created file attachment: " + fileName + " (ID: " + fileId + ")");
                        return getFileAttachmentById(fileId);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to create file attachment: " + e.getMessage());
        }
        return null;
    }

    // Get file attachment by ID
    public FileAttachmentDTO getFileAttachmentById(int fileId) {
        String sql = "SELECT fa.id, fa.file_name, fa.file_path, fa.file_size, fa.file_type, " +
                "fa.file_hash, fa.message_id, fa.channel_id, fa.workspace_id, fa.uploaded_by, " +
                "fa.uploaded_at, fa.download_count, fa.thumbnail_path, fa.image_width, " +
                "fa.image_height, fa.status, " +
                "u.username as uploader_username " +
                "FROM file_attachments fa " +
                "LEFT JOIN users u ON fa.uploaded_by = u.id " +
                "WHERE fa.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fileId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFileAttachmentDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get file attachment by ID: " + e.getMessage());
        }
        return null;
    }

    // Get all file attachments for a message
    public List<FileAttachmentDTO> getFilesByMessageId(int messageId) {
        String sql = "SELECT fa.id, fa.file_name, fa.file_path, fa.file_size, fa.file_type, " +
                "fa.file_hash, fa.message_id, fa.channel_id, fa.workspace_id, fa.uploaded_by, " +
                "fa.uploaded_at, fa.download_count, fa.thumbnail_path, fa.image_width, " +
                "fa.image_height, fa.status, " +
                "u.username as uploader_username " +
                "FROM file_attachments fa " +
                "LEFT JOIN users u ON fa.uploaded_by = u.id " +
                "WHERE fa.message_id = ? AND fa.status != 'DELETED'";

        List<FileAttachmentDTO> files = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapResultSetToFileAttachmentDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get files by message ID: " + e.getMessage());
        }
        return files;
    }

    // Get all file attachments in a channel
    public List<FileAttachmentDTO> getFilesByChannelId(int channelId, int limit) {
        String sql = "SELECT fa.id, fa.file_name, fa.file_path, fa.file_size, fa.file_type, " +
                "fa.file_hash, fa.message_id, fa.channel_id, fa.workspace_id, fa.uploaded_by, " +
                "fa.uploaded_at, fa.download_count, fa.thumbnail_path, fa.image_width, " +
                "fa.image_height, fa.status, " +
                "u.username as uploader_username " +
                "FROM file_attachments fa " +
                "LEFT JOIN users u ON fa.uploaded_by = u.id " +
                "WHERE fa.channel_id = ? AND fa.status != 'DELETED' " +
                "ORDER BY fa.uploaded_at DESC LIMIT ?";

        List<FileAttachmentDTO> files = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapResultSetToFileAttachmentDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get files by channel ID: " + e.getMessage());
        }
        return files;
    }

    // Get all file attachments in a workspace
    public List<FileAttachmentDTO> getFilesByWorkspaceId(int workspaceId, int limit) {
        String sql = "SELECT fa.id, fa.file_name, fa.file_path, fa.file_size, fa.file_type, " +
                "fa.file_hash, fa.message_id, fa.channel_id, fa.workspace_id, fa.uploaded_by, " +
                "fa.uploaded_at, fa.download_count, fa.thumbnail_path, fa.image_width, " +
                "fa.image_height, fa.status, " +
                "u.username as uploader_username " +
                "FROM file_attachments fa " +
                "LEFT JOIN users u ON fa.uploaded_by = u.id " +
                "WHERE fa.workspace_id = ? AND fa.status != 'DELETED' " +
                "ORDER BY fa.uploaded_at DESC LIMIT ?";

        List<FileAttachmentDTO> files = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapResultSetToFileAttachmentDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get files by workspace ID: " + e.getMessage());
        }
        return files;
    }

    // Get files uploaded by a specific user
    public List<FileAttachmentDTO> getFilesByUploader(int userId, int limit) {
        String sql = "SELECT fa.id, fa.file_name, fa.file_path, fa.file_size, fa.file_type, " +
                "fa.file_hash, fa.message_id, fa.channel_id, fa.workspace_id, fa.uploaded_by, " +
                "fa.uploaded_at, fa.download_count, fa.thumbnail_path, fa.image_width, " +
                "fa.image_height, fa.status, " +
                "u.username as uploader_username " +
                "FROM file_attachments fa " +
                "LEFT JOIN users u ON fa.uploaded_by = u.id " +
                "WHERE fa.uploaded_by = ? AND fa.status != 'DELETED' " +
                "ORDER BY fa.uploaded_at DESC LIMIT ?";

        List<FileAttachmentDTO> files = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapResultSetToFileAttachmentDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get files by uploader: " + e.getMessage());
        }
        return files;
    }

    // Increment download count for a file
    public boolean incrementDownloadCount(int fileId) {
        String sql = "UPDATE file_attachments SET download_count = download_count + 1 WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fileId);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("[Database] Incremented download count for file: " + fileId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to increment download count: " + e.getMessage());
        }
        return false;
    }

    // Update file status (UPLOADING, AVAILABLE, DELETED, etc.)
    public boolean updateFileStatus(int fileId, FileStatus status) {
        String sql = "UPDATE file_attachments SET status = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, fileId);

            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to update file status: " + e.getMessage());
            return false;
        }
    }

    // Soft delete a file attachment - mark as deleted
    public boolean deleteFileAttachment(int fileId) {
        String sql = "UPDATE file_attachments SET status = 'DELETED' WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fileId);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("[Database] Soft deleted file attachment: " + fileId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete file attachment: " + e.getMessage());
        }
        return false;
    }

    // Permanently delete a file attachment from database
    public boolean hardDeleteFileAttachment(int fileId) {
        String sql = "DELETE FROM file_attachments WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fileId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Hard deleted file attachment: " + fileId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to hard delete file attachment: " + e.getMessage());
        }
        return false;
    }

    // Delete all file attachments in a channel
    public int deleteFilesByChannelId(int channelId) {
        String sql = "DELETE FROM file_attachments WHERE channel_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, channelId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Deleted " + deleted + " files from channel " + channelId);
            }
            return deleted;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete files by channel: " + e.getMessage());
        }
        return 0;
    }

    // Delete all file attachments in a workspace
    public int deleteFilesByWorkspaceId(int workspaceId) {
        String sql = "DELETE FROM file_attachments WHERE workspace_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("[Database] Deleted " + deleted + " files from workspace " + workspaceId);
            }
            return deleted;
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to delete files by workspace: " + e.getMessage());
        }
        return 0;
    }

    // Get total storage used by a workspace in bytes
    public long getTotalStorageUsedByWorkspace(int workspaceId) {
        String sql = "SELECT COALESCE(SUM(file_size), 0) FROM file_attachments WHERE workspace_id = ? AND status != 'DELETED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get total storage used: " + e.getMessage());
        }
        return 0;
    }

    // Get total number of files in a workspace
    public int getTotalFileCountByWorkspace(int workspaceId) {
        String sql = "SELECT COUNT(*) FROM file_attachments WHERE workspace_id = ? AND status != 'DELETED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get total file count: " + e.getMessage());
        }
        return 0;
    }

    // Get recently uploaded files across the entire server (for admin/stats)
    public List<FileAttachmentDTO> getRecentlyUploadedFiles(int limit) {
        String sql = "SELECT fa.id, fa.file_name, fa.file_path, fa.file_size, fa.file_type, " +
                "fa.file_hash, fa.message_id, fa.channel_id, fa.workspace_id, fa.uploaded_by, " +
                "fa.uploaded_at, fa.download_count, fa.thumbnail_path, fa.image_width, " +
                "fa.image_height, fa.status, " +
                "u.username as uploader_username " +
                "FROM file_attachments fa " +
                "LEFT JOIN users u ON fa.uploaded_by = u.id " +
                "WHERE fa.status != 'DELETED' " +
                "ORDER BY fa.uploaded_at DESC LIMIT ?";

        List<FileAttachmentDTO> files = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapResultSetToFileAttachmentDTO(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to get recently uploaded files: " + e.getMessage());
        }
        return files;
    }

    // Check if a file with the same hash exists
    public FileAttachmentDTO findDuplicateFile(String fileHash, int workspaceId) {
        String sql = "SELECT fa.id, fa.file_name, fa.file_path, fa.file_size, fa.file_type, " +
                "fa.file_hash, fa.message_id, fa.channel_id, fa.workspace_id, fa.uploaded_by, " +
                "fa.uploaded_at, fa.download_count, fa.thumbnail_path, fa.image_width, " +
                "fa.image_height, fa.status, " +
                "u.username as uploader_username " +
                "FROM file_attachments fa " +
                "LEFT JOIN users u ON fa.uploaded_by = u.id " +
                "WHERE fa.file_hash = ? AND fa.workspace_id = ? AND fa.status != 'DELETED' LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fileHash);
            stmt.setInt(2, workspaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFileAttachmentDTO(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Failed to find duplicate file: " + e.getMessage());
        }
        return null;
    }

    // Map ResultSet row to FileAttachmentDTO object
    private FileAttachmentDTO mapResultSetToFileAttachmentDTO(ResultSet rs) throws SQLException {
        FileAttachmentDTO file = new FileAttachmentDTO();
        file.setId(rs.getInt("id"));
        file.setFileName(rs.getString("file_name"));
        file.setFilePath(rs.getString("file_path"));
        file.setFileSize(rs.getLong("file_size"));
        file.setFileType(rs.getString("file_type"));
        file.setFileHash(rs.getString("file_hash"));

        int messageId = rs.getInt("message_id");
        if (!rs.wasNull()) {
            file.setMessageId(messageId);
        }

        file.setChannelId(rs.getInt("channel_id"));
        file.setWorkspaceId(rs.getInt("workspace_id"));
        file.setUploadedBy(rs.getInt("uploaded_by"));
        file.setUploaderUsername(rs.getString("uploader_username"));

        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        if (uploadedAt != null) {
            file.setUploadedAt(uploadedAt.toLocalDateTime());
        }

        file.setDownloadCount(rs.getInt("download_count"));
        file.setThumbnailPath(rs.getString("thumbnail_path"));

        int imageWidth = rs.getInt("image_width");
        if (!rs.wasNull()) {
            file.setImageWidth(imageWidth);
        }

        int imageHeight = rs.getInt("image_height");
        if (!rs.wasNull()) {
            file.setImageHeight(imageHeight);
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                file.setStatus(FileStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                file.setStatus(FileStatus.AVAILABLE);
            }
        }

        return file;
    }
}