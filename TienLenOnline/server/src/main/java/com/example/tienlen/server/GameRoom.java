package com.example.tienlen.server;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.model.Deck;
import com.example.tienlen.common.model.Player;
import com.example.tienlen.common.model.PlayerPosition;
import com.example.tienlen.common.model.PlayerStatus;
import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.network.MessageType;
import com.example.tienlen.common.rule.SpecialMoveDetector;
import com.example.tienlen.common.rule.TienLenRule;

import java.util.*;
import java.util.concurrent.*;

/**
 * Quản lý một phòng chơi Tiến Lên từ 2 đến 4 người.
 * Tự động chia bài, chọn ngẫu nhiên người đánh đầu, quản lý lượt chơi,
 * gửi cảnh báo lượt đánh và xử lý luật Tiến Lên Miền Nam.
 */
public class GameRoom {
    public static final int TURN_TIMEOUT_SECONDS = 60;

    private final String roomId;
    private final int maxPlayers;
    private final RoomManager roomManager;
    private final List<IPlayerSession> players = new CopyOnWriteArrayList<>();
    private final Map<String, List<Card>> playerHands = new ConcurrentHashMap<>();

    private final ScheduledExecutorService turnTimerExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> turnTimerFuture = null;

    private volatile boolean isPlaying = false;
    private int currentTurnIndex = -1;
    private List<Card> tableCards = new ArrayList<>();
    private String lastPlayerWhoPlayedId = null;
    private final Set<String> passedPlayerIds = ConcurrentHashMap.newKeySet();
    private final List<String> finishedPlayerIds = new CopyOnWriteArrayList<>();

    public GameRoom(String roomId, int maxPlayers) {
        this(roomId, maxPlayers, null);
    }

    public GameRoom(String roomId, int maxPlayers, RoomManager roomManager) {
        this.roomId = roomId;
        this.maxPlayers = Math.max(2, Math.min(maxPlayers, 4));
        this.roomManager = roomManager;
    }

    public String getRoomId() {
        return roomId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public synchronized void addPlayer(IPlayerSession player) {
        if (isFull() || isPlaying) {
            Message err = new Message(MessageType.ERROR_MSG);
            err.setPayload("Phòng đã đủ " + maxPlayers + " người hoặc ván đang chơi!");
            player.sendMessage(err);
            return;
        }

        // Chống trùng tên trong cùng một phòng
        String baseName = player.getPlayerName();
        String uniqueName = baseName;
        int duplicateIndex = 2;
        boolean hasDuplicate = true;
        while (hasDuplicate) {
            hasDuplicate = false;
            for (IPlayerSession existing : players) {
                if (existing.getPlayerName().equalsIgnoreCase(uniqueName)) {
                    uniqueName = baseName + " (" + duplicateIndex + ")";
                    duplicateIndex++;
                    hasDuplicate = true;
                    break;
                }
            }
        }
        player.setPlayerName(uniqueName);

        players.add(player);
        System.out.println("[ROOM " + roomId + "] " + player.getPlayerName() + " tham gia (" + players.size() + "/" + maxPlayers + ")");

        broadcastRoomInfo();

        if (players.size() == maxPlayers) {
            startGame();
        }
    }

    public synchronized void removePlayer(IPlayerSession player) {
        players.remove(player);
        playerHands.remove(player.getPlayerId());
        passedPlayerIds.remove(player.getPlayerId());

        System.out.println("[ROOM " + roomId + "] " + player.getPlayerName() + " đã rời phòng.");

        if (players.isEmpty()) {
            cancelTurnTimer();
            turnTimerExecutor.shutdownNow();
            if (roomManager != null) {
                roomManager.removeRoom(roomId);
            }
            return;
        }

        if (isPlaying) {
            cancelTurnTimer();
            isPlaying = false;
            Message overMsg = new Message(MessageType.GAME_OVER);
            overMsg.setPayload("Người chơi " + player.getPlayerName() + " đã rời phòng. Ván đấu kết thúc!");
            broadcast(overMsg);
        } else {
            broadcastRoomInfo();
        }
    }

    /**
     * Cho phép chủ phòng kích hoạt bắt đầu ván đấu sớm khi đã có từ 2 người trở lên.
     */
    public synchronized void requestStartGame(IPlayerSession requester) {
        if (isPlaying) {
            sendError(requester, "Ván đấu đang diễn ra rồi!");
            return;
        }
        if (players.size() < 2) {
            sendError(requester, "Cần tối thiểu 2 người chơi trong bàn để bắt đầu ván!");
            return;
        }
        if (players.isEmpty() || !players.get(0).getPlayerId().equals(requester.getPlayerId())) {
            sendError(requester, "Chỉ chủ phòng mới có quyền bắt đầu ván đấu sớm!");
            return;
        }
        System.out.println("[ROOM " + roomId + "] Chủ phòng " + requester.getPlayerName() + " bắt đầu ván với " + players.size() + " người.");
        startGame();
    }

    public synchronized void startGame() {
        isPlaying = true;
        tableCards.clear();
        passedPlayerIds.clear();
        finishedPlayerIds.clear();
        lastPlayerWhoPlayedId = null;

        Deck deck = new Deck();
        List<List<Card>> dealtHands = deck.deal(players.size());
        for (int i = 0; i < players.size(); i++) {
            playerHands.put(players.get(i).getPlayerId(), new ArrayList<>(dealtHands.get(i)));
        }

        currentTurnIndex = new Random().nextInt(players.size());
        IPlayerSession startingPlayer = players.get(currentTurnIndex);

        System.out.println("[ROOM " + roomId + "] Bắt đầu ván với " + players.size() + " người! Lượt đầu ngẫu nhiên: " + startingPlayer.getPlayerName());

        String hostId = players.isEmpty() ? null : players.get(0).getPlayerId();
        for (int i = 0; i < players.size(); i++) {
            IPlayerSession p = players.get(i);
            Message startMsg = new Message(MessageType.GAME_STARTED);
            startMsg.setRoomId(roomId);
            startMsg.setPlayerId(p.getPlayerId());
            startMsg.setHostPlayerId(hostId);
            startMsg.setCards(playerHands.get(p.getPlayerId()));
            startMsg.setCurrentTurnPlayerId(startingPlayer.getPlayerId());
            startMsg.setCurrentTurnPlayerName(startingPlayer.getPlayerName());
            startMsg.setTurnTimeoutSeconds(TURN_TIMEOUT_SECONDS);
            startMsg.setPlayers(buildPlayerListFor(p));
            p.sendMessage(startMsg);
        }

        sendTurnAlert(startingPlayer);
        startTurnTimer();
    }

    public synchronized void handlePlayCards(IPlayerSession player, List<Card> cardsToPlay) {
        if (!isPlaying) {
            sendError(player, "Ván đấu chưa bắt đầu hoặc đã kết thúc!");
            return;
        }

        IPlayerSession currentTurnPlayer = players.get(currentTurnIndex);
        if (!player.getPlayerId().equals(currentTurnPlayer.getPlayerId())) {
            sendError(player, "Chưa đến lượt bạn đánh bài!");
            return;
        }

        List<Card> hand = playerHands.get(player.getPlayerId());
        if (hand == null || !hand.containsAll(cardsToPlay)) {
            sendError(player, "Bạn không sở hữu những lá bài này trên tay!");
            return;
        }

        String validationError = TienLenRule.validateMove(cardsToPlay, tableCards);
        if (validationError != null) {
            sendError(player, validationError);
            return;
        }

        cancelTurnTimer();

        List<Card> oldTableCards = new ArrayList<>(tableCards);
        String defenderName = "Người chơi trước";
        if (lastPlayerWhoPlayedId != null) {
            for (IPlayerSession p : players) {
                if (p.getPlayerId().equals(lastPlayerWhoPlayedId)) {
                    defenderName = p.getPlayerName();
                    break;
                }
            }
        }

        String specialAlert = SpecialMoveDetector.detectSpecialEvent(
                player.getPlayerName(), cardsToPlay, defenderName, oldTableCards);

        if (specialAlert != null) {
            System.out.println("[ROOM " + roomId + "] 💥 " + specialAlert);
        }

        for (Card c : cardsToPlay) {
            hand.remove(c);
        }
        tableCards = new ArrayList<>(cardsToPlay);
        lastPlayerWhoPlayedId = player.getPlayerId();

        System.out.println("[ROOM " + roomId + "] " + player.getPlayerName() + " đánh: " + cardsToPlay + " (Còn " + hand.size() + " lá)");

        if (hand.isEmpty()) {
            finishedPlayerIds.add(player.getPlayerId());
            System.out.println("[ROOM " + roomId + "] 🏆 " + player.getPlayerName() + " ĐÃ HẾT BÀI ĐẦU TIÊN VÀ VỀ NHẤT!");
            endGame();
            return;
        }

        advanceTurnAfterPlay(specialAlert);
    }

    public synchronized void handlePassTurn(IPlayerSession player) {
        if (!isPlaying) return;

        IPlayerSession currentTurnPlayer = players.get(currentTurnIndex);
        if (!player.getPlayerId().equals(currentTurnPlayer.getPlayerId())) {
            sendError(player, "Chưa đến lượt của bạn!");
            return;
        }

        if (tableCards.isEmpty()) {
            sendError(player, "Bạn là người bắt đầu vòng chơi mới, không thể bỏ lượt!");
            return;
        }

        cancelTurnTimer();

        passedPlayerIds.add(player.getPlayerId());
        System.out.println("[ROOM " + roomId + "] " + player.getPlayerName() + " bỏ lượt.");

        List<IPlayerSession> activeInRound = getActivePlayersInRound();
        if (activeInRound.size() <= 1) {
            startNewRound();
        } else {
            advanceTurnToNextActive();
            broadcastRoundUpdate(players.get(currentTurnIndex));
            startTurnTimer();
        }
    }

    private void advanceTurnAfterPlay(String actionAlert) {
        advanceTurnToNextActive();
        broadcastRoundUpdate(players.get(currentTurnIndex), actionAlert);
        startTurnTimer();
    }

    private void advanceTurnToNextActive() {
        int count = 0;
        int nextIndex = currentTurnIndex;
        while (count < players.size()) {
            nextIndex = (nextIndex + 1) % players.size();
            IPlayerSession nextPlayer = players.get(nextIndex);
            if (!finishedPlayerIds.contains(nextPlayer.getPlayerId())
                    && !passedPlayerIds.contains(nextPlayer.getPlayerId())) {
                currentTurnIndex = nextIndex;
                sendTurnAlert(nextPlayer);
                return;
            }
            count++;
        }
        startNewRound();
    }

    private void startNewRound() {
        System.out.println("[ROOM " + roomId + "] Hết vòng! Xóa bài trên bàn, bắt đầu vòng mới.");
        tableCards.clear();
        passedPlayerIds.clear();

        if (lastPlayerWhoPlayedId != null && !finishedPlayerIds.contains(lastPlayerWhoPlayedId)) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getPlayerId().equals(lastPlayerWhoPlayedId)) {
                    currentTurnIndex = i;
                    break;
                }
            }
        } else {
            for (int i = 0; i < players.size(); i++) {
                int idx = (currentTurnIndex + i) % players.size();
                if (!finishedPlayerIds.contains(players.get(idx).getPlayerId())) {
                    currentTurnIndex = idx;
                    break;
                }
            }
        }

        IPlayerSession newLeader = players.get(currentTurnIndex);

        for (IPlayerSession p : players) {
            Message newRoundMsg = new Message(MessageType.NEW_ROUND);
            newRoundMsg.setPayload("Vòng đánh mới! " + newLeader.getPlayerName() + " được quyền ra bài trước.");
            newRoundMsg.setCurrentTurnPlayerId(newLeader.getPlayerId());
            newRoundMsg.setCurrentTurnPlayerName(newLeader.getPlayerName());
            newRoundMsg.setTurnTimeoutSeconds(TURN_TIMEOUT_SECONDS);
            newRoundMsg.setPlayers(buildPlayerListFor(p));
            p.sendMessage(newRoundMsg);
        }

        sendTurnAlert(newLeader);
        startTurnTimer();
    }

    private List<IPlayerSession> getActivePlayersInRound() {
        List<IPlayerSession> active = new ArrayList<>();
        for (IPlayerSession p : players) {
            if (!finishedPlayerIds.contains(p.getPlayerId()) && !passedPlayerIds.contains(p.getPlayerId())) {
                active.add(p);
            }
        }
        return active;
    }

    private void sendTurnAlert(IPlayerSession turnPlayer) {
        Message alert = new Message(MessageType.TURN_ALERT);
        alert.setPlayerId(turnPlayer.getPlayerId());
        alert.setPlayerName(turnPlayer.getPlayerName());
        alert.setTurnTimeoutSeconds(TURN_TIMEOUT_SECONDS);
        alert.setPayload("⚠️ ĐẾN LƯỢT BẠN ĐÁNH BÀI!");
        turnPlayer.sendMessage(alert);
    }

    private void broadcastRoundUpdate(IPlayerSession currentTurn) {
        broadcastRoundUpdate(currentTurn, null);
    }

    private void broadcastRoundUpdate(IPlayerSession currentTurn, String actionAlert) {
        for (IPlayerSession p : players) {
            Message update = new Message(MessageType.ROUND_UPDATE);
            update.setCards(new ArrayList<>(tableCards));
            update.setCurrentTurnPlayerId(currentTurn.getPlayerId());
            update.setCurrentTurnPlayerName(currentTurn.getPlayerName());
            update.setTurnTimeoutSeconds(TURN_TIMEOUT_SECONDS);
            update.setPayload(actionAlert);
            update.setPlayers(buildPlayerListFor(p));
            p.sendMessage(update);
        }
    }

    private void endGame() {
        cancelTurnTimer();
        isPlaying = false;
        String winnerId = finishedPlayerIds.isEmpty() ? null : finishedPlayerIds.get(0);
        String winnerName = "Không xác định";
        for (IPlayerSession p : players) {
            if (p.getPlayerId().equals(winnerId)) {
                winnerName = p.getPlayerName();
                break;
            }
        }

        System.out.println("[ROOM " + roomId + "] 🏆 Ván đấu kết thúc! Quán quân về nhất: " + winnerName);

        StringBuilder summary = new StringBuilder();
        summary.append("🏆 QUÁN QUÂN VỀ NHẤT: ").append(winnerName).append(" (Đánh hết bài đầu tiên!)\n\n");
        summary.append("📊 BẢNG TỔNG KẾT VÁN ĐẤU:\n");
        summary.append("  🥇 ").append(winnerName).append(" — VỀ NHẤT (0 lá bài)\n");

        for (IPlayerSession p : players) {
            if (!p.getPlayerId().equals(winnerId)) {
                int cardsLeft = playerHands.getOrDefault(p.getPlayerId(), Collections.emptyList()).size();
                summary.append("  🥈 ").append(p.getPlayerName()).append(" — Còn ").append(cardsLeft).append(" lá bài\n");
            }
        }

        String hostId = players.isEmpty() ? null : players.get(0).getPlayerId();
        for (IPlayerSession p : players) {
            Message overMsg = new Message(MessageType.GAME_OVER);
            overMsg.setRoomId(roomId);
            overMsg.setHostPlayerId(hostId);
            overMsg.setPlayerId(winnerId);
            overMsg.setPlayerName(winnerName);
            overMsg.setPayload(summary.toString());
            overMsg.setPlayers(buildPlayerListFor(p));
            p.sendMessage(overMsg);
        }
    }

    private synchronized void startTurnTimer() {
        cancelTurnTimer();
        if (!isPlaying || players.isEmpty() || currentTurnIndex < 0 || currentTurnIndex >= players.size()) {
            return;
        }

        turnTimerFuture = turnTimerExecutor.schedule(this::onTurnTimeout, TURN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void cancelTurnTimer() {
        if (turnTimerFuture != null) {
            turnTimerFuture.cancel(false);
            turnTimerFuture = null;
        }
    }

    private synchronized void onTurnTimeout() {
        if (!isPlaying || players.isEmpty() || currentTurnIndex < 0 || currentTurnIndex >= players.size()) {
            return;
        }
        IPlayerSession turnPlayer = players.get(currentTurnIndex);
        if (turnPlayer == null) return;

        System.out.println("[ROOM " + roomId + "] ⏰ HẾT 60 GIÂY! Tự động xử lý lượt cho " + turnPlayer.getPlayerName());

        List<Card> hand = playerHands.get(turnPlayer.getPlayerId());
        if (hand == null || hand.isEmpty()) {
            handlePassTurn(turnPlayer);
            return;
        }

        List<Card> autoMove = TienLenRule.suggestMove(hand, tableCards);
        if (autoMove != null && !autoMove.isEmpty()) {
            System.out.println("[ROOM " + roomId + "] ⏰ Hết 60s: Tự động đánh bài gợi ý cho " + turnPlayer.getPlayerName() + ": " + autoMove);
            handlePlayCards(turnPlayer, autoMove);
        } else if (tableCards.isEmpty()) {
            List<Card> fallback = List.of(hand.get(0));
            System.out.println("[ROOM " + roomId + "] ⏰ Hết 60s: Tự động đánh lá nhỏ nhất mở vòng cho " + turnPlayer.getPlayerName() + ": " + fallback);
            handlePlayCards(turnPlayer, fallback);
        } else {
            System.out.println("[ROOM " + roomId + "] ⏰ Hết 60s: Không có bài hợp lệ để chặn -> Tự động bỏ lượt cho " + turnPlayer.getPlayerName());
            handlePassTurn(turnPlayer);
        }
    }

    private void sendError(IPlayerSession player, String message) {
        Message err = new Message(MessageType.ERROR_MSG);
        err.setPayload(message);
        player.sendMessage(err);
    }

    private void broadcast(Message message) {
        for (IPlayerSession p : players) {
            p.sendMessage(message);
        }
    }

    private void broadcastRoomInfo() {
        String hostId = players.isEmpty() ? null : players.get(0).getPlayerId();
        for (IPlayerSession p : players) {
            Message info = new Message(MessageType.ROOM_INFO);
            info.setRoomId(roomId);
            info.setMaxPlayers(maxPlayers);
            info.setHostPlayerId(hostId);
            String statusText;
            if (players.size() >= 2) {
                statusText = "Đã có " + players.size() + "/" + maxPlayers + " người. Chủ phòng có thể bấm BẮT ĐẦU VÁN hoặc chờ thêm!";
            } else {
                statusText = "Đang chờ người chơi (" + players.size() + "/" + maxPlayers + ")...";
            }
            info.setPayload(statusText);
            info.setPlayers(buildPlayerListFor(p));
            p.sendMessage(info);
        }
    }

    private List<Player> buildPlayerListFor(IPlayerSession viewer) {
        List<Player> result = new ArrayList<>();
        int viewerIndex = players.indexOf(viewer);
        if (viewerIndex < 0) viewerIndex = 0;

        int total = players.size();
        for (int i = 0; i < total; i++) {
            IPlayerSession p = players.get(i);
            int relativePos = (i - viewerIndex + total) % total;

            PlayerPosition pos;
            if (relativePos == 0) {
                pos = PlayerPosition.BOTTOM;
            } else if (total == 2) {
                pos = PlayerPosition.TOP;
            } else if (total == 3) {
                pos = relativePos == 1 ? PlayerPosition.RIGHT : PlayerPosition.LEFT;
            } else {
                pos = switch (relativePos) {
                    case 1 -> PlayerPosition.RIGHT;
                    case 2 -> PlayerPosition.TOP;
                    case 3 -> PlayerPosition.LEFT;
                    default -> PlayerPosition.TOP;
                };
            }

            PlayerStatus status;
            if (finishedPlayerIds.contains(p.getPlayerId())) {
                status = PlayerStatus.WINNER;
            } else if (!isPlaying) {
                status = PlayerStatus.FINISHED;
            } else if (passedPlayerIds.contains(p.getPlayerId())) {
                status = PlayerStatus.PASSED;
            } else if (currentTurnIndex >= 0 && currentTurnIndex < players.size() && players.get(currentTurnIndex).getPlayerId().equals(p.getPlayerId())) {
                status = PlayerStatus.PLAYING;
            } else {
                status = PlayerStatus.WAITING;
            }

            int cardCount = playerHands.getOrDefault(p.getPlayerId(), Collections.emptyList()).size();
            result.add(new Player(p.getPlayerId(), p.getPlayerName(), cardCount, pos, status));
        }
        return result;
    }
}
