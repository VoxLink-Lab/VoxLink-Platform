package voxlink.shared.protocol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the fundamental unit of communication between the client and the server
 */
public class Packet implements Serializable {

    private static final long serialVersionUID = 1l;

    // Core packet identifiers
    private String packetId;
    private RequestType requestType;
    private ResponseType responseType;
    private PacketStatus status;
    private long timestamp;

    // Data / payload
    private Map<String, Object> data;
    private String errorMessage;

    // Connection metadata
    private String authToken;
    private int userId;

    // Constructor for client request
    public Packet(RequestType requestType) {
        this.packetId = UUID.randomUUID().toString();
        this.requestType = requestType;
        this.responseType = null;
        this.status = PacketStatus.PENDING;
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
    }

    // Constructor for server response
    public Packet(ResponseType responseType) {
        this.packetId = UUID.randomUUID().toString();
        this.requestType = null;
        this.responseType = responseType;
        this.status = PacketStatus.PENDING;
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
    }

    // Getters and Setters

    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public PacketStatus getStatus() {
        return status;
    }

    public void setStatus(PacketStatus status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // Add key - value to packet's data
    public Packet put(String key, Object value) {
       this.data.put(key, value);
       return this;
    }

    // Retrieve value from the key
    public Object get(String key) {
        return this.data.get(key);
    }

    // Check if packet contains specific key
    public boolean containsKey(String key) {
        return this.data.containsKey(key);
    }

    // Mark packet as successful
    public void success() {
        this.status = PacketStatus.SUCCESS;
    }

    // Mark packet as failed
    public void error(String message) {
        this.status = PacketStatus.ERROR;
        this.errorMessage = message;
    }

    // Check if this packet is success
    public boolean isSuccess() {
        return this.status == PacketStatus.SUCCESS;
    }

    // Check if this packet is error
    public boolean isError() {
        return this.status == PacketStatus.ERROR;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "packetId='" + packetId + '\'' +
                ", requestType=" + requestType +
                ", responseType=" + responseType +
                ", status=" + status +
                ", timestamp=" + timestamp +
                ", userId=" + userId +
                ", data=" + data.keySet() +
                '}';
    }


}
