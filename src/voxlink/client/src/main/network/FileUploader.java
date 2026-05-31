package voxlink.client.src.main.network;

import voxlink.client.src.main.state.UserStore;
import voxlink.shared.dto.FileAttachmentDTO;
import voxlink.shared.protocol.FileTransferProtocol;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FileUploader handles file uploads from client to server.
 */
public class FileUploader {

    private final ServerConnection connection;
    private final UserStore userStore;
    private final AtomicBoolean isUploading;

    private UploadProgressListener progressListener;
    private int currentFileId;
    private String currentFileName;
    private long currentFileSize;
    private long bytesUploaded;
    private int chunksSent;

    public FileUploader() {
        this.connection = ServerConnection.getInstance();
        this.userStore = UserStore.getInstance();
        this.isUploading = new AtomicBoolean(false);
    }

    // Set progress listener for upload callbacks
    public void setProgressListener(UploadProgressListener listener) {
        this.progressListener = listener;
    }

    // Upload a file to a channel
    public CompletableFuture<FileAttachmentDTO> uploadFile(File file, int channelId, int messageId) {
        CompletableFuture<FileAttachmentDTO> future = new CompletableFuture<>();

        if (isUploading.get()) {
            future.completeExceptionally(new IllegalStateException("Another upload is in progress"));
            return future;
        }

        if (!file.exists() || !file.canRead()) {
            future.completeExceptionally(new IOException("File not found or cannot be read: " + file.getPath()));
            return future;
        }

        if (file.length() > FileTransferProtocol.MAX_FILE_SIZE) {
            future.completeExceptionally(new IOException("File too large. Max size: " +
                    formatFileSize(FileTransferProtocol.MAX_FILE_SIZE)));
            return future;
        }

        // Validate file type
        String mimeType = getMimeType(file.getName());
        if (!FileTransferProtocol.isAllowedFileType(mimeType)) {
            future.completeExceptionally(new IOException("File type not allowed: " + mimeType));
            return future;
        }

        isUploading.set(true);
        currentFileName = file.getName();
        currentFileSize = file.length();
        bytesUploaded = 0;
        chunksSent = 0;

        // Calculate file hash
        String fileHash = calculateFileHash(file);

        // Start upload in background thread
        new Thread(() -> {
            try {
                // Step 1: Send upload start
                FileAttachmentDTO result = performUpload(file, channelId, messageId, fileHash, future);
                if (result != null) {
                    future.complete(result);
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                isUploading.set(false);
            }
        }).start();

        return future;
    }

    // Perform the actual upload process
    private FileAttachmentDTO performUpload(File file, int channelId, int messageId,
                                            String fileHash, CompletableFuture<FileAttachmentDTO> future)
            throws Exception {

        // Send upload start
        FileTransferProtocol.UploadStartData startData = new FileTransferProtocol.UploadStartData(
                file.getName(), file.length(), getMimeType(file.getName()), fileHash, channelId, messageId
        );

        Packet startPacket = new Packet(RequestType.FILE_UPLOAD_START);
        startPacket.setAuthToken(userStore.getAuthToken());
        startPacket.setUserId(userStore.getUserId());
        startPacket.put("uploadData", startData);

        connection.sendPacket(startPacket);

        // Wait for acknowledgment with timeout
        Packet startResponse = waitForResponse(ResponseType.FILE_UPLOAD_START_ACK, 30000);
        if (startResponse == null || startResponse.isError()) {
            throw new IOException("Server rejected upload: " +
                    (startResponse != null ? startResponse.getErrorMessage() : "Timeout"));
        }

        Integer fileId = (Integer) startResponse.get("fileId");
        Integer chunkSize = (Integer) startResponse.get("chunkSize");
        Integer totalChunks = (Integer) startResponse.get("totalChunks");

        if (fileId == null) {
            throw new IOException("Server did not provide file ID");
        }

        this.currentFileId = fileId;

        // Send file chunks
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[FileTransferProtocol.CHUNK_SIZE];
            int chunkIndex = 0;
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                // Prepare chunk data
                byte[] chunkData = buffer;
                if (bytesRead < FileTransferProtocol.CHUNK_SIZE) {
                    chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                }

                boolean isLast = bytesRead < FileTransferProtocol.CHUNK_SIZE;

                FileTransferProtocol.UploadChunkData chunkDataObj = new FileTransferProtocol.UploadChunkData(
                        fileId, chunkIndex, totalChunks, chunkData, isLast
                );

                Packet chunkPacket = new Packet(RequestType.FILE_UPLOAD_CHUNK);
                chunkPacket.setAuthToken(userStore.getAuthToken());
                chunkPacket.setUserId(userStore.getUserId());
                chunkPacket.put("chunkData", chunkDataObj);

                connection.sendPacket(chunkPacket);

                // Wait for chunk acknowledgment
                Packet chunkResponse = waitForResponse(ResponseType.FILE_UPLOAD_CHUNK_ACK, 10000);
                if (chunkResponse == null || chunkResponse.isError()) {
                    throw new IOException("Failed to send chunk " + chunkIndex);
                }

                // Update progress
                bytesUploaded += bytesRead;
                chunksSent = chunkIndex + 1;
                int progressPercent = (int) ((bytesUploaded * 100) / currentFileSize);

                if (progressListener != null) {
                    progressListener.onProgress(progressPercent, bytesUploaded, currentFileSize);
                }

                chunkIndex++;
            }
        }

        // Send upload complete
        Packet completePacket = new Packet(RequestType.FILE_UPLOAD_COMPLETE);
        completePacket.setAuthToken(userStore.getAuthToken());
        completePacket.setUserId(userStore.getUserId());
        completePacket.put("fileId", fileId);
        completePacket.put("channelId", channelId);

        connection.sendPacket(completePacket);

        // Wait for completion acknowledgment
        Packet completeResponse = waitForResponse(ResponseType.FILE_UPLOAD_COMPLETE_ACK, 30000);
        if (completeResponse == null || completeResponse.isError()) {
            throw new IOException("Server failed to complete upload: " +
                    (completeResponse != null ? completeResponse.getErrorMessage() : "Timeout"));
        }

        FileAttachmentDTO uploadedFile = (FileAttachmentDTO) completeResponse.get("file");

        if (progressListener != null) {
            progressListener.onComplete(uploadedFile);
        }

        System.out.println("[FileUploader] Upload completed: " + currentFileName);
        return uploadedFile;
    }

    // Cancel ongoing upload
    public void cancelUpload() {
        if (isUploading.get() && currentFileId > 0) {
            // Send cancel notification to server
            Packet cancelPacket = new Packet(RequestType.FILE_UPLOAD_FAILURE);
            cancelPacket.setAuthToken(userStore.getAuthToken());
            cancelPacket.setUserId(userStore.getUserId());
            cancelPacket.put("fileId", currentFileId);
            connection.sendPacket(cancelPacket);

            isUploading.set(false);

            if (progressListener != null) {
                progressListener.onError("Upload cancelled by user");
            }

            System.out.println("[FileUploader] Upload cancelled: " + currentFileName);
        }
    }

   // Check if currently uploading
    public boolean isUploading() {
        return isUploading.get();
    }

    // Get current upload progress percentage
    public int getProgressPercent() {
        if (currentFileSize == 0) return 0;
        return (int) ((bytesUploaded * 100) / currentFileSize);
    }

    // Get current file name being uploaded
    public String getCurrentFileName() {
        return currentFileName;
    }

    // Wait for a specific response type
    private Packet waitForResponse(ResponseType responseType, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        Packet[] responseHolder = new Packet[1];

        ServerConnection.PacketListener listener = new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet packet) {
                if (packet.getResponseType() == responseType) {
                    responseHolder[0] = packet;
                }
            }
        };

        connection.addPacketListener(listener);

        try {
            while (System.currentTimeMillis() - startTime < timeoutMs && responseHolder[0] == null) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            connection.removePacketListener(listener);
        }

        return responseHolder[0];
    }

    // Calculate SHA-256 hash of a file
    private String calculateFileHash(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[FileTransferProtocol.CHUNK_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
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
            System.err.println("[FileUploader] Failed to calculate hash: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get MIME type from file name
     * @param fileName File name
     * @return MIME type string
     */
    private String getMimeType(String fileName) {
        String extension = FileTransferProtocol.getFileExtension(fileName).toLowerCase();
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            case ".txt" -> "text/plain";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".mp4" -> "video/mp4";
            case ".mp3" -> "audio/mpeg";
            case ".zip" -> "application/zip";
            default -> "application/octet-stream";
        };
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

    // Progress listener interface for upload callbacks
    public interface UploadProgressListener {
        void onProgress(int percent, long bytesUploaded, long totalBytes);
        void onComplete(FileAttachmentDTO file);
        void onError(String error);
    }
}