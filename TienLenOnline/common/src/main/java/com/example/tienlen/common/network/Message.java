package com.example.tienlen.common.network;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.model.Player;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId = java.util.UUID.randomUUID().toString();
    private MessageType type;
    private String roomId;
    private String playerId;
    private String playerName;
    private String currentTurnPlayerId;
    private String currentTurnPlayerName;
    private String payload;
    private List<Card> cards = new ArrayList<>();
    private List<Player> players = new ArrayList<>();
    private String hostPlayerId;
    private int maxPlayers;
    private int turnTimeoutSeconds = 60;
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

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getCurrentTurnPlayerId() {
        return currentTurnPlayerId;
    }

    public void setCurrentTurnPlayerId(String currentTurnPlayerId) {
        this.currentTurnPlayerId = currentTurnPlayerId;
    }

    public String getCurrentTurnPlayerName() {
        return currentTurnPlayerName;
    }

    public void setCurrentTurnPlayerName(String currentTurnPlayerName) {
        this.currentTurnPlayerName = currentTurnPlayerName;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards != null ? cards : new ArrayList<>();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players != null ? players : new ArrayList<>();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }

    public void setHostPlayerId(String hostPlayerId) {
        this.hostPlayerId = hostPlayerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public int getTurnTimeoutSeconds() {
        return turnTimeoutSeconds;
    }

    public void setTurnTimeoutSeconds(int turnTimeoutSeconds) {
        this.turnTimeoutSeconds = turnTimeoutSeconds;
    }
}
