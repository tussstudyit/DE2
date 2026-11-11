package com.example.tienlen.server.rmi;

import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.rmi.ITienLenClientCallback;
import com.example.tienlen.server.GameRoom;
import com.example.tienlen.server.IPlayerSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Phiên người chơi kết nối qua Java RMI.
 * Hỗ trợ đồng thời cả Remote Callback lẫn hàng đợi Event Queue (chống chặn tường lửa LAN).
 */
public class RmiPlayerSession implements IPlayerSession {
    private final String playerId;
    private String playerName;
    private ITienLenClientCallback callback;
    private GameRoom currentRoom;
    private final Queue<Message> pendingEvents = new ConcurrentLinkedQueue<>();

    public RmiPlayerSession(String playerId, String playerName, ITienLenClientCallback callback) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.callback = callback;
    }

    @Override
    public String getPlayerId() {
        return playerId;
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    @Override
    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    @Override
    public void setCurrentRoom(GameRoom currentRoom) {
        this.currentRoom = currentRoom;
    }

    public void setCallback(ITienLenClientCallback callback) {
        this.callback = callback;
    }

    @Override
    public void sendMessage(Message message) {
        // Luôn đưa vào hàng đợi để hỗ trợ trường hợp tường lửa LAN chặn RMI callback
        pendingEvents.add(message);

        // Gọi RMI Callback trực tiếp tới Client nếu có
        if (callback != null) {
            try {
                callback.onMessage(message);
            } catch (Exception ignored) {
                // Nếu RMI callback không đến được máy client (do tường lửa client chặn port vào),
                // client sẽ tự động lấy message qua pollEvents()
            }
        }
    }

    public List<Message> drainEvents() {
        List<Message> events = new ArrayList<>();
        Message m;
        while ((m = pendingEvents.poll()) != null) {
            events.add(m);
        }
        return events;
    }
}
