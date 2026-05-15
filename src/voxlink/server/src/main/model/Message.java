package voxlink.server.src.main.model;

import java.sql.Timestamp;

public class Message {
    private int messageId;
    private int channelId;
    private int senderId;
    private int receiverId;
    private String content;
    private boolean edited;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Message() {
    }

    public Message(int messageId, int channelId, int senderId, int receiverId, String content, boolean edited,
            Timestamp createdAt, Timestamp updatedAt) {
        this.messageId = messageId;
        this.channelId = channelId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.edited = edited;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

}
