package voxlink.server.src.main.service;

import voxlink.server.src.main.config.ServerConfig;
import voxlink.server.src.main.repository.FileAttachmentRepository;
import voxlink.server.src.main.repository.MessageRepository;
import voxlink.shared.dto.FileAttachmentDTO;
import voxlink.shared.dto.FileStatus;
import voxlink.shared.dto.MessageDTO;
import voxlink.shared.dto.MessageType;
import voxlink.shared.protocol.FileTransferProtocol;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

/**
 * FileTransferService handles the business logic for file uploads and downloads.
 */
public class FileTransferService {

    private final FileAttachmentRepository fileRepository;
    private final MessageRepository messageRepository;

    // Track active uploads: fileId -> RandomAccessFile
    private final Map<Integer, RandomAccessFile> activeUploads;
    private final Map<Integer, UploadSession> uploadSessions;

    // Storage directories
    private final String uploadDir;
    private final String tempDir;
    private final String thumbnailDir;

    public FileTransferService() {
        this.fileRepository = new FileAttachmentRepository();
        this.messageRepository = new MessageRepository();
        this.activeUploads = new ConcurrentHashMap<>();
        this.uploadSessions = new ConcurrentHashMap<>();

        // Set up directories
        this.uploadDir = ServerConfig.FILE_STORAGE_PATH + "uploads/";
        this.tempDir = ServerConfig.FILE_STORAGE_PATH + "temp/";
        this.thumbnailDir = ServerConfig.FILE_STORAGE_PATH + "thumbnails/";

        createDirectories();
    }

    // Create required directories if they don't exist
    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(tempDir));
            Files.createDirectories(Paths.get(thumbnailDir));
            System.out.println("[FileTransferService] Storage directories initialized");
        } catch (IOException e) {
            System.err.println("[FileTransferService] Failed to create directories: " + e.getMessage());
        }
    }

    // Initialize a new file upload
    public FileAttachmentDTO initiateUpload(String fileName, long fileSize, String fileType,
                                            String fileHash, int channelId, int uploadedBy) {
        // Validate file size
        if (fileSize > FileTransferProtocol.MAX_FILE_SIZE) {
            System.err.println("[FileTransferService] File too large: " + fileSize);
            return null;
        }

        // Validate file type
        if (!FileTransferProtocol.isAllowedFileType(fileType)) {
            System.err.println("[FileTransferService] Invalid file type: " + fileType);
            return null;
        }

        // Check for duplicate file
        FileAttachmentDTO existingFile = fileRepository.findDuplicateFile(fileHash, -1);
        if (existingFile != null) {
            System.out.println("[FileTransferService] Duplicate file detected: " + fileName);
            return existingFile;
        }

        // Create file record in database with UPLOADING status
        FileAttachmentDTO file = fileRepository.createFileAttachment(
                fileName, "", fileSize, fileType, fileHash,
                null, channelId, -1, uploadedBy,
                null, null, null
        );

        if (file == null) {
            return null;
        }

        // Create temporary file for chunk assembly
        String tempFilePath = tempDir + file.getId() + FileTransferProtocol.TEMP_FILE_EXTENSION;
        try {
            RandomAccessFile raf = new RandomAccessFile(tempFilePath, "rw");
            raf.setLength(fileSize); // Pre-allocate space
            activeUploads.put(file.getId(), raf);

            UploadSession session = new UploadSession(file.getId(), fileName, fileSize, fileType, fileHash, channelId, uploadedBy);
            uploadSessions.put(file.getId(), session);

            System.out.println("[FileTransferService] Upload initiated: fileId=" + file.getId() + ", fileName=" + fileName);
            return file;

        } catch (IOException e) {
            System.err.println("[FileTransferService] Failed to create temp file: " + e.getMessage());
            return null;
        }
    }

    // Write a chunk of data to the temporary file
    public boolean writeChunk(int fileId, int chunkIndex, byte[] data, long offset) {
        RandomAccessFile raf = activeUploads.get(fileId);
        if (raf == null) {
            System.err.println("[FileTransferService] No active upload for fileId: " + fileId);
            return false;
        }

        try {
            synchronized (raf) {
                raf.seek(offset);
                raf.write(data);
            }

            // Update session progress
            UploadSession session = uploadSessions.get(fileId);
            if (session != null) {
                session.setReceivedBytes(session.getReceivedBytes() + data.length);
                session.setLastChunkIndex(chunkIndex);
            }

            return true;

        } catch (IOException e) {
            System.err.println("[FileTransferService] Failed to write chunk: " + e.getMessage());
            return false;
        }
    }

    // Complete an upload - verify file, generate thumbnail, move to final location
    public FileAttachmentDTO completeUpload(int fileId, int channelId, int uploadedBy) {
        RandomAccessFile raf = activeUploads.remove(fileId);
        UploadSession session = uploadSessions.remove(fileId);

        if (raf == null || session == null) {
            System.err.println("[FileTransferService] No active upload session for fileId: " + fileId);
            return null;
        }

        try {
            raf.close();

            String tempFilePath = tempDir + fileId + FileTransferProtocol.TEMP_FILE_EXTENSION;
            Path tempPath = Paths.get(tempFilePath);

            // Verify file size
            long actualSize = Files.size(tempPath);
            if (actualSize != session.getFileSize()) {
                System.err.println("[FileTransferService] File size mismatch. Expected: " + session.getFileSize() + ", Actual: " + actualSize);
                Files.delete(tempPath);
                return null;
            }

            // Generate unique final file name
            String extension = FileTransferProtocol.getFileExtension(session.getFileName());
            String finalFileName = fileId + "_" + System.currentTimeMillis() + extension;
            String finalFilePath = uploadDir + getDatePath() + finalFileName;
            String finalFullPath = uploadDir + getDatePath();

            // Create date-based subdirectory
            Files.createDirectories(Paths.get(finalFullPath));
            finalFilePath = finalFullPath + finalFileName;

            // Move file to final location
            Files.move(tempPath, Paths.get(finalFilePath));

            // Generate thumbnail for images
            String thumbnailPath = null;
            Integer imageWidth = null;
            Integer imageHeight = null;

            if (session.getFileType().startsWith("image/")) {
                ThumbnailResult thumbnail = generateThumbnail(finalFilePath, fileId);
                if (thumbnail != null) {
                    thumbnailPath = thumbnail.getPath();
                    imageWidth = thumbnail.getWidth();
                    imageHeight = thumbnail.getHeight();
                }
            }

            // Update file record in database
            FileAttachmentDTO file = fileRepository.getFileAttachmentById(fileId);
            if (file != null) {
                file.setFilePath(finalFilePath);
                file.setThumbnailPath(thumbnailPath);
                file.setImageWidth(imageWidth);
                file.setImageHeight(imageHeight);
                file.setStatus(FileStatus.AVAILABLE);
                fileRepository.updateFileStatus(fileId, FileStatus.AVAILABLE);
            }

            // Create a message for the file (so it appears in chat)
            String messageContent = "📎 **" + session.getFileName() + "** (" + formatFileSize(session.getFileSize()) + ")";
            MessageDTO message = messageRepository.sendMessage(
                    messageContent, channelId, uploadedBy, MessageType.FILE, null
            );

            if (message != null && file != null) {
                file.setMessageId(message.getId());
                // Update file with message ID
            }

            System.out.println("[FileTransferService] Upload completed: fileId=" + fileId + ", fileName=" + session.getFileName());
            return file;

        } catch (IOException e) {
            System.err.println("[FileTransferService] Failed to complete upload: " + e.getMessage());
            return null;
        }
    }

    // Cancel an ongoing upload
    public void cancelUpload(int fileId) {
        RandomAccessFile raf = activeUploads.remove(fileId);
        uploadSessions.remove(fileId);

        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                System.err.println("[FileTransferService] Error closing file: " + e.getMessage());
            }
        }

        // Delete temp file
        String tempFilePath = tempDir + fileId + FileTransferProtocol.TEMP_FILE_EXTENSION;
        try {
            Files.deleteIfExists(Paths.get(tempFilePath));
        } catch (IOException e) {
            System.err.println("[FileTransferService] Failed to delete temp file: " + e.getMessage());
        }

        // Update database status
        fileRepository.updateFileStatus(fileId, FileStatus.UPLOAD_FAILED);

        System.out.println("[FileTransferService] Upload cancelled: fileId=" + fileId);
    }

    // Get file data for download
    public FileDownloadData getFileForDownload(int fileId) {
        FileAttachmentDTO file = fileRepository.getFileAttachmentById(fileId);
        if (file == null || file.getStatus() != FileStatus.AVAILABLE) {
            return null;
        }

        Path filePath = Paths.get(file.getFilePath());
        if (!Files.exists(filePath)) {
            System.err.println("[FileTransferService] File not found on disk: " + file.getFilePath());
            return null;
        }

        try {
            byte[] fileData = Files.readAllBytes(filePath);
            return new FileDownloadData(file, fileData);
        } catch (IOException e) {
            System.err.println("[FileTransferService] Failed to read file: " + e.getMessage());
            return null;
        }
    }

    // Get file as stream for chunked download
    public FileInputStream getFileStream(int fileId) {
        FileAttachmentDTO file = fileRepository.getFileAttachmentById(fileId);
        if (file == null || file.getStatus() != FileStatus.AVAILABLE) {
            return null;
        }

        try {
            return new FileInputStream(file.getFilePath());
        } catch (FileNotFoundException e) {
            System.err.println("[FileTransferService] File not found: " + e.getMessage());
            return null;
        }
    }

    // Delete a file (soft delete)
    public boolean deleteFile(int fileId, int userId) {
        FileAttachmentDTO file = fileRepository.getFileAttachmentById(fileId);
        if (file == null) {
            return false;
        }

        // Soft delete from database
        boolean deleted = fileRepository.deleteFileAttachment(fileId);

        if (deleted) {
            System.out.println("[FileTransferService] File deleted: fileId=" + fileId + " by user=" + userId);
        }

        return deleted;
    }

    // Generate thumbnail for image files
    private ThumbnailResult generateThumbnail(String imagePath, int fileId) {
        try {
            BufferedImage originalImage = ImageIO.read(new File(imagePath));
            if (originalImage == null) {
                return null;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // Calculate thumbnail dimensions (max 200px)
            int thumbWidth = 200;
            int thumbHeight = (thumbWidth * originalHeight) / originalWidth;

            // Create scaled image
            Image scaledImage = originalImage.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH);
            BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            // Save thumbnail
            String thumbnailFileName = fileId + "_thumb.jpg";
            String thumbnailPath = thumbnailDir + thumbnailFileName;
            ImageIO.write(thumbnail, "jpg", new File(thumbnailPath));

            return new ThumbnailResult(thumbnailPath, originalWidth, originalHeight);

        } catch (IOException e) {
            System.err.println("[FileTransferService] Failed to generate thumbnail: " + e.getMessage());
            return null;
        }
    }

    // Calculate CRC32 checksum of a file
    public static long calculateChecksum(String filePath) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            CRC32 crc = new CRC32();
            byte[] buffer = new byte[FileTransferProtocol.CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
            return crc.getValue();
        } catch (IOException e) {
            System.err.println("[FileTransferService] Failed to calculate checksum: " + e.getMessage());
            return 0;
        }
    }

    // Calculate SHA-256 hash of a file
    public static String calculateFileHash(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = new FileInputStream(filePath)) {
                byte[] buffer = new byte[FileTransferProtocol.CHUNK_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("[FileTransferService] Failed to calculate hash: " + e.getMessage());
            return null;
        }
    }

    // Get date-based path for organized storage (YYYY/MM/)
    private String getDatePath() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/");
        return LocalDateTime.now().format(formatter);
    }

    public FileAttachmentDTO getFileAttachment(int fileId) {
        return fileRepository.getFileAttachmentById(fileId);
    }

    public void incrementDownloadCount(int fileId) {
        fileRepository.incrementDownloadCount(fileId);
    }

    // Format file size for display
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    // Clean up old temp files (should be called periodically)
    public void cleanupTempFiles() {
        File tempFolder = new File(tempDir);
        File[] tempFiles = tempFolder.listFiles((dir, name) -> name.endsWith(FileTransferProtocol.TEMP_FILE_EXTENSION));

        if (tempFiles != null) {
            long now = System.currentTimeMillis();
            long maxAge = 24 * 60 * 60 * 1000; // 24 hours

            for (File tempFile : tempFiles) {
                if (now - tempFile.lastModified() > maxAge) {
                    tempFile.delete();
                    System.out.println("[FileTransferService] Cleaned up old temp file: " + tempFile.getName());
                }
            }
        }
    }

    // Session data for an ongoing upload
    private static class UploadSession {
        private final int fileId;
        private final String fileName;
        private final long fileSize;
        private final String fileType;
        private final String fileHash;
        private final int channelId;
        private final int uploadedBy;
        private long receivedBytes;
        private int lastChunkIndex;

        public UploadSession(int fileId, String fileName, long fileSize, String fileType,
                             String fileHash, int channelId, int uploadedBy) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileType = fileType;
            this.fileHash = fileHash;
            this.channelId = channelId;
            this.uploadedBy = uploadedBy;
            this.receivedBytes = 0;
            this.lastChunkIndex = -1;
        }

        public int getFileId() { return fileId; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getFileType() { return fileType; }
        public String getFileHash() { return fileHash; }
        public int getChannelId() { return channelId; }
        public int getUploadedBy() { return uploadedBy; }
        public long getReceivedBytes() { return receivedBytes; }
        public void setReceivedBytes(long receivedBytes) { this.receivedBytes = receivedBytes; }
        public int getLastChunkIndex() { return lastChunkIndex; }
        public void setLastChunkIndex(int lastChunkIndex) { this.lastChunkIndex = lastChunkIndex; }
    }

    // Result of thumbnail generation
    private static class ThumbnailResult {
        private final String path;
        private final int width;
        private final int height;

        public ThumbnailResult(String path, int width, int height) {
            this.path = path;
            this.width = width;
            this.height = height;
        }

        public String getPath() { return path; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    // File download data container
    public static class FileDownloadData {
        private final FileAttachmentDTO metadata;
        private final byte[] data;

        public FileDownloadData(FileAttachmentDTO metadata, byte[] data) {
            this.metadata = metadata;
            this.data = data;
        }

        public FileAttachmentDTO getMetadata() { return metadata; }
        public byte[] getData() { return data; }
    }
}