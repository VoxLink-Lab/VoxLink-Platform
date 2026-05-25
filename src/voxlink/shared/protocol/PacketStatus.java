package voxlink.shared.protocol;

import java.io.Serializable;

/**
 * Enum representing status of a packet during it's life cycle
 */
public enum PacketStatus implements Serializable {

    PENDING,
    SENT,
    RECEIVED,
    SUCCESS,
    ERROR,
    REQUIRES_ACK,
    ACKNOWLEDGED,
    TIMEOUT,
    REJECTED,
    PROCESSING,
    CANCELLED,
    DUPLICATE,
    INVALID
}