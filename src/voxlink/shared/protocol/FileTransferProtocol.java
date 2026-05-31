package voxlink.shared.protocol;

import java.io.Serial;
import java.io.Serializable;

/**
 * FileTransferProtocol defines constants and data structures for file transfer
 */
public class FileTransferProtocol {

    // --- CONFIGURATION CONSTANTS ---

    // Maximum chunk size in bytes (64 KB)
    public static final int CHUNK_SIZE = 64 * 1024;

    // Maximum file size allowed for upload (100 MB)
    public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    // Maximum file name length
    public static final int MAX_FILE_NAME_LENGTH = 255;

    // Temporary file extension while uploading
    public static final String TEMP_FILE_EXTENSION = ".tmp";

    // Socket timeout for file transfer in milliseconds
    public static final int FILE_TRANSFER_TIMEOUT_MS = 5 * 60 * 1000;

    // Status codes
    public static final int STATUS_OK = 200;
    public static final int STATUS_FILE_NOT_FOUND = 404;
    public static final int STATUS_FILE_TOO_LARGE = 413;
    public static final int STATUS_INVALID_FILE_TYPE = 415;
    public static final int STATUS_UPLOAD_FAILED = 500;
    public static final int STATUS_DOWNLOAD_FAILED = 501;
    public static final int STATUS_PERMISSION_DENIED = 403;
    public static final int STATUS_STORAGE_FULL = 507;

    // Supported MIME types
    public static final String[] ALLOWED_MIME_TYPES = {
            // Images
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp",
            // Documents
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv",
            // Archives
            "application/zip", "application/x-rar-compressed",
            // Audio
            "audio/mpeg", "audio/wav", "audio/ogg",
            // Video
            "video/mp4", "video/x-msvideo", "video/quicktime"
    };

    // Data structure for initiating a file upload
    public static class UploadStartData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String fileName;
        private long fileSize;
        private String fileType;
        private String fileHash;
        private int channelId;
        private int messageId;

        public UploadStartData() {}

        public UploadStartData(String fileName, long fileSize, String fileType,
                               String fileHash, int channelId, int messageId) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileType = fileType;
            this.fileHash = fileHash;
            this.channelId = channelId;
            this.messageId = messageId;
        }

        // Getters and Setters
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
        public int getChannelId() { return channelId; }
        public void setChannelId(int channelId) { this.channelId = channelId; }
        public int getMessageId() { return messageId; }
        public void setMessageId(int messageId) { this.messageId = messageId; }

        @Override
        public String toString() {
            return "UploadStartData{" +
                    "fileName='" + fileName + '\'' +
                    ", fileSize=" + fileSize +
                    ", fileType='" + fileType + '\'' +
                    ", channelId=" + channelId +
                    '}';
        }
    }

    // Data structure for a single file chunk during upload
    public static class UploadChunkData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private int fileId;
        private int chunkIndex;
        private int totalChunks;
        private byte[] data;
        private boolean isLast;

        public UploadChunkData() {}

        public UploadChunkData(int fileId, int chunkIndex, int totalChunks,
                               byte[] data, boolean isLast) {
            this.fileId = fileId;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.data = data;
            this.isLast = isLast;
        }

        // Getters and Setters
        public int getFileId() { return fileId; }
        public void setFileId(int fileId) { this.fileId = fileId; }
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        public boolean isLast() { return isLast; }
        public void setLast(boolean last) { isLast = last; }

        @Override
        public String toString() {
            return "UploadChunkData{" +
                    "fileId=" + fileId +
                    ", chunkIndex=" + chunkIndex +
                    ", totalChunks=" + totalChunks +
                    ", dataSize=" + (data != null ? data.length : 0) +
                    ", isLast=" + isLast +
                    '}';
        }
    }

    // Data structure for requesting a file download
    public static class DownloadRequestData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private int fileId;
        private int channelId;
        private int requestedBy;

        public DownloadRequestData() {}

        public DownloadRequestData(int fileId, int channelId, int requestedBy) {
            this.fileId = fileId;
            this.channelId = channelId;
            this.requestedBy = requestedBy;
        }

        // Getters and Setters
        public int getFileId() { return fileId; }
        public void setFileId(int fileId) { this.fileId = fileId; }
        public int getChannelId() { return channelId; }
        public void setChannelId(int channelId) { this.channelId = channelId; }
        public int getRequestedBy() { return requestedBy; }
        public void setRequestedBy(int requestedBy) { this.requestedBy = requestedBy; }

        @Override
        public String toString() {
            return "DownloadRequestData{" +
                    "fileId=" + fileId +
                    ", channelId=" + channelId +
                    ", requestedBy=" + requestedBy +
                    '}';
        }
    }

    // Data structure for a single file chunk during download
    public static class DownloadChunkData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private int fileId;
        private int chunkIndex;
        private int totalChunks;
        private byte[] data;
        private boolean isLast;
        private String fileName;
        private long fileSize;

        public DownloadChunkData() {}

        public DownloadChunkData(int fileId, int chunkIndex, int totalChunks,
                                 byte[] data, boolean isLast, String fileName, long fileSize) {
            this.fileId = fileId;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.data = data;
            this.isLast = isLast;
            this.fileName = fileName;
            this.fileSize = fileSize;
        }

        // Getters and Setters
        public int getFileId() { return fileId; }
        public void setFileId(int fileId) { this.fileId = fileId; }
        public int getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        public boolean isLast() { return isLast; }
        public void setLast(boolean last) { isLast = last; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        @Override
        public String toString() {
            return "DownloadChunkData{" +
                    "fileId=" + fileId +
                    ", chunkIndex=" + chunkIndex +
                    ", totalChunks=" + totalChunks +
                    ", dataSize=" + (data != null ? data.length : 0) +
                    ", isLast=" + isLast +
                    ", fileName='" + fileName + '\'' +
                    '}';
        }
    }

    // Data structure for upload completion notification
    public static class UploadCompleteData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private int fileId;
        private String filePath;
        private String thumbnailPath;
        private long finalSize;
        private boolean success;
        private String errorMessage;

        public UploadCompleteData() {}

        public UploadCompleteData(int fileId, String filePath, String thumbnailPath,
                                  long finalSize, boolean success, String errorMessage) {
            this.fileId = fileId;
            this.filePath = filePath;
            this.thumbnailPath = thumbnailPath;
            this.finalSize = finalSize;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        // Getters and Setters
        public int getFileId() { return fileId; }
        public void setFileId(int fileId) { this.fileId = fileId; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getThumbnailPath() { return thumbnailPath; }
        public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
        public long getFinalSize() { return finalSize; }
        public void setFinalSize(long finalSize) { this.finalSize = finalSize; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        @Override
        public String toString() {
            return "UploadCompleteData{" +
                    "fileId=" + fileId +
                    ", success=" + success +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }


    // Check if a file type is allowed
    public static boolean isAllowedFileType(String mimeType) {
        if (mimeType == null) return false;
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (allowed.equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }

    // Get file extension from file name
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }

    // A unique file name to avoid collisions
    public static String generateUniqueFileName(String originalName, int fileId, long timestamp) {
        String extension = getFileExtension(originalName);
        return fileId + "_" + timestamp + extension;
    }

    // Calculate number of chunks needed for a file
    public static int calculateChunkCount(long fileSize) {
        return (int) Math.ceil((double) fileSize / CHUNK_SIZE);
    }

    // Get status message for status code
    public static String getStatusMessage(int statusCode) {
        return switch (statusCode) {
            case STATUS_OK -> "OK";
            case STATUS_FILE_NOT_FOUND -> "File not found";
            case STATUS_FILE_TOO_LARGE -> "File too large";
            case STATUS_INVALID_FILE_TYPE -> "Invalid file type";
            case STATUS_UPLOAD_FAILED -> "Upload failed";
            case STATUS_DOWNLOAD_FAILED -> "Download failed";
            case STATUS_PERMISSION_DENIED -> "Permission denied";
            case STATUS_STORAGE_FULL -> "Storage full";
            default -> "Unknown error";
        };
    }
}