package voxlink.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1l;

    // Core message information
    private int id;
    private String content;
    private int channelId;
    private String channelName;
    private int workspaceId;

    // Sender information
    private int senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;

    // Timestamps
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
    private LocalDateTime deletedAt;

    // Message metadata
    private MessageType type;
    private MessageStatus status;

    // File attachment
    private List<FileAttachmentDTO> attachments;

    // Reply functionality
    private Integer replyToMessageId;
    private String replyToMessageContent;

    // Reads
    private List<Integer> readByUsersId;

    // Default constructor
    public MessageDTO() {
        this.type = MessageType.TEXT;
        this.status = MessageStatus.SENT;
        this.attachments = new ArrayList<>();
        this.readByUsersId = new ArrayList<>();
    }

    // Constructor for creating basic text message
    public MessageDTO(int id, String content, int channelId, int senderId, String senderUsername) {
        this.id = id;
        this.content = content;
        this.channelId = channelId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.sentAt = LocalDateTime.now();
    }

    // Getters and Setters


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
        this.workspaceId = workspaceId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public void setSenderDisplayName(String senderDisplayName) {
        this.senderDisplayName = senderDisplayName;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public List<FileAttachmentDTO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<FileAttachmentDTO> attachments) {
        this.attachments = attachments;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(Integer replayToMessageId) {
        this.replyToMessageId = replayToMessageId;
    }

    public String getReplyToMessageContent() {
        return replyToMessageContent;
    }

    public void setReplyToMessageContent(String replayToMessageContent) {
        this.replyToMessageContent = replayToMessageContent;
    }

    public List<Integer> getReadByUsersId() {
        return readByUsersId;
    }

    public void setReadByUsersId(List<Integer> readByUsersId) {
        this.readByUsersId = readByUsersId;
    }

    // Add a file attachment to the message
    public void addAttachment(FileAttachmentDTO attachment) {
        if(this.attachments == null) this.attachments = new ArrayList<>();

        this.attachments.add(attachment);
    }

    // Check if message has been edited before
    public boolean isEdited() {
        return this.editedAt != null;
    }

    // Check if message has been deleted
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // Check if message has attachments
    public boolean hasAttachments() {
        return this.attachments != null && !this.attachments.isEmpty();
    }

    // Check if message is a reply to another message
    public boolean isReply() {
        return this.replyToMessageContent != null && this.replyToMessageId > 0;
    }

    // Mark message as read by user
    public void markAsReadByUser(int userId) {
        if(this.readByUsersId == null) this.readByUsersId = new ArrayList<>();

        if(!this.readByUsersId.contains(userId)) this.readByUsersId.add(userId);
    }

    // Check if a user has read the message
    public boolean isReadByUser(int userId) {
        return this.readByUsersId != null && this.readByUsersId.contains(userId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MessageDTO that = (MessageDTO) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "MessageDTO{" +
                "id=" + id +
                ", sender='" + senderUsername + '\'' +
                ", channelId=" + channelId +
                ", content='" + (content != null ? content.substring(0, Math.min(20, content.length())) : "null") + "'" +
                ", type=" + type +
                '}';
    }
}
