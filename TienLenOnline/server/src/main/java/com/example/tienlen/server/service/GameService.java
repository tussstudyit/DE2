package com.example.tienlen.server.service;

import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.network.MessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final ObjectMapper objectMapper;
    private final Set<String> connectedSessionIds = ConcurrentHashMap.newKeySet();

    public GameService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerSession(String sessionId) {
        connectedSessionIds.add(sessionId);
    }

    public void removeSession(String sessionId) {
        connectedSessionIds.remove(sessionId);
    }

    public String handleRawMessage(String sessionId, String rawMessage) {
        try {
            Message incomingMessage = objectMapper.readValue(rawMessage, Message.class);
            Message response = new Message(MessageType.GAME_STATE);
            response.setRoomId(incomingMessage.getRoomId());
            response.setPlayerId(incomingMessage.getPlayerId());
            response.setPayload("Server đã nhận message: " + incomingMessage.getType());
            return toJson(response);
        } catch (JsonProcessingException exception) {
            Message response = new Message(MessageType.GAME_STATE);
            response.setPlayerId(sessionId);
            response.setPayload("Server đã nhận text message.");
            return toJson(response);
        }
    }

    public int getConnectedSessionCount() {
        return connectedSessionIds.size();
    }

    private String toJson(Message message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            return "{\"type\":\"GAME_STATE\",\"payload\":\"Server response error\"}";
        }
    }
}
