package com.example.tienlen.server;

import com.example.tienlen.common.network.Message;

/**
 * Interface đại diện cho phiên kết nối của một người chơi trên Server.
 * Cho phép GameRoom tương tác đồng nhất dù người chơi kết nối qua Java RMI hay TCP Socket.
 */
public interface IPlayerSession {
    String getPlayerId();
    String getPlayerName();
    void setPlayerName(String playerName);
    GameRoom getCurrentRoom();
    void setCurrentRoom(GameRoom currentRoom);
    void sendMessage(Message message);
}
