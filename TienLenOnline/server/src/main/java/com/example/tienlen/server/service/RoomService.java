package com.example.tienlen.server.service;

import com.example.tienlen.server.game.GameRoom;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RoomService {
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public GameRoom createRoom(String roomId) {
        GameRoom room = new GameRoom(roomId);
        rooms.put(roomId, room);
        return room;
    }

    public GameRoom findRoom(String roomId) {
        return rooms.get(roomId);
    }

    public Collection<GameRoom> getRooms() {
        return rooms.values();
    }
}
