package voxlink.server.src.main.model;

import java.sql.Timestamp;

public class Attachment {
    private int attachmentId;
    private int messageId;
    private String filename;
    private String filepath;
    private double filesize;
    private Timestamp uploadedAt;

    public Attachment() {

    }

    public Attachment(int attachmentId, int messageId, String filename, String filepath, double filesize, Timestamp uploadedAt) {
        this.attachmentId = attachmentId;
        this.messageId = messageId;
        this.filename = filename;
        this.filepath = filepath;
        this.filesize = filesize;
        this.uploadedAt = uploadedAt;
    }

    public int getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(int attachmentId) {
        this.attachmentId = attachmentId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public double getFilesize() {
        return filesize;
    }

    public void setFilesize(double filesize) {
        this.filesize = filesize;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
