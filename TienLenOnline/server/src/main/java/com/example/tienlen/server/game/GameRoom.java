package com.example.tienlen.server.game;

import com.example.tienlen.common.model.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameRoom {
    private final String roomId;
    private final List<Player> players = new ArrayList<>();

    public GameRoom(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public void addPlayer(Player player) {
        players.add(player);
    }
}
