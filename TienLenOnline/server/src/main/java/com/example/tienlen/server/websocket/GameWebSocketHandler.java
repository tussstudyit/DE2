package com.example.tienlen.server.websocket;

import com.example.tienlen.server.service.GameService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final GameService gameService;

    public GameWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        gameService.registerSession(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String response = gameService.handleRawMessage(session.getId(), message.getPayload());
        session.sendMessage(new TextMessage(response));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        gameService.removeSession(session.getId());
    }
}
