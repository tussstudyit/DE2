package com.example.tienlen.server.rmi;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.network.MessageType;
import com.example.tienlen.common.rmi.ITienLenClientCallback;
import com.example.tienlen.common.rmi.ITienLenService;
import com.example.tienlen.server.GameRoom;
import com.example.tienlen.server.RoomManager;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hiện thực Remote Service RMI của trò chơi Tiến Lên Miền Nam.
 * Xử lý các lệnh gọi từ xa (Remote Method Invocation) từ các Client trong mạng.
 */
public class TienLenServiceImpl extends UnicastRemoteObject implements ITienLenService {
    private final RoomManager roomManager;
    private final Map<String, RmiPlayerSession> sessions = new ConcurrentHashMap<>();

    public TienLenServiceImpl(RoomManager roomManager) throws RemoteException {
        super(0);
        this.roomManager = roomManager;
    }

    @Override
    public synchronized String registerPlayer(String playerName, ITienLenClientCallback callback) throws RemoteException {
        String playerId = UUID.randomUUID().toString().substring(0, 8);
        String name = (playerName != null && !playerName.isBlank()) ? playerName.trim() : "Player_" + playerId;
        RmiPlayerSession session = new RmiPlayerSession(playerId, name, callback);
        sessions.put(playerId, session);
        System.out.println("[RMI SERVER] Client kết nối: " + name + " (ID: " + playerId + ")");

        Message ack = new Message(MessageType.CONNECT_ACK);
        ack.setPlayerId(playerId);
        ack.setPlayerName(name);
        ack.setPayload("Kết nối thành công tới Tiến Lên RMI Server!");
        session.sendMessage(ack);

        return playerId;
    }

    @Override
    public synchronized void createRoom(String roomId, String playerId, int maxPlayers) throws RemoteException {
        RmiPlayerSession session = sessions.get(playerId);
        if (session == null) return;

        if (session.getCurrentRoom() != null) {
            session.getCurrentRoom().removePlayer(session);
            session.setCurrentRoom(null);
        }

        int targetMax = Math.max(2, Math.min(maxPlayers > 0 ? maxPlayers : 4, 4));
        if (roomId != null && !roomId.isBlank() && roomManager.hasRoom(roomId)) {
            Message err = new Message(MessageType.ERROR_MSG);
            err.setPayload("Phòng mã " + roomId + " đã tồn tại, vui lòng chọn mã khác!");
            session.sendMessage(err);
            return;
        }

        GameRoom room = roomManager.createRoom(roomId, targetMax);
        session.setCurrentRoom(room);
        room.addPlayer(session);
    }

    @Override
    public synchronized void joinRoom(String roomId, String playerId, int maxPlayers) throws RemoteException {
        RmiPlayerSession session = sessions.get(playerId);
        if (session == null) return;

        if (session.getCurrentRoom() != null) {
            session.getCurrentRoom().removePlayer(session);
            session.setCurrentRoom(null);
        }

        int targetMax = Math.max(2, Math.min(maxPlayers > 0 ? maxPlayers : 4, 4));
        GameRoom targetRoom;
        if (roomId == null || roomId.isBlank()) {
            targetRoom = roomManager.findAvailableRoom(targetMax);
            if (targetRoom == null) {
                targetRoom = roomManager.createRoom(null, targetMax);
            }
        } else {
            targetRoom = roomManager.findRoom(roomId);
            if (targetRoom == null) {
                Message err = new Message(MessageType.ERROR_MSG);
                err.setPayload("Không tìm thấy phòng chơi có mã: " + roomId);
                session.sendMessage(err);
                return;
            }
        }

        session.setCurrentRoom(targetRoom);
        targetRoom.addPlayer(session);
    }

    @Override
    public synchronized void startGame(String roomId, String playerId) throws RemoteException {
        RmiPlayerSession session = sessions.get(playerId);
        if (session != null && session.getCurrentRoom() != null) {
            session.getCurrentRoom().requestStartGame(session);
        }
    }

    @Override
    public synchronized void playCards(String roomId, String playerId, List<Card> cards) throws RemoteException {
        RmiPlayerSession session = sessions.get(playerId);
        if (session != null && session.getCurrentRoom() != null) {
            session.getCurrentRoom().handlePlayCards(session, cards);
        }
    }

    @Override
    public synchronized void passTurn(String roomId, String playerId) throws RemoteException {
        RmiPlayerSession session = sessions.get(playerId);
        if (session != null && session.getCurrentRoom() != null) {
            session.getCurrentRoom().handlePassTurn(session);
        }
    }

    @Override
    public synchronized void leaveRoom(String roomId, String playerId) throws RemoteException {
        RmiPlayerSession session = sessions.get(playerId);
        if (session != null && session.getCurrentRoom() != null) {
            session.getCurrentRoom().removePlayer(session);
            session.setCurrentRoom(null);
        }
    }

    @Override
    public synchronized void disconnect(String playerId) throws RemoteException {
        RmiPlayerSession session = sessions.remove(playerId);
        if (session != null && session.getCurrentRoom() != null) {
            session.getCurrentRoom().removePlayer(session);
            session.setCurrentRoom(null);
            System.out.println("[RMI SERVER] Client ngắt kết nối: " + session.getPlayerName() + " (" + playerId + ")");
        }
    }

    @Override
    public List<Message> pollEvents(String playerId) throws RemoteException {
        RmiPlayerSession session = sessions.get(playerId);
        if (session != null) {
            return session.drainEvents();
        }
        return Collections.emptyList();
    }
}
