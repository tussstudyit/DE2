package com.example.tienlen.server;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.network.JsonUtil;
import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.network.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Quản lý luồng giao tiếp với một Client kết nối qua TCP Socket.
 */
public class ClientHandler implements Runnable, IPlayerSession {
    private final Socket socket;
    private final RoomManager roomManager;
    private PrintWriter out;
    private BufferedReader in;

    private String playerId;
    private String playerName;
    private GameRoom currentRoom;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, RoomManager roomManager) {
        this.socket = socket;
        this.roomManager = roomManager;
        this.playerId = UUID.randomUUID().toString().substring(0, 8);
        this.playerName = "Player_" + playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(GameRoom currentRoom) {
        this.currentRoom = currentRoom;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            System.out.println("[SERVER] Client kết nối từ: " + socket.getRemoteSocketAddress() + " (ID: " + playerId + ")");

            String line;
            while (running && (line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    Message message = JsonUtil.fromJson(line, Message.class);
                    handleMessage(message);
                } catch (Exception e) {
                    System.err.println("[SERVER] Lỗi xử lý message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Client ngắt kết nối: " + playerName + " (" + playerId + ")");
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Message message) {
        if (message == null || message.getType() == null) return;

        switch (message.getType()) {
            case CONNECT_REQ -> {
                if (message.getPlayerName() != null && !message.getPlayerName().isBlank()) {
                    this.playerName = message.getPlayerName().trim();
                }
                Message ack = new Message(MessageType.CONNECT_ACK);
                ack.setPlayerId(this.playerId);
                ack.setPlayerName(this.playerName);
                ack.setPayload("Kết nối thành công tới Tiến Lên Server!");
                sendMessage(ack);
            }

            case CREATE_ROOM -> {
                int maxPlayers = message.getMaxPlayers();
                if (maxPlayers < 2 || maxPlayers > 4) {
                    maxPlayers = 4;
                }
                String roomId = message.getRoomId();
                if (roomId == null || roomId.isBlank()) {
                    roomId = roomManager.generateNextRoomId();
                } else {
                    roomId = roomId.trim().toUpperCase();
                }

                if (roomManager.hasRoom(roomId)) {
                    sendError("Mã phòng [" + roomId + "] đã tồn tại! Vui lòng chọn mã khác hoặc bấm Vào phòng.");
                    break;
                }

                GameRoom room = roomManager.createRoom(roomId, maxPlayers);
                this.currentRoom = room;
                room.addPlayer(this);
            }

            case JOIN_ROOM -> {
                String targetRoomId = message.getRoomId();
                int maxPlayers = message.getMaxPlayers() > 0 ? message.getMaxPlayers() : 4;
                GameRoom room;

                if (targetRoomId != null && !targetRoomId.isBlank()) {
                    targetRoomId = targetRoomId.trim().toUpperCase();
                    room = roomManager.findRoom(targetRoomId);
                    if (room == null) {
                        sendError("Không tìm thấy phòng mang mã [" + targetRoomId + "]! Vui lòng kiểm tra lại mã phòng.");
                        break;
                    }
                } else {
                    // Vào bàn nhanh: tìm phòng còn chỗ
                    room = roomManager.findAvailableRoom(maxPlayers);
                    if (room == null) {
                        String newRoomId = roomManager.generateNextRoomId();
                        room = roomManager.createRoom(newRoomId, maxPlayers);
                    }
                }

                if (room.isFull() || room.isPlaying()) {
                    sendError("Phòng [" + room.getRoomId() + "] đã đủ " + room.getMaxPlayers() + " người hoặc đang trong ván chơi!");
                } else {
                    this.currentRoom = room;
                    room.addPlayer(this);
                }
            }

            case START_GAME -> {
                if (currentRoom != null) {
                    currentRoom.requestStartGame(this);
                } else {
                    sendError("Bạn chưa vào phòng chơi nào!");
                }
            }

            case PLAY_CARDS -> {
                if (currentRoom != null) {
                    List<Card> cards = message.getCards();
                    currentRoom.handlePlayCards(this, cards);
                }
            }

            case PASS_TURN -> {
                if (currentRoom != null) {
                    currentRoom.handlePassTurn(this);
                }
            }

            default -> System.out.println("[SERVER] Chưa xử lý message type: " + message.getType());
        }
    }

    private void sendError(String errorMsg) {
        Message err = new Message(MessageType.ERROR_MSG);
        err.setPayload(errorMsg);
        sendMessage(err);
    }

    public synchronized void sendMessage(Message message) {
        if (out != null && !socket.isClosed()) {
            try {
                String json = JsonUtil.toJson(message);
                out.println(json);
            } catch (Exception e) {
                System.err.println("[SERVER] Lỗi gửi message tới " + playerName + ": " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    private void cleanup() {
        if (currentRoom != null) {
            currentRoom.removePlayer(this);
        }
        disconnect();
    }
}
