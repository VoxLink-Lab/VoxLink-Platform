package voxlink.server.src.main.network;

import voxlink.server.src.main.service.FileTransferService;
import voxlink.shared.dto.FileAttachmentDTO;
import voxlink.shared.protocol.FileTransferProtocol;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileDownloadHandler processes file download requests from clients.
 */
public class FileDownloadHandler {

    private final FileTransferService fileTransferService;
    private final Map<Integer, DownloadContext> activeDownloads;

    public FileDownloadHandler() {
        this.fileTransferService = new FileTransferService();
        this.activeDownloads = new ConcurrentHashMap<>();
    }

    // Process a file download packet
    public Packet processPacket(Packet packet, ClientHandler clientHandler) {
        RequestType type = packet.getRequestType();

        if (type == RequestType.FILE_DOWNLOAD) {
            return handleDownloadRequest(packet, clientHandler);
        }

        return null;
    }

    // Handle download request
    private Packet handleDownloadRequest(Packet packet, ClientHandler clientHandler) {
        try {
            // Extract download request data
            Integer fileId = (Integer) packet.get("fileId");
            Integer channelId = (Integer) packet.get("channelId");

            if (fileId == null || channelId == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_DOWNLOAD_FAILED, "Missing fileId or channelId");
            }

            // Check if user has permission to download from this channel
            if (!clientHandler.isMemberOfChannel(channelId)) {
                return createErrorResponse(FileTransferProtocol.STATUS_PERMISSION_DENIED, "Not a member of this channel");
            }

            // Get file metadata
            FileAttachmentDTO file = fileTransferService.getFileAttachment(fileId);
            if (file == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_FILE_NOT_FOUND, "File not found");
            }

            // Get file input stream
            FileInputStream fileStream = fileTransferService.getFileStream(fileId);
            if (fileStream == null) {
                return createErrorResponse(FileTransferProtocol.STATUS_FILE_NOT_FOUND, "File not found on disk");
            }

            // Calculate total chunks
            int totalChunks = FileTransferProtocol.calculateChunkCount(file.getFileSize());

            // Create download context
            DownloadContext context = new DownloadContext(
                    fileId,
                    channelId,
                    file.getFileName(),
                    file.getFileSize(),
                    totalChunks,
                    fileStream,
                    clientHandler
            );
            activeDownloads.put(fileId, context);

            // Read first chunk
            byte[] buffer = new byte[FileTransferProtocol.CHUNK_SIZE];
            int bytesRead = fileStream.read(buffer);
            boolean isLast = bytesRead < FileTransferProtocol.CHUNK_SIZE;

            if (bytesRead == -1) {
                fileStream.close();
                activeDownloads.remove(fileId);
                return createErrorResponse(FileTransferProtocol.STATUS_DOWNLOAD_FAILED, "File is empty");
            }

            // Trim buffer if last chunk
            byte[] chunkData = buffer;
            if (bytesRead < FileTransferProtocol.CHUNK_SIZE) {
                chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
            }

            // Update context progress
            context.setBytesSent(bytesRead);
            context.setLastChunkSent(0);

            // Increment download count in database
            fileTransferService.incrementDownloadCount(fileId);

            // Create response with first chunk
            FileTransferProtocol.DownloadChunkData chunkDataObj = new FileTransferProtocol.DownloadChunkData(
                    fileId, 0, totalChunks, chunkData, isLast, file.getFileName(), file.getFileSize()
            );

            Packet response = new Packet(ResponseType.FILE_DOWNLOAD_DATA);
            response.put("chunkData", chunkDataObj);
            response.success();

            System.out.println("[FileDownloadHandler] Download started: fileId=" + fileId +
                    ", fileName=" + file.getFileName() +
                    ", size=" + file.getFileSize() +
                    ", totalChunks=" + totalChunks +
                    ", by=" + clientHandler.getUsername());

            return response;

        } catch (Exception e) {
            System.err.println("[FileDownloadHandler] Error handling download request: " + e.getMessage());
            return createErrorResponse(FileTransferProtocol.STATUS_DOWNLOAD_FAILED, "Server error: " + e.getMessage());
        }
    }

    // Send the next chunk for an active download
    public Packet sendNextChunk(int fileId, int chunkIndex) {
        DownloadContext context = activeDownloads.get(fileId);
        if (context == null) {
            return null;
        }

        try {
            // Read next chunk
            byte[] buffer = new byte[FileTransferProtocol.CHUNK_SIZE];
            int bytesRead = context.getFileStream().read(buffer);
            boolean isLast = false;

            if (bytesRead == -1) {
                // Download complete
                completeDownload(fileId);
                return null;
            }

            // Trim buffer if needed
            byte[] chunkData = buffer;
            if (bytesRead < FileTransferProtocol.CHUNK_SIZE) {
                chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                isLast = true;
            }

            // Update context progress
            context.setBytesSent(context.getBytesSent() + bytesRead);
            context.setLastChunkSent(chunkIndex);

            // Create chunk data
            FileTransferProtocol.DownloadChunkData chunkDataObj = new FileTransferProtocol.DownloadChunkData(
                    fileId, chunkIndex, context.getTotalChunks(), chunkData, isLast,
                    context.getFileName(), context.getFileSize()
            );

            Packet response = new Packet(ResponseType.FILE_DOWNLOAD_DATA);
            response.put("chunkData", chunkDataObj);
            response.success();

            // Log progress
            if (chunkIndex % 10 == 0 || isLast) {
                int progress = (int) ((context.getBytesSent() * 100.0) / context.getFileSize());
                System.out.println("[FileDownloadHandler] Download progress: fileId=" + fileId +
                        ", progress=" + progress + "% (chunk " + chunkIndex +
                        "/" + context.getTotalChunks() + ")");
            }

            return response;

        } catch (IOException e) {
            System.err.println("[FileDownloadHandler] Error reading chunk: " + e.getMessage());
            completeDownload(fileId);
            return null;
        }
    }

    // Complete an active download and clean up resources
    public void completeDownload(int fileId) {
        DownloadContext context = activeDownloads.remove(fileId);
        if (context != null) {
            try {
                context.getFileStream().close();
                System.out.println("[FileDownloadHandler] Download completed: fileId=" + fileId +
                        ", fileName=" + context.getFileName() +
                        ", totalBytes=" + context.getBytesSent());
            } catch (IOException e) {
                System.err.println("[FileDownloadHandler] Error closing file stream: " + e.getMessage());
            }
        }
    }

    // Cancel an active download
    public void cancelDownload(int fileId) {
        DownloadContext context = activeDownloads.remove(fileId);
        if (context != null) {
            try {
                context.getFileStream().close();
                System.out.println("[FileDownloadHandler] Download cancelled: fileId=" + fileId);
            } catch (IOException e) {
                System.err.println("[FileDownloadHandler] Error closing file stream: " + e.getMessage());
            }
        }
    }

    // Get file attachment by ID
    public FileAttachmentDTO getFileAttachment(int fileId) {
        return fileTransferService.getFileAttachment(fileId);
    }

    // Check if a download is active
    public boolean isDownloadActive(int fileId) {
        return activeDownloads.containsKey(fileId);
    }

    // Create an error response packet
    private Packet createErrorResponse(int statusCode, String message) {
        Packet response = new Packet(ResponseType.FILE_DOWNLOAD_FAILURE);
        response.put("statusCode", statusCode);
        response.put("message", message);
        response.error(message);
        return response;
    }

    // Context for an active download
    private static class DownloadContext {
        private final int fileId;
        private final int channelId;
        private final String fileName;
        private final long fileSize;
        private final int totalChunks;
        private final FileInputStream fileStream;
        private final ClientHandler clientHandler;
        private long bytesSent;
        private int lastChunkSent;

        public DownloadContext(int fileId, int channelId, String fileName, long fileSize,
                               int totalChunks, FileInputStream fileStream, ClientHandler clientHandler) {
            this.fileId = fileId;
            this.channelId = channelId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.totalChunks = totalChunks;
            this.fileStream = fileStream;
            this.clientHandler = clientHandler;
            this.bytesSent = 0;
            this.lastChunkSent = -1;
        }

        public int getFileId() { return fileId; }
        public int getChannelId() { return channelId; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public int getTotalChunks() { return totalChunks; }
        public FileInputStream getFileStream() { return fileStream; }
        public ClientHandler getClientHandler() { return clientHandler; }
        public long getBytesSent() { return bytesSent; }
        public void setBytesSent(long bytesSent) { this.bytesSent = bytesSent; }
        public int getLastChunkSent() { return lastChunkSent; }
        public void setLastChunkSent(int lastChunkSent) { this.lastChunkSent = lastChunkSent; }
    }
}