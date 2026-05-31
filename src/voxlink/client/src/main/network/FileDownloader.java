package voxlink.client.src.main.network;

import voxlink.client.src.main.state.UserStore;
import voxlink.shared.protocol.FileTransferProtocol;
import voxlink.shared.protocol.Packet;
import voxlink.shared.protocol.RequestType;
import voxlink.shared.protocol.ResponseType;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FileDownloader handles file downloads from server to client.
 */
public class FileDownloader {

    private final ServerConnection connection;
    private final UserStore userStore;
    private final AtomicBoolean isDownloading;

    private DownloadProgressListener progressListener;
    private int currentFileId;
    private String currentFileName;
    private long currentFileSize;
    private long bytesDownloaded;
    private int chunksReceived;
    private FileOutputStream fileOutputStream;
    private String saveDirectory;

    // Track active downloads: fileId -> DownloadContext
    private final ConcurrentHashMap<Integer, DownloadContext> activeDownloads;

    public FileDownloader() {
        this.connection = ServerConnection.getInstance();
        this.userStore = UserStore.getInstance();
        this.isDownloading = new AtomicBoolean(false);
        this.activeDownloads = new ConcurrentHashMap<>();
        this.saveDirectory = System.getProperty("user.home") + "/Downloads/VoxLink/";
    }

    // Set progress listener for download callbacks
    public void setProgressListener(DownloadProgressListener listener) {
        this.progressListener = listener;
    }

    // Set save directory for downloaded files
    public void setSaveDirectory(String directory) {
        this.saveDirectory = directory;
        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Download a file from a channel
    public CompletableFuture<File> downloadFile(int fileId, int channelId, String fileName, long fileSize) {
        CompletableFuture<File> future = new CompletableFuture<>();

        if (isDownloading.get()) {
            future.completeExceptionally(new IllegalStateException("Another download is in progress"));
            return future;
        }

        isDownloading.set(true);
        this.currentFileId = fileId;
        this.currentFileName = fileName;
        this.currentFileSize = fileSize;
        this.bytesDownloaded = 0;
        this.chunksReceived = 0;

        // Create download directory if it doesn't exist
        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Create output file
        String filePath = saveDirectory + fileName;
        File outputFile = new File(filePath);

        // Handle duplicate file names
        int counter = 1;
        while (outputFile.exists()) {
            String nameWithoutExt = fileName;
            String extension = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                nameWithoutExt = fileName.substring(0, dotIndex);
                extension = fileName.substring(dotIndex);
            }
            String newFileName = nameWithoutExt + " (" + counter + ")" + extension;
            filePath = saveDirectory + newFileName;
            outputFile = new File(filePath);
            counter++;
        }

        try {
            fileOutputStream = new FileOutputStream(outputFile);
        } catch (IOException e) {
            isDownloading.set(false);
            future.completeExceptionally(new IOException("Failed to create output file: " + e.getMessage()));
            return future;
        }

        // Start download in background thread
        File finalOutputFile = outputFile;
        new Thread(() -> {
            try {
                File result = performDownload(fileId, channelId, finalOutputFile);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                isDownloading.set(false);
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    System.err.println("[FileDownloader] Error closing file: " + e.getMessage());
                }
            }
        }).start();

        return future;
    }

    // Perform the actual download process
    private File performDownload(int fileId, int channelId, File outputFile) throws Exception {
        // Create download context
        DownloadContext context = new DownloadContext(fileId, outputFile);
        activeDownloads.put(fileId, context);

        // Send download request
        Packet requestPacket = new Packet(RequestType.FILE_DOWNLOAD);
        requestPacket.setAuthToken(userStore.getAuthToken());
        requestPacket.setUserId(userStore.getUserId());
        requestPacket.put("fileId", fileId);
        requestPacket.put("channelId", channelId);

        connection.sendPacket(requestPacket);

        // Register listener for download chunks
        CompletableFuture<Boolean> completionFuture = new CompletableFuture<>();

        ServerConnection.PacketListener chunkListener = new ServerConnection.PacketListener() {
            @Override
            public void onPacketReceived(Packet packet) {
                if (packet.getResponseType() == ResponseType.FILE_DOWNLOAD_DATA) {
                    handleChunkData(packet, fileId, completionFuture);
                } else if (packet.getResponseType() == ResponseType.FILE_DOWNLOAD_FAILURE) {
                    String error = packet.getErrorMessage();
                    completionFuture.completeExceptionally(new IOException(error != null ? error : "Download failed"));
                }
            }
        };

        connection.addPacketListener(chunkListener);

        try {
            // Wait for download to complete
            completionFuture.get();
        } finally {
            connection.removePacketListener(chunkListener);
            activeDownloads.remove(fileId);
        }

        if (progressListener != null) {
            progressListener.onComplete(outputFile);
        }

        System.out.println("[FileDownloader] Download completed: " + currentFileName);
        return outputFile;
    }

    // Handle incoming chunk data
    private void handleChunkData(Packet packet, int fileId, CompletableFuture<Boolean> completionFuture) {
        try {
            FileTransferProtocol.DownloadChunkData chunkData = (FileTransferProtocol.DownloadChunkData) packet.get("chunkData");

            if (chunkData == null) {
                completionFuture.completeExceptionally(new IOException("Invalid chunk data"));
                return;
            }

            if (chunkData.getFileId() != fileId) {
                // Wrong file, ignore
                return;
            }

            // Write chunk to file
            synchronized (fileOutputStream) {
                fileOutputStream.write(chunkData.getData());
                fileOutputStream.flush();
            }

            // Update progress
            bytesDownloaded += chunkData.getData().length;
            chunksReceived++;

            int progressPercent = (int) ((bytesDownloaded * 100) / currentFileSize);

            if (progressListener != null) {
                progressListener.onProgress(progressPercent, bytesDownloaded, currentFileSize);
            }

            // Check if this was the last chunk
            if (chunkData.isLast()) {
                // Verify file size
                if (bytesDownloaded != currentFileSize) {
                    completionFuture.completeExceptionally(new IOException(
                            "File size mismatch. Expected: " + currentFileSize + ", Downloaded: " + bytesDownloaded
                    ));
                } else {
                    completionFuture.complete(true);
                }
            }

            // Log progress
            if (chunksReceived % 10 == 0 || chunkData.isLast()) {
                System.out.println("[FileDownloader] Download progress: " + progressPercent + "% (" +
                        chunksReceived + "/" + chunkData.getTotalChunks() + " chunks)");
            }

        } catch (IOException e) {
            completionFuture.completeExceptionally(new IOException("Failed to write chunk: " + e.getMessage()));
        }
    }

    // Cancel ongoing download
    public void cancelDownload() {
        if (isDownloading.get() && currentFileId > 0) {
            DownloadContext context = activeDownloads.remove(currentFileId);
            if (context != null && context.getOutputFile() != null) {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    // Delete partially downloaded file
                    context.getOutputFile().delete();
                } catch (IOException e) {
                    System.err.println("[FileDownloader] Error cancelling download: " + e.getMessage());
                }
            }

            isDownloading.set(false);

            if (progressListener != null) {
                progressListener.onError("Download cancelled by user");
            }

            System.out.println("[FileDownloader] Download cancelled: " + currentFileName);
        }
    }

    // Check if currently downloading
    public boolean isDownloading() {
        return isDownloading.get();
    }

    // Get current download progress percentage
    public int getProgressPercent() {
        if (currentFileSize == 0) return 0;
        return (int) ((bytesDownloaded * 100) / currentFileSize);
    }

    // Get current file name being downloaded
    public String getCurrentFileName() {
        return currentFileName;
    }

    // Get save directory path
    public String getSaveDirectory() {
        return saveDirectory;
    }

    // Open the save directory in file explorer
    public void openSaveDirectory() {
        try {
            File dir = new File(saveDirectory);
            if (dir.exists()) {
                Desktop.getDesktop().open(dir);
            }
        } catch (Exception e) {
            System.err.println("[FileDownloader] Failed to open directory: " + e.getMessage());
        }
    }

    // Download context for tracking active downloads
    private static class DownloadContext {
        private final int fileId;
        private final File outputFile;

        public DownloadContext(int fileId, File outputFile) {
            this.fileId = fileId;
            this.outputFile = outputFile;
        }

        public int getFileId() { return fileId; }
        public File getOutputFile() { return outputFile; }
    }

    // Progress listener interface for download callbacks
    public interface DownloadProgressListener {
        void onProgress(int percent, long bytesDownloaded, long totalBytes);
        void onComplete(File file);
        void onError(String error);
    }
}