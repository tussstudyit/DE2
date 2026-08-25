package com.example.tienlen.server.model;

public class ServerPlayerSession {
    private String sessionId;
    private String playerId;
    private String roomId;

    public ServerPlayerSession() {
    }

    public ServerPlayerSession(String sessionId, String playerId, String roomId) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.roomId = roomId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
