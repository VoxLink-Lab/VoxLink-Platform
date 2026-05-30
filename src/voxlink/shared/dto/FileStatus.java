package voxlink.shared.dto;

import java.io.Serializable;

/**
 * Represents the current state of a file in the system.
 */
public enum FileStatus implements Serializable {

    UPLOADING("uploading", "⏳", "Uploading..."),
    UPLOADING_CHUNK("uploading_chunk", "📤", "Uploading..."),
    AVAILABLE("available", "", "Available"),
    UPLOAD_FAILED("upload_failed", "", "Upload failed"),
    DELETED("deleted", "🗑️", "Deleted"),
    PROCESSING("processing", "🔄", "Processing..."),
    DOWNLOADING("downloading", "⬇️", "Downloading..."),
    DOWNLOAD_COMPLETE("download_complete", "", "Download complete");

    private final String code;
    private final String icon;
    private final String displayText;

    FileStatus(String code, String icon, String displayText) {
        this.code = code;
        this.icon = icon;
        this.displayText = displayText;
    }

    public String getCode() {
        return code;
    }

    public String getIcon() {
        return icon;
    }

    public String getDisplayText() {
        return displayText;
    }

    public static FileStatus fromCode(String code) {
        for (FileStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return AVAILABLE;
    }

    // Check if file is available for download
    public boolean isAvailable() {
        return this == AVAILABLE;
    }

    // Check if the file in an error state
    public boolean isError() {
        return this == UPLOAD_FAILED;
    }

    // check if file is currently being transferred
    public boolean isTransferring() {
        return this == UPLOADING || this == UPLOADING_CHUNK || this == DOWNLOADING;
    }

    @Override
    public String toString() {
        return displayText;
    }
}