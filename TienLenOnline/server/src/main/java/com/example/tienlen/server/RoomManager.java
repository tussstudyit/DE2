package com.example.tienlen.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quản lý danh sách các phòng chơi Tiến Lên trên Server.
 * Sinh mã bàn 6 chữ số tự động và giải phóng phòng khi người chơi rời đi.
 */
public class RoomManager {
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger roomSequence = new AtomicInteger(100000);

    public synchronized GameRoom createRoom(String rawRoomId, int maxPlayers) {
        String roomId;
        if (rawRoomId == null || rawRoomId.isBlank()) {
            roomId = generateNextRoomId();
        } else {
            roomId = rawRoomId.trim().toUpperCase();
        }

        GameRoom room = new GameRoom(roomId, maxPlayers, this);
        rooms.put(roomId, room);
        System.out.println("[SERVER] Đã tạo phòng mới: " + roomId + " (Chế độ tối đa " + maxPlayers + " người)");
        return room;
    }

    public GameRoom findRoom(String roomId) {
        if (roomId == null) return null;
        return rooms.get(roomId.trim().toUpperCase());
    }

    public boolean hasRoom(String roomId) {
        if (roomId == null) return false;
        return rooms.containsKey(roomId.trim().toUpperCase());
    }

    public GameRoom findAvailableRoom(int maxPlayers) {
        for (GameRoom room : rooms.values()) {
            if (!room.isFull() && !room.isPlaying() && room.getMaxPlayers() == maxPlayers) {
                return room;
            }
        }
        return null;
    }

    /**
     * Sinh mã phòng ngẫu nhiên duy nhất gồm 6 chữ số (100001 - 999999).
     */
    public String generateNextRoomId() {
        while (true) {
            int seq = roomSequence.incrementAndGet();
            if (seq > 999999) {
                roomSequence.set(100000);
                seq = roomSequence.incrementAndGet();
            }
            String id = String.format("%06d", seq);
            if (!rooms.containsKey(id)) {
                return id;
            }
        }
    }

    public void removeRoom(String roomId) {
        if (roomId != null) {
            String key = roomId.trim().toUpperCase();
            rooms.remove(key);
            System.out.println("[SERVER] Đã giải tán phòng: " + key);
        }
    }

    public Collection<GameRoom> getAllRooms() {
        return rooms.values();
    }
}
