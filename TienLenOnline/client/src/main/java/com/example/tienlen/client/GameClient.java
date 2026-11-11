package com.example.tienlen.client;

import com.example.tienlen.client.rmi.TienLenClientCallbackImpl;
import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.network.MessageType;
import com.example.tienlen.common.network.NetworkUtil;
import com.example.tienlen.common.rmi.ITienLenClientCallback;
import com.example.tienlen.common.rmi.ITienLenService;
import javafx.application.Platform;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Quản lý kết nối và giao tiếp từ xa tới Server qua Java RMI (Remote Method Invocation).
 * Hỗ trợ các máy tính trong cùng mạng LAN kết nối bằng địa chỉ IP của máy chủ.
 * Tự động chuyển tiếp sự kiện nhận được lên JavaFX Application Thread.
 */
public class GameClient {
    private ITienLenService service;
    private ITienLenClientCallback callback;
    private Thread pollerThread;
    private volatile boolean connected = false;

    private String myPlayerId;
    private String myPlayerName;
    private String currentRoomId;

    // Bộ chống trùng lặp thông điệp nhận từ cả RMI Callback và Poller
    private final Set<String> processedMessageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Consumer<Message> onConnectSuccess;
    private Consumer<Message> onRoomInfo;
    private Consumer<Message> onGameStarted;
    private Consumer<Message> onRoundUpdate;
    private Consumer<Message> onNewRound;
    private Consumer<Message> onTurnAlert;
    private Consumer<Message> onGameOver;
    private Consumer<String> onError;
    private Runnable onDisconnected;

    public void connect(String host, int port, String playerName) throws Exception {
        if (port <= 0 || port == 8888) {
            port = ITienLenService.DEFAULT_RMI_PORT; // 1099
        }
        if (host == null || host.isBlank()) {
            host = "127.0.0.1";
        }

        // Đặt hostname của Client cho RMI để server có thể định tuyến gọi ngược lại qua LAN
        String clientLanIp = NetworkUtil.getLocalLanIp();
        System.setProperty("java.rmi.server.hostname", clientLanIp);

        Registry registry = LocateRegistry.getRegistry(host, port);
        this.service = (ITienLenService) registry.lookup(ITienLenService.SERVICE_NAME);
        this.myPlayerName = playerName;

        // Tạo đối tượng RMI Callback nhận thông điệp từ Server
        this.callback = new TienLenClientCallbackImpl(msg -> Platform.runLater(() -> dispatchMessage(msg)));

        // Đăng ký người chơi lên RMI Server
        this.myPlayerId = service.registerPlayer(playerName, callback);
        this.connected = true;

        System.out.println("[CLIENT] Đã kết nối RMI Server (" + host + ":" + port + ") với ID: " + myPlayerId);

        // Khởi động luồng phụ Poller nhằm đảm bảo nhận 100% thông điệp kể cả khi tường lửa LAN chặn RMI Callback
        pollerThread = new Thread(this::pollLoop, "RmiClient-Poller");
        pollerThread.setDaemon(true);
        pollerThread.start();
    }

    private void pollLoop() {
        while (connected) {
            try {
                if (service != null && myPlayerId != null) {
                    List<Message> pending = service.pollEvents(myPlayerId);
                    if (pending != null && !pending.isEmpty()) {
                        for (Message m : pending) {
                            Platform.runLater(() -> dispatchMessage(m));
                        }
                    }
                }
                Thread.sleep(120);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                if (connected) {
                    System.out.println("[CLIENT] Mất kết nối tới RMI Server: " + e.getMessage());
                    disconnect();
                    break;
                }
            }
        }
    }

    private void dispatchMessage(Message message) {
        if (message == null || message.getType() == null) return;

        // Khử trùng lặp thông điệp
        if (message.getMessageId() != null) {
            if (!processedMessageIds.add(message.getMessageId())) {
                return;
            }
        }

        if (message.getRoomId() != null && !message.getRoomId().isBlank()) {
            this.currentRoomId = message.getRoomId();
        }

        switch (message.getType()) {
            case CONNECT_ACK -> {
                this.myPlayerId = message.getPlayerId();
                if (onConnectSuccess != null) onConnectSuccess.accept(message);
            }
            case ROOM_INFO -> {
                if (onRoomInfo != null) onRoomInfo.accept(message);
            }
            case GAME_STARTED -> {
                if (onGameStarted != null) onGameStarted.accept(message);
            }
            case ROUND_UPDATE -> {
                if (onRoundUpdate != null) onRoundUpdate.accept(message);
            }
            case NEW_ROUND -> {
                if (onNewRound != null) onNewRound.accept(message);
            }
            case TURN_ALERT -> {
                if (onTurnAlert != null) onTurnAlert.accept(message);
            }
            case GAME_OVER -> {
                if (onGameOver != null) onGameOver.accept(message);
            }
            case ERROR_MSG -> {
                if (onError != null) onError.accept(message.getPayload());
            }
            default -> System.out.println("[CLIENT] Nhận thông điệp không xác định: " + message.getType());
        }
    }

    public void createRoom(String roomId, int maxPlayers) {
        this.currentRoomId = roomId;
        executeRemote(() -> service.createRoom(roomId, myPlayerId, maxPlayers));
    }

    public void joinRoom(String roomId, int maxPlayers) {
        this.currentRoomId = roomId;
        executeRemote(() -> service.joinRoom(roomId, myPlayerId, maxPlayers));
    }

    public void startGame() {
        executeRemote(() -> service.startGame(currentRoomId, myPlayerId));
    }

    public void playCards(List<Card> cards) {
        executeRemote(() -> service.playCards(currentRoomId, myPlayerId, cards));
    }

    public void passTurn() {
        executeRemote(() -> service.passTurn(currentRoomId, myPlayerId));
    }

    public void disconnect() {
        connected = false;
        if (pollerThread != null) {
            pollerThread.interrupt();
            pollerThread = null;
        }
        if (service != null && myPlayerId != null) {
            try {
                service.disconnect(myPlayerId);
            } catch (Exception ignored) {
            }
        }
        if (callback != null) {
            try {
                UnicastRemoteObject.unexportObject(callback, true);
            } catch (Exception ignored) {
            }
            callback = null;
        }
        if (onDisconnected != null) {
            Platform.runLater(onDisconnected);
        }
    }

    private void executeRemote(RemoteTask task) {
        if (!connected || service == null) return;
        try {
            task.execute();
        } catch (Exception e) {
            System.err.println("[CLIENT] Lỗi gọi hàm từ xa RMI: " + e.getMessage());
            if (onError != null) {
                Platform.runLater(() -> onError.accept("Lỗi gọi RMI: " + e.getMessage()));
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getMyPlayerId() {
        return myPlayerId;
    }

    public String getMyPlayerName() {
        return myPlayerName;
    }

    public void setOnConnectSuccess(Consumer<Message> onConnectSuccess) {
        this.onConnectSuccess = onConnectSuccess;
    }

    public void setOnRoomInfo(Consumer<Message> onRoomInfo) {
        this.onRoomInfo = onRoomInfo;
    }

    public void setOnGameStarted(Consumer<Message> onGameStarted) {
        this.onGameStarted = onGameStarted;
    }

    public void setOnRoundUpdate(Consumer<Message> onRoundUpdate) {
        this.onRoundUpdate = onRoundUpdate;
    }

    public void setOnNewRound(Consumer<Message> onNewRound) {
        this.onNewRound = onNewRound;
    }

    public void setOnTurnAlert(Consumer<Message> onTurnAlert) {
        this.onTurnAlert = onTurnAlert;
    }

    public void setOnGameOver(Consumer<Message> onGameOver) {
        this.onGameOver = onGameOver;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    public void setOnDisconnected(Runnable onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    @FunctionalInterface
    private interface RemoteTask {
        void execute() throws Exception;
    }
}
