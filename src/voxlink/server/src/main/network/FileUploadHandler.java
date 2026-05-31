package voxlink.server.src.main.network;

import voxlink.server.src.main.service.FileTransferService;
import voxlink.shared.dto.FileAttachmentDTO;
import voxlink.shared.protocol.FileTransferProtocol;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileUploadHandler processes file upload requests from clients.
 */
public class FileUploadHandler {

    private final FileTransferService fileTransferService;
    private final Map<Integer, UploadContext> activeUploads;

    public FileUploadHandler() {
        this.fileTransferService = new FileTransferService();
        this.activeUploads = new ConcurrentHashMap<>();
    }

    // Process a file upload packet
    public Packet processPacket(Packet packet, ClientHandler clientHandler) {
        RequestType type = packet.getRequestType();

        if (type == RequestType.FILE_UPLOAD_START) {
            return handleUploadStart(packet, clientHandler);
        } else if (type == RequestType.FILE_UPLOAD_CHUNK) {
            return handleUploadChunk(packet, clientHandler);
        } else if (type == RequestType.FILE_UPLOAD_COMPLETE) {
            return handleUploadComplete(packet, clientHandler);
        }

        return null;
    }

    // Handle upload start request
    private Packet handleUploadStart(Packet packet, ClientHandler clientHandler) {
        try {
            // Extract upload start data
            Object dataObj = packet.get("uploadData");
            FileTransferProtocol.UploadStartData uploadData = null;

            if (dataObj instanceof FileTransferProtocol.UploadStartData) {
                uploadData = (FileTransferProtocol.UploadStartData) dataObj;
            } else if (dataObj instanceof byte[]) {
                // Deserialize if sent as byte array
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream((byte[]) dataObj))) {
                    uploadData = (FileTransferProtocol.UploadStartData) ois.readObject();
                }
            }

            if (uploadData == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_INVALID_FILE_TYPE, "Invalid upload data");
            }

            // Check if user has permission to upload to this channel
            if (!clientHandler.isMemberOfChannel(uploadData.getChannelId())) {
                return createErrorResponse(FileTransferProtocol.STATUS_PERMISSION_DENIED, "Not a member of this channel");
            }

            // Initiate upload
            FileAttachmentDTO file = fileTransferService.initiateUpload(
                    uploadData.getFileName(),
                    uploadData.getFileSize(),
                    uploadData.getFileType(),
                    uploadData.getFileHash(),
                    uploadData.getChannelId(),
                    clientHandler.getUserId()
            );

            if (file == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "Failed to initiate upload");
            }

            // Create upload context
            UploadContext context = new UploadContext(
                    file.getId(),
                    uploadData.getFileName(),
                    uploadData.getFileSize(),
                    uploadData.getFileType(),
                    uploadData.getChannelId(),
                    clientHandler.getUserId()
            );
            activeUploads.put(file.getId(), context);

            // Create response
            Packet response = new Packet(ResponseType.FILE_UPLOAD_START_ACK);
            response.put("fileId", file.getId());
            response.put("chunkSize", FileTransferProtocol.CHUNK_SIZE);
            response.put("totalChunks", FileTransferProtocol.calculateChunkCount(uploadData.getFileSize()));
            response.success();

            System.out.println("[FileUploadHandler] Upload started: fileId=" + file.getId() +
                    ", fileName=" + uploadData.getFileName() +
                    ", size=" + uploadData.getFileSize() +
                    ", from=" + clientHandler.getUsername());

            return response;

        } catch (Exception e) {
            System.err.println("[FileUploadHandler] Error handling upload start: " + e.getMessage());
            return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "Server error: " + e.getMessage());
        }
    }

    // Handle upload chunk request
    private Packet handleUploadChunk(Packet packet, ClientHandler clientHandler) {
        try {
            // Extract chunk data
            Object dataObj = packet.get("chunkData");
            FileTransferProtocol.UploadChunkData chunkData = null;

            if (dataObj instanceof FileTransferProtocol.UploadChunkData) {
                chunkData = (FileTransferProtocol.UploadChunkData) dataObj;
            } else if (dataObj instanceof byte[]) {
                try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream((byte[]) dataObj))) {
                    chunkData = (FileTransferProtocol.UploadChunkData) ois.readObject();
                }
            }

            if (chunkData == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "Invalid chunk data");
            }

            // Get upload context
            UploadContext context = activeUploads.get(chunkData.getFileId());
            if (context == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "No active upload for fileId: " + chunkData.getFileId());
            }

            // Calculate offset for this chunk
            long offset = (long) chunkData.getChunkIndex() * FileTransferProtocol.CHUNK_SIZE;

            // Write chunk
            boolean success = fileTransferService.writeChunk(
                    chunkData.getFileId(),
                    chunkData.getChunkIndex(),
                    chunkData.getData(),
                    offset
            );

            if (!success) {
                return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "Failed to write chunk");
            }

            // Update context progress
            context.setReceivedChunks(context.getReceivedChunks() + 1);
            context.setLastChunkIndex(chunkData.getChunkIndex());

            // Send acknowledgment
            Packet response = new Packet(ResponseType.FILE_UPLOAD_CHUNK_ACK);
            response.put("fileId", chunkData.getFileId());
            response.put("chunkIndex", chunkData.getChunkIndex());
            response.put("received", true);
            response.success();

            // Log progress every 10 chunks
            if (context.getReceivedChunks() % 10 == 0) {
                int progress = (int) ((context.getReceivedChunks() * 100.0) / chunkData.getTotalChunks());
                System.out.println("[FileUploadHandler] Upload progress: fileId=" + chunkData.getFileId() +
                        ", progress=" + progress + "% (" + context.getReceivedChunks() +
                        "/" + chunkData.getTotalChunks() + ")");
            }

            return response;

        } catch (Exception e) {
            System.err.println("[FileUploadHandler] Error handling upload chunk: " + e.getMessage());
            return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "Server error: " + e.getMessage());
        }
    }

    // Handle upload complete request
    private Packet handleUploadComplete(Packet packet, ClientHandler clientHandler) {
        try {
            Integer fileId = (Integer) packet.get("fileId");
            Integer channelId = (Integer) packet.get("channelId");

            if (fileId == null || channelId == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "Missing fileId or channelId");
            }

            // Complete the upload
            FileAttachmentDTO file = fileTransferService.completeUpload(fileId, channelId, clientHandler.getUserId());

            if (file == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "Failed to complete upload");
            }

            // Remove from active uploads
            activeUploads.remove(fileId);

            // Broadcast file message to channel
            broadcastFileMessage(file, channelId, clientHandler);

            // Create response
            Packet response = new Packet(ResponseType.FILE_UPLOAD_COMPLETE_ACK);
            response.put("fileId", fileId);
            response.put("file", file);
            response.success();

            System.out.println("[FileUploadHandler] Upload completed: fileId=" + fileId +
                    ", fileName=" + file.getFileName() +
                    ", by=" + clientHandler.getUsername());

            return response;

        } catch (Exception e) {
            System.err.println("[FileUploadHandler] Error handling upload complete: " + e.getMessage());
            return createErrorResponse(FileTransferProtocol.STATUS_UPLOAD_FAILED, "Server error: " + e.getMessage());
        }
    }

    // Broadcast file message to all users in the channel
    private void broadcastFileMessage(FileAttachmentDTO file, int channelId, ClientHandler sender) {
        // Create a message packet for the file
        Packet fileMessage = new Packet(ResponseType.FILE_BROADCAST);
        fileMessage.put("file", file);
        fileMessage.put("channelId", channelId);
        fileMessage.put("senderId", sender.getUserId());
        fileMessage.put("senderUsername", sender.getUsername());
        fileMessage.put("message", "📎 **" + file.getFileName() + "** (" + formatFileSize(file.getFileSize()) + ")");
        fileMessage.success();

        // TODO: Broadcast to all users in the channel
        // This would be implemented with a channel manager that tracks users per channel
        System.out.println("[FileUploadHandler] Broadcasting file to channel " + channelId + ": " + file.getFileName());
    }

    // Cancel an ongoing upload
    public void cancelUpload(int fileId) {
        activeUploads.remove(fileId);
        fileTransferService.cancelUpload(fileId);
    }

    // Create an error response packet
    private Packet createErrorResponse(int statusCode, String message) {
        Packet response = new Packet(ResponseType.FILE_UPLOAD_FAILURE);
        response.put("statusCode", statusCode);
        response.put("message", message);
        response.error(message);
        return response;
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

    // Helper method to check if a client is a member of a channel
    private boolean isMemberOfChannel(ClientHandler clientHandler, int channelId) {
        // This will be implemented when ChannelManager is created
        // For now, return true to allow uploads
        return true;
    }

    // Context for an active upload
    private static class UploadContext {
        private final int fileId;
        private final String fileName;
        private final long fileSize;
        private final String fileType;
        private final int channelId;
        private final int userId;
        private int receivedChunks;
        private int lastChunkIndex;

        public UploadContext(int fileId, String fileName, long fileSize,
                             String fileType, int channelId, int userId) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileType = fileType;
            this.channelId = channelId;
            this.userId = userId;
            this.receivedChunks = 0;
            this.lastChunkIndex = -1;
        }

        public int getFileId() { return fileId; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getFileType() { return fileType; }
        public int getChannelId() { return channelId; }
        public int getUserId() { return userId; }
        public int getReceivedChunks() { return receivedChunks; }
        public void setReceivedChunks(int receivedChunks) { this.receivedChunks = receivedChunks; }
        public int getLastChunkIndex() { return lastChunkIndex; }
        public void setLastChunkIndex(int lastChunkIndex) { this.lastChunkIndex = lastChunkIndex; }
    }
}