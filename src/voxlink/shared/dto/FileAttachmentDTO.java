package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a file attached to a message or shared in a channel.
 */
public class FileAttachmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // Core file information
    private int id;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String fileType;
    private String fileExtension;

    // File hash for duplicate detection
    private String fileHash;

    // Upload information
    private int uploadedBy;
    private String uploaderUsername;
    private LocalDateTime uploadedAt;

    // Message association
    private Integer messageId;
    private int channelId;
    private int workspaceId;

    // Download information
    private int downloadCount;

    // Preview information for images
    private String thumbnailPath;
    private Integer imageWidth;
    private Integer imageHeight;

    // Status
    private FileStatus status;

    // Default constructor
    public FileAttachmentDTO() {
        this.downloadCount = 0;
        this.status = FileStatus.AVAILABLE;
        this.uploadedAt = LocalDateTime.now();
    }

    // Constructor for a new file upload
    public FileAttachmentDTO(String fileName, long fileSize, String fileType, int uploadedBy) {
        this();
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.uploadedBy = uploadedBy;

        // Extract extension from filename
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            this.fileExtension = fileName.substring(lastDot);
        } else {
            this.fileExtension = "";
        }
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public int getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(int uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getUploaderUsername() {
        return uploaderUsername;
    }

    public void setUploaderUsername(String uploaderUsername) {
        this.uploaderUsername = uploaderUsername;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }


    // Ger formatted file size
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }

    // Check if file is an image
    public boolean isImage() {
        return fileType != null && fileType.startsWith("image/");
    }

    // Check if file is a pdf
    public boolean isPdf() {
        return "application/pdf".equals(fileType);
    }

    /**
     * Check if file is a text file
     */
    public boolean isText() {
        return fileType != null && fileType.startsWith("text/");
    }

    // Check if file is a video
    public boolean isVideo() {
        return fileType != null && fileType.startsWith("video/");
    }

    // Check if file is an audio file
    public boolean isAudio() {
        return fileType != null && fileType.startsWith("audio/");
    }


    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FileAttachmentDTO that = (FileAttachmentDTO) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "FileAttachmentDTO{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                ", uploadedBy=" + uploadedBy +
                '}';
    }
}