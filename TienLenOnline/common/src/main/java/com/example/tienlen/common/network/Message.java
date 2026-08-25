package com.example.tienlen.common.network;

import java.time.Instant;

public class Message {
    private MessageType type;
    private String roomId;
    private String playerId;
    private String payload;
    private Instant createdAt = Instant.now();

    public Message() {
    }

    public Message(MessageType type) {
        this.type = type;
    }

    public Message(MessageType type, String roomId, String playerId, String payload) {
        this.type = type;
        this.roomId = roomId;
        this.playerId = playerId;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
