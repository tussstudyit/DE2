package com.example.tienlen.client;

import com.example.tienlen.client.ui.GameView;
import com.example.tienlen.client.ui.LobbyView;
import com.example.tienlen.common.model.Card;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

import com.example.tienlen.common.rule.CardCombinationType;
import com.example.tienlen.common.rule.TienLenRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Điều khiển luồng ứng dụng Client: chuyển đổi giữa Lobby và Bàn chơi,
 * tự động kết nối Java RMI (mặc định 127.0.0.1:1099 hoặc IP mạng LAN), hỗ trợ mã bàn 6 số, nhận diện nước đi và gợi ý bài.
 */
public class GameController {
    private static final String DEFAULT_HOST = System.getProperty("tienlen.server.host", "127.0.0.1");
    private static final int DEFAULT_PORT = Integer.getInteger("tienlen.server.port", 1099);

    private final StackPane rootPane = new StackPane();
    private final LobbyView lobbyView = new LobbyView();
    private final GameView gameView = new GameView();
    private final GameClient client = new GameClient();

    private String currentRoomId;
    private String currentHostPlayerId;
    private int declaredPlayerCount = 4;
    private List<Card> myLastSelectedCards;
    private final List<Card> currentMyHandCards = new ArrayList<>();
    private final List<Card> currentTableCards = new ArrayList<>();
    private int hintCycleIndex = 0;
    private String lastTurnProcessedKey = "";

    // Quản lý đồng hồ đếm ngược 60 giây và hiển thị lượt chạy
    private Timeline clientTurnTimeline;
    private int currentRemainingSeconds = 60;
    private String currentActiveTurnPlayerId = null;
    private String currentActiveTurnPlayerName = null;

    public GameController() {
        rootPane.getChildren().add(lobbyView);
        setupClientCallbacks();
        setupLobbyEvents();
        setupGameEvents();
        setupKeyEvents();
    }

    public Parent getRoot() {
        return rootPane;
    }

    public LobbyView getLobbyView() {
        return lobbyView;
    }

    private void setupLobbyEvents() {
        lobbyView.getCreateRoomButton().setOnAction(e -> handleCreateRoom());
        lobbyView.getJoinRoomButton().setOnAction(e -> handleJoinRoom());
        lobbyView.getQuickJoinButton().setOnAction(e -> handleQuickJoin());
    }

    private void handleCreateRoom() {
        String name = lobbyView.getPlayerName();
        int players = lobbyView.getPlayerCount();
        String roomId = lobbyView.getRoomId();

        if (name.isBlank()) {
            lobbyView.setErrorStatus("Vui lòng nhập tên người chơi!");
            return;
        }

        if (roomId.isBlank()) {
            lobbyView.generateRandomRoomCode();
            roomId = lobbyView.getRoomId();
        } else if (!roomId.matches("\\d{6}")) {
            lobbyView.setErrorStatus("Mã phòng phải là dãy gồm đúng 6 chữ số (ví dụ: 100001)!");
            return;
        }

        declaredPlayerCount = players;
        currentRoomId = roomId;
        ClientInstanceDetector.saveLastRoomCode(currentRoomId);

        String host = lobbyView.getServerHost();
        ClientInstanceDetector.saveLastServerHost(host);

        lobbyView.setBusy(true);
        lobbyView.setStatus("Đang kết nối tới RMI Server (" + host + ")...");

        try {
            if (!client.isConnected()) {
                client.connect(host, DEFAULT_PORT, name);
            }
            client.createRoom(currentRoomId, declaredPlayerCount);
        } catch (Exception ex) {
            lobbyView.setBusy(false);
            lobbyView.setErrorStatus("Không thể kết nối tới RMI Server tại " + host + ": " + ex.getMessage() + ". Vui lòng kiểm tra ServerApplication đã chạy chưa.");
        }
    }

    private void handleJoinRoom() {
        String name = lobbyView.getPlayerName();
        int players = lobbyView.getPlayerCount();
        String roomId = lobbyView.getRoomId();

        if (name.isBlank()) {
            lobbyView.setErrorStatus("Vui lòng nhập tên người chơi!");
            return;
        }

        if (roomId.isBlank()) {
            lobbyView.setErrorStatus("Vui lòng nhập mã phòng 6 chữ số cần vào!");
            return;
        } else if (!roomId.matches("\\d{6}")) {
            lobbyView.setErrorStatus("Mã phòng phải là dãy gồm đúng 6 chữ số (ví dụ: 100001)!");
            return;
        }

        declaredPlayerCount = players;
        currentRoomId = roomId;

        String host = lobbyView.getServerHost();
        ClientInstanceDetector.saveLastServerHost(host);

        lobbyView.setBusy(true);
        lobbyView.setStatus("Đang kết nối tới RMI Server (" + host + ")...");

        try {
            if (!client.isConnected()) {
                client.connect(host, DEFAULT_PORT, name);
            }
            client.joinRoom(currentRoomId, declaredPlayerCount);
        } catch (Exception ex) {
            lobbyView.setBusy(false);
            lobbyView.setErrorStatus("Không thể kết nối tới RMI Server tại " + host + ": " + ex.getMessage() + ". Vui lòng kiểm tra ServerApplication đã chạy chưa.");
        }
    }

    private void handleQuickJoin() {
        String name = lobbyView.getPlayerName();
        int players = lobbyView.getPlayerCount();

        if (name.isBlank()) {
            lobbyView.setErrorStatus("Vui lòng nhập tên người chơi!");
            return;
        }

        declaredPlayerCount = players;
        currentRoomId = null;

        String host = lobbyView.getServerHost();
        ClientInstanceDetector.saveLastServerHost(host);

        lobbyView.setBusy(true);
        lobbyView.setStatus("Đang kết nối RMI và tìm bàn nhanh...");

        try {
            if (!client.isConnected()) {
                client.connect(host, DEFAULT_PORT, name);
            }
            client.joinRoom(null, declaredPlayerCount);
        } catch (Exception ex) {
            lobbyView.setBusy(false);
            lobbyView.setErrorStatus("Không thể kết nối tới RMI Server tại " + host + ": " + ex.getMessage());
        }
    }

    private void setupGameEvents() {
        gameView.setOnPlayRequested(e -> handlePlayCards());
        gameView.setOnPassRequested(e -> handlePassTurn());
        gameView.setOnLeaveRequested(e -> handleLeaveRoom());
        gameView.setOnStartGameRequested(e -> handleStartGame());
        gameView.setOnHintRequested(e -> handleHint());
        gameView.setOnHandSelectionChanged(this::updatePlayButtonAndSelectionState);
    }

    private void setupKeyEvents() {
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    // Nếu đang ở màn hình sảnh Lobby, không can thiệp phím để người dùng nhập text bình thường
                    if (!rootPane.getChildren().contains(gameView)) {
                        return;
                    }

                    if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                        if (gameView.isPlayButtonEnabled() && !gameView.getSelectedCards().isEmpty()) {
                            handlePlayCards();
                            event.consume();
                        } else if (gameView.isPassButtonEnabled() && (gameView.isPassButtonHighlighted() || gameView.getSelectedCards().isEmpty())) {
                            handlePassTurn();
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.H) {
                        if (gameView.isHintButtonEnabled()) {
                            handleHint();
                            event.consume();
                        }
                    } else if (event.getCode() == KeyCode.ESCAPE) {
                        gameView.clearSelectedCards();
                        event.consume();
                    }
                });
            }
        });
    }

    private void handleStartGame() {
        client.startGame();
    }

    private void handleHint() {
        if (currentMyHandCards.isEmpty()) {
            return;
        }

        List<List<Card>> validMoves = TienLenRule.findValidMoves(currentMyHandCards, currentTableCards);
        if (validMoves.isEmpty()) {
            gameView.clearSelectedCards();
            gameView.setTurnActive(true, false);
            gameView.setTurnMessage("⚠️ BẠN KHÔNG CÓ BÀI ĐỂ CHẶN -> BẤM 'BỎ LƯỢT'");
            gameView.setMatchStatus("Bạn không có bài nào chặn được! Vui lòng bấm BỎ LƯỢT.");
            updatePlayButtonAndSelectionState();
            return;
        }

        hintCycleIndex++;
        int currentOpt = (hintCycleIndex % validMoves.size());
        List<Card> chosenMove = validMoves.get(currentOpt);

        // Tự động đẩy các lá bài của phương án này lên (sẽ kích hoạt onHandSelectionChanged)
        gameView.selectCards(chosenMove);

        String cardStr = chosenMove.stream().map(Card::displayText).collect(Collectors.joining(" "));
        CardCombinationType type = TienLenRule.getCombinationType(chosenMove);
        String typeName = type != null ? type.getDisplayName() : "bài";

        gameView.setHintButtonText("💡 ĐỔI GỢI Ý (" + (currentOpt + 1) + "/" + validMoves.size() + ")");
        gameView.setMatchStatus("💡 Gợi ý (" + (currentOpt + 1) + "/" + validMoves.size() + "): " + typeName + " [ " + cardStr + " ] -> Bấm 'ĐÁNH BÀI' (Space) để ra bài!");
        updatePlayButtonAndSelectionState();
    }

    /**
     * Cập nhật thời gian thực trạng thái sáng/tối và kích hoạt của nút 'ĐÁNH BÀI' và 'BỎ LƯỢT':
     * - Nếu bài đang chọn KHÔNG hợp lệ hoặc rỗng: Nút Đánh bài KHÔNG SÁNG và bị vô hiệu hóa.
     * - Nếu bài đang chọn HỢP LỆ (đè được bài trên bàn hoặc hợp lệ ở bàn trống): Nút Đánh bài SÁNG XANH rực rỡ.
     * - Nếu bàn trống: Bắt buộc phải đánh, không được phép bỏ lượt.
     */
    private void updatePlayButtonAndSelectionState() {
        boolean isMyTurn = client.getMyPlayerId() != null && client.getMyPlayerId().equals(currentActiveTurnPlayerId);
        if (!isMyTurn) {
            gameView.setPlayButtonState(false, false);
            gameView.setPassButtonState(false, false);
            return;
        }

        List<Card> selected = gameView.getSelectedCards();
        boolean hasAnyValidInHand = TienLenRule.hasValidMove(currentMyHandCards, currentTableCards);

        // Người mở đầu vòng chơi mới (bàn trống) bắt buộc phải ra bài, không thể bỏ lượt
        boolean canPass = !currentTableCards.isEmpty();
        gameView.setPassButtonState(canPass, canPass && !hasAnyValidInHand);

        if (selected.isEmpty()) {
            // Chưa chọn bài -> Nút Đánh bài KHÔNG SÁNG
            gameView.setPlayButtonState(false, false);
            if (!hasAnyValidInHand) {
                gameView.setMatchStatus("Hệ thống nhận diện: Bạn không có bài hợp lệ để đè bài trên bàn. Hãy bấm BỎ LƯỢT.");
            } else {
                gameView.setMatchStatus("Hãy nhấp chọn các lá bài hợp lệ hoặc bấm 'GỢI Ý' (Tab) để ra bài.");
            }
            return;
        }

        // Kiểm tra tính hợp lệ của tổ hợp bài đã chọn so với bài trên bàn
        String validationError = TienLenRule.validateMove(selected, currentTableCards);
        if (validationError != null) {
            // NƯỚC ĐI KHÔNG HỢP LỆ -> Nút Đánh bài KHÔNG SÁNG và bị vô hiệu hóa!
            gameView.setPlayButtonState(false, false);
            String cardStr = selected.stream().map(Card::displayText).collect(Collectors.joining(" "));
            gameView.setMatchStatus("⚠️ Bài chọn [ " + cardStr + " ] không hợp lệ: " + validationError);
        } else {
            // NƯỚC ĐI HỢP LỆ -> Nút Đánh bài SÁNG XANH rực rỡ và cho phép click!
            gameView.setPlayButtonState(true, true);
            CardCombinationType type = TienLenRule.getCombinationType(selected);
            String typeName = type != null ? type.getDisplayName() : "bài";
            String cardStr = selected.stream().map(Card::displayText).collect(Collectors.joining(" "));
            gameView.setMatchStatus("✅ Hợp lệ: " + typeName + " [ " + cardStr + " ] -> Bấm 'ĐÁNH BÀI' (Space/Enter) để ra bài!");
        }
    }

    private void applyMyTurn() {
        String turnKey = currentMyHandCards.size() + "_" + currentTableCards.toString();
        boolean isSameTurn = turnKey.equals(lastTurnProcessedKey);
        lastTurnProcessedKey = turnKey;

        if (isSameTurn && !gameView.getSelectedCards().isEmpty()) {
            updatePlayButtonAndSelectionState();
            return;
        }

        hintCycleIndex = 0;
        List<List<Card>> validMoves = TienLenRule.findValidMoves(currentMyHandCards, currentTableCards);
        boolean hasValid = !validMoves.isEmpty();

        gameView.setTurnActive(true, hasValid);

        if (!hasValid) {
            gameView.clearSelectedCards();
            gameView.setHintButtonText("💡 GỢI Ý (0)");
            gameView.setTurnMessage("⚠️ BẠN KHÔNG CÓ BÀI ĐỂ CHẶN -> BẤM 'BỎ LƯỢT' (Space/Enter)");
            updatePlayButtonAndSelectionState();
            return;
        }

        // TỰ ĐỘNG ĐẨY NHỮNG LÁ NƯỚC ĐI TIẾP THEO CHO NGƯỜI CHƠI (Chuẩn Web Game Facebook & ZingPlay)
        // Người chơi không cần chọn tính toán, chỉ việc bấm ĐÁNH BÀI hoặc phím Space/Enter!
        List<Card> bestMove = validMoves.get(0);
        gameView.selectCards(bestMove);

        String cardStr = bestMove.stream().map(Card::displayText).collect(Collectors.joining(" "));
        CardCombinationType type = TienLenRule.getCombinationType(bestMove);
        String typeName = type != null ? type.getDisplayName() : "bài";

        if (validMoves.size() > 1) {
            gameView.setHintButtonText("💡 ĐỔI GỢI Ý (1/" + validMoves.size() + ")");
            gameView.setMatchStatus("Đã đẩy sẵn: " + typeName + " [ " + cardStr + " ] -> Bấm 'ĐÁNH BÀI' (Space) hoặc 'ĐỔI GỢI Ý' (Tab).");
        } else {
            gameView.setHintButtonText("💡 GỢI Ý (1/1)");
            gameView.setMatchStatus("Đã đẩy sẵn: " + typeName + " [ " + cardStr + " ] -> Bấm 'ĐÁNH BÀI' (Space) để đánh ngay!");
        }
        updatePlayButtonAndSelectionState();
    }

    private void applyOpponentTurn(String opponentName, String actionDesc) {
        lastTurnProcessedKey = "";
        gameView.clearSelectedCards();
        gameView.setTurnActive(false, false);
        gameView.setHintButtonText("💡 GỢI Ý");
        gameView.setTurnMessage("LƯỢT ĐÁNH CỦA: " + opponentName.toUpperCase());
        gameView.setMatchStatus("Đang chờ " + opponentName + " " + actionDesc + "...");
    }

    private void handlePlayCards() {
        boolean isMyTurn = client.getMyPlayerId() != null && client.getMyPlayerId().equals(currentActiveTurnPlayerId);
        if (!isMyTurn) {
            return;
        }

        List<Card> selected = gameView.getSelectedCards();
        if (selected.isEmpty()) {
            gameView.setMatchStatus("Hãy nhấp chọn ít nhất một lá bài trước khi đánh!");
            updatePlayButtonAndSelectionState();
            return;
        }

        // Kiểm tra tính hợp lệ chặt chẽ trước khi gửi tới server
        String validationError = TienLenRule.validateMove(selected, currentTableCards);
        if (validationError != null) {
            gameView.setMatchStatus("⚠️ Không thể đánh bài: " + validationError);
            updatePlayButtonAndSelectionState();
            return;
        }

        // Vô hiệu hóa ngay lập tức nút đánh bài để tránh double-click và dừng timer
        gameView.setPlayButtonState(false, false);
        stopClientTurnTimer();
        myLastSelectedCards = selected;
        client.playCards(selected);
    }

    private void handlePassTurn() {
        boolean isMyTurn = client.getMyPlayerId() != null && client.getMyPlayerId().equals(currentActiveTurnPlayerId);
        if (!isMyTurn) {
            return;
        }

        if (currentTableCards.isEmpty()) {
            gameView.setMatchStatus("⚠️ Bạn là người mở đầu vòng chơi mới, không thể bỏ lượt!");
            updatePlayButtonAndSelectionState();
            return;
        }

        // Vô hiệu hóa ngay lập tức nút bỏ lượt và phản hồi tức thì ngay từ click đầu tiên
        gameView.setTurnActive(false, false);
        gameView.clearSelectedCards();
        gameView.setTurnMessage("ĐÃ BỎ LƯỢT - ĐANG CHỜ NGƯỜI TIẾP THEO...");
        gameView.setMatchStatus("Bạn đã bỏ lượt thành công. Đang chờ người tiếp theo...");
        stopClientTurnTimer();
        client.passTurn();
    }

    private void handleLeaveRoom() {
        stopClientTurnTimer();
        gameView.closeCelebrationDialog();
        client.disconnect();
        showLobbyView();
        lobbyView.setBusy(false);
        lobbyView.setStatus("Đã rời phòng chơi.");
    }

    private void startClientTurnTimer(String activePlayerId, String activePlayerName, int totalSeconds) {
        stopClientTurnTimer();
        currentActiveTurnPlayerId = activePlayerId;
        currentActiveTurnPlayerName = activePlayerName;
        currentRemainingSeconds = totalSeconds;

        gameView.updateTurnTimer(activePlayerId, currentRemainingSeconds, totalSeconds);
        updateTurnBannerTimer();

        clientTurnTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            currentRemainingSeconds--;
            gameView.updateTurnTimer(currentActiveTurnPlayerId, currentRemainingSeconds, totalSeconds);
            updateTurnBannerTimer();

            if (currentRemainingSeconds <= 0) {
                stopClientTurnTimer();
                onClientTurnExpired();
            }
        }));
        clientTurnTimeline.setCycleCount(totalSeconds);
        clientTurnTimeline.play();
    }

    private void updateTurnBannerTimer() {
        boolean isMyTurn = client.getMyPlayerId() != null && client.getMyPlayerId().equals(currentActiveTurnPlayerId);
        if (isMyTurn) {
            boolean hasValid = TienLenRule.hasValidMove(currentMyHandCards, currentTableCards);
            gameView.setTurnBannerForMe(currentRemainingSeconds, hasValid);
        } else if (currentActiveTurnPlayerName != null) {
            gameView.setTurnBannerForOpponent(currentActiveTurnPlayerName, currentRemainingSeconds);
        }
    }

    private void stopClientTurnTimer() {
        if (clientTurnTimeline != null) {
            clientTurnTimeline.stop();
            clientTurnTimeline = null;
        }
        gameView.stopAllTurnTimers();
    }

    /**
     * Tự động đánh bài gợi ý khi người dùng không phản hồi trong 60 giây.
     * - Nếu có bài gợi ý hợp lệ để chặn bài trên bàn: Tự động đánh bài gợi ý đó.
     * - Nếu là người mở vòng mới (bàn trống): Tự động đánh bài gợi ý hoặc lá nhỏ nhất.
     * - Nếu không có bài nào chặn được: Tự động bỏ lượt.
     */
    private void autoPlaySuggestedCardsOrPass() {
        boolean isMyTurn = client.getMyPlayerId() != null && client.getMyPlayerId().equals(currentActiveTurnPlayerId);
        if (!isMyTurn) {
            return;
        }

        System.out.println("[CLIENT] ⏰ Hết 60 giây! Tự động đánh bài gợi ý cho người chơi.");

        // 1. Kiểm tra nếu các lá bài đang chọn trên màn hình là nước đi hợp lệ -> Đánh ngay
        List<Card> selected = gameView.getSelectedCards();
        if (selected != null && !selected.isEmpty() && TienLenRule.validateMove(selected, currentTableCards) == null) {
            gameView.setMatchStatus("⏰ Hết 60s: Hệ thống tự động đánh bài gợi ý cho bạn!");
            handlePlayCards();
            return;
        }

        // 2. Tìm nước đi gợi ý tối ưu nhất theo luật chơi
        List<Card> suggested = TienLenRule.suggestMove(currentMyHandCards, currentTableCards);
        if (suggested != null && !suggested.isEmpty()) {
            gameView.selectCards(suggested);
            gameView.setMatchStatus("⏰ Hết 60s: Hệ thống tự động đánh nước đi gợi ý!");
            handlePlayCards();
            return;
        }

        // 3. Nếu trên bàn trống và chưa có gợi ý, tự động đánh lá nhỏ nhất
        if (currentTableCards.isEmpty() && !currentMyHandCards.isEmpty()) {
            List<Card> fallback = List.of(currentMyHandCards.get(0));
            gameView.selectCards(fallback);
            gameView.setMatchStatus("⏰ Hết 60s: Hệ thống tự động đánh lá nhỏ nhất mở vòng!");
            handlePlayCards();
            return;
        }

        // 4. Nếu không có bài nào chặn được -> Tự động bỏ lượt
        gameView.setMatchStatus("⏰ Hết 60s: Bạn không có bài hợp lệ để chặn -> Tự động bỏ lượt.");
        handlePassTurn();
    }

    private void onClientTurnExpired() {
        autoPlaySuggestedCardsOrPass();
    }

    private void setupClientCallbacks() {
        client.setOnConnectSuccess(msg -> {
            lobbyView.setStatus("Kết nối Server thành công! Đang vào bàn...");
        });

        client.setOnRoomInfo(msg -> {
            showGameView();
            currentHostPlayerId = msg.getHostPlayerId();
            gameView.setRoomTitle("MÃ BÀN: " + msg.getRoomId());
            gameView.setPlayers(msg.getPlayers());
            gameView.setMatchStatus(msg.getPayload());
            gameView.setTurnMessage("ĐANG CHỜ NGƯỜI CHƠI...");
            gameView.setTurnActive(false);

            boolean isHost = client.getMyPlayerId() != null && client.getMyPlayerId().equals(msg.getHostPlayerId());
            gameView.updateWaitingStatus(isHost, msg.getPlayers().size(), msg.getMaxPlayers());
        });

        client.setOnGameStarted(msg -> {
            gameView.closeCelebrationDialog();
            if (msg.getHostPlayerId() != null) {
                currentHostPlayerId = msg.getHostPlayerId();
            }
            currentMyHandCards.clear();
            if (msg.getCards() != null) {
                currentMyHandCards.addAll(msg.getCards());
            }
            currentTableCards.clear();
            lastTurnProcessedKey = "";

            showGameView();
            gameView.hideStartGameButton();
            gameView.setHandCards(msg.getCards());
            gameView.setPlayedCards(List.of());
            gameView.setPlayers(msg.getPlayers());
            gameView.setTableStatus("Ván đấu đã bắt đầu! Đã tự động chia 13 lá bài cho mỗi người.");

            boolean isMyTurn = client.getMyPlayerId().equals(msg.getCurrentTurnPlayerId());
            if (isMyTurn) {
                applyMyTurn();
            } else {
                applyOpponentTurn(msg.getCurrentTurnPlayerName(), "đánh bài mở đầu");
            }

            int timeout = msg.getTurnTimeoutSeconds() > 0 ? msg.getTurnTimeoutSeconds() : 60;
            startClientTurnTimer(msg.getCurrentTurnPlayerId(), msg.getCurrentTurnPlayerName(), timeout);
        });

        client.setOnRoundUpdate(msg -> {
            currentTableCards.clear();
            if (msg.getCards() != null) {
                currentTableCards.addAll(msg.getCards());
            }

            gameView.setPlayedCards(msg.getCards());
            gameView.setPlayers(msg.getPlayers());

            if (myLastSelectedCards != null && !msg.getCards().isEmpty()
                    && msg.getCards().containsAll(myLastSelectedCards)) {
                gameView.removePlayedCards(myLastSelectedCards);
                currentMyHandCards.removeAll(myLastSelectedCards);
                gameView.clearSelectedCards();
                myLastSelectedCards = null;
            }

            if (msg.getPayload() != null && !msg.getPayload().isBlank()) {
                gameView.showSpecialEventBanner(msg.getPayload());
            } else if (!msg.getCards().isEmpty()) {
                String cardStr = msg.getCards().stream().map(Card::displayText).collect(Collectors.joining(" "));
                gameView.setTableStatus("Bài hiện tại trên bàn: [ " + cardStr + " ]");
            }

            boolean isMyTurn = client.getMyPlayerId().equals(msg.getCurrentTurnPlayerId());
            if (isMyTurn) {
                applyMyTurn();
            } else {
                applyOpponentTurn(msg.getCurrentTurnPlayerName(), "đánh bài");
            }

            int timeout = msg.getTurnTimeoutSeconds() > 0 ? msg.getTurnTimeoutSeconds() : 60;
            startClientTurnTimer(msg.getCurrentTurnPlayerId(), msg.getCurrentTurnPlayerName(), timeout);
        });

        client.setOnNewRound(msg -> {
            currentTableCards.clear();
            lastTurnProcessedKey = "";

            gameView.setPlayedCards(List.of());
            gameView.setPlayers(msg.getPlayers());
            gameView.setTableStatus("Vòng đánh mới! Bài trên bàn đã được thu dọn.");

            boolean isMyTurn = client.getMyPlayerId().equals(msg.getCurrentTurnPlayerId());
            if (isMyTurn) {
                applyMyTurn();
            } else {
                applyOpponentTurn(msg.getCurrentTurnPlayerName(), "mở vòng mới");
            }

            int timeout = msg.getTurnTimeoutSeconds() > 0 ? msg.getTurnTimeoutSeconds() : 60;
            startClientTurnTimer(msg.getCurrentTurnPlayerId(), msg.getCurrentTurnPlayerName(), timeout);
        });

        client.setOnTurnAlert(msg -> {
            applyMyTurn();
            int timeout = msg.getTurnTimeoutSeconds() > 0 ? msg.getTurnTimeoutSeconds() : 60;
            startClientTurnTimer(client.getMyPlayerId(), client.getMyPlayerName(), timeout);
        });

        client.setOnGameOver(msg -> {
            stopClientTurnTimer();
            gameView.setTurnActive(false);
            gameView.setPlayers(msg.getPlayers());
            gameView.setTurnMessage("🏆 VÁN ĐẤU KẾT THÚC");
            gameView.setMatchStatus(msg.getPayload());

            if (msg.getHostPlayerId() != null) {
                currentHostPlayerId = msg.getHostPlayerId();
            }

            boolean isWinner = client.getMyPlayerId() != null && client.getMyPlayerId().equals(msg.getPlayerId());
            String winnerName = msg.getPlayerName() != null ? msg.getPlayerName() : "Người chơi";

            gameView.showCongratulationsDialog(isWinner, winnerName, msg.getPayload(), () -> {
                // CHƠI TIẾP: Ở lại phòng, làm sạch bàn, chuẩn bị cho ván đấu mới
                currentMyHandCards.clear();
                currentTableCards.clear();
                lastTurnProcessedKey = "";
                gameView.setPlayedCards(List.of());
                gameView.setHandCards(List.of());
                gameView.setTurnMessage("CHỜ VÁN ĐẤU TIẾP THEO...");

                boolean isHost = client.getMyPlayerId() != null && client.getMyPlayerId().equals(currentHostPlayerId);
                int playerCount = msg.getPlayers() != null ? msg.getPlayers().size() : 2;
                gameView.updateWaitingStatus(isHost, playerCount, declaredPlayerCount);
                if (isHost) {
                    gameView.setMatchStatus("Bạn là chủ phòng. Bấm 'BẮT ĐẦU VÁN NGAY' khi mọi người đã sẵn sàng.");
                } else {
                    gameView.setMatchStatus("Đang ở trong phòng. Chờ chủ phòng bắt đầu ván đấu mới...");
                }
            }, () -> {
                // VỀ SẢNH
                handleLeaveRoom();
            });
        });

        client.setOnError(errMsg -> {
            if (rootPane.getChildren().contains(lobbyView)) {
                lobbyView.setBusy(false);
                lobbyView.setErrorStatus(errMsg);
            } else {
                gameView.showErrorAlert(errMsg);
                boolean isMyTurn = client.getMyPlayerId() != null && client.getMyPlayerId().equals(currentActiveTurnPlayerId);
                if (isMyTurn) {
                    updatePlayButtonAndSelectionState();
                    if (clientTurnTimeline == null && currentRemainingSeconds > 0) {
                        startClientTurnTimer(currentActiveTurnPlayerId, currentActiveTurnPlayerName, currentRemainingSeconds);
                    }
                }
            }
        });

        client.setOnDisconnected(() -> {
            stopClientTurnTimer();
            showLobbyView();
            lobbyView.setBusy(false);
            lobbyView.setStatus("Mất kết nối tới máy chủ.");
        });
    }

    private void showGameView() {
        if (!rootPane.getChildren().contains(gameView)) {
            rootPane.getChildren().setAll(gameView);
        }
    }

    private void showLobbyView() {
        if (!rootPane.getChildren().contains(lobbyView)) {
            rootPane.getChildren().setAll(lobbyView);
        }
    }
}
