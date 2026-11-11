package com.example.tienlen.client.ui;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.model.Player;
import com.example.tienlen.common.model.PlayerPosition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.effect.DropShadow;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.*;

/**
 * Giao diện chính của bàn chơi bài Tiến Lên.
 * Thiết kế chia rõ rệt thành 2 phần theo yêu cầu:
 * - PHẦN TRÊN: Hiển thị các lá bài đã đánh của tất cả người chơi và thông tin đối thủ.
 * - PHẦN DƯỚI: Hiển thị các lá bài chưa đánh trên tay của người chơi hiện tại và các nút điều khiển.
 * - CẢNH BÁO: Hiển thị nổi bật cảnh báo khi đến lượt đánh.
 */
public class GameView extends BorderPane {
    private final PlayerView topPlayerView = new PlayerView(false);
    private final PlayerView leftPlayerView = new PlayerView(false);
    private final PlayerView rightPlayerView = new PlayerView(false);
    private final PlayerView bottomPlayerView = new PlayerView(true);
    private final Map<PlayerPosition, PlayerView> playerViews = new EnumMap<>(PlayerPosition.class);

    // PHẦN TRÊN: Bài đã đánh trên bàn
    private final HandView playedCardsView = new HandView(54, 78, false);
    private final Label tableStatusLabel = new Label("Bàn chơi trống. Người đánh đầu vòng có thể ra bài tùy ý.");
    private final Label roomTitleLabel = new Label("PHÒNG CHƠI TIẾN LÊN");

    // Banner thông báo sự kiện chặt bài / chặt hàng vui nhộn
    private final Label specialEventLabel = new Label();
    private final HBox specialEventBox = new HBox(specialEventLabel);
    private SequentialTransition specialEventAnimation;

    // PHẦN DƯỚI: Bài chưa đánh của người chơi hiện tại & Cảnh báo lượt đánh
    private final HandView currentHandView = new HandView(74, 108, true);
    private final Label turnAlertLabel = new Label("ĐANG CHỜ BẮT ĐẦU VÁN");
    private final Label matchStatusLabel = new Label("Trạng thái: Đang kết nối...");
    private final Button playButton = new Button("ĐÁNH BÀI");
    private final Button passButton = new Button("BỎ LƯỢT");
    private final Button hintButton = new Button("💡 GỢI Ý");
    private final Button sortButton = new Button("XẾP BÀI");
    private final Button clearButton = new Button("BỎ CHỌN");
    private final Button leaveButton = new Button("RỜI BÀN");
    private final Button startGameButton = new Button("🚀 BẮT ĐẦU VÁN NGAY");

    private final List<Card> currentHandCards = new ArrayList<>();

    public GameView() {
        getStyleClass().add("game-root");
        setPadding(new Insets(16, 22, 16, 22));

        playerViews.put(PlayerPosition.TOP, topPlayerView);
        playerViews.put(PlayerPosition.LEFT, leftPlayerView);
        playerViews.put(PlayerPosition.RIGHT, rightPlayerView);
        playerViews.put(PlayerPosition.BOTTOM, bottomPlayerView);

        setTop(createHeaderAndTopArea());
        setLeft(createSideArea(leftPlayerView));
        setRight(createSideArea(rightPlayerView));
        setCenter(createCenterTableArea());
        setBottom(createBottomArea());

        setTurnActive(false);

        sortButton.setOnAction(e -> sortHandCards());
        clearButton.setOnAction(e -> clearSelectedCards());
    }

    private VBox createHeaderAndTopArea() {
        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);

        HBox navBar = new HBox(16);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(4, 10, 8, 10));

        roomTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        roomTitleLabel.setTextFill(Color.web("#f8d66d"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        leaveButton.getStyleClass().addAll("game-button", "secondary-button");
        leaveButton.setStyle("-fx-font-size: 12px; -fx-padding: 6 14 6 14;");

        navBar.getChildren().addAll(roomTitleLabel, spacer, leaveButton);

        HBox topPlayerBox = new HBox(topPlayerView);
        topPlayerBox.setAlignment(Pos.CENTER);
        topPlayerBox.setMinHeight(140);

        topBox.getChildren().addAll(navBar, topPlayerBox);
        return topBox;
    }

    private VBox createSideArea(PlayerView playerView) {
        VBox sideArea = new VBox(playerView);
        sideArea.setAlignment(Pos.CENTER);
        sideArea.setMinWidth(140);
        sideArea.setPrefWidth(150);
        return sideArea;
    }

    private StackPane createCenterTableArea() {
        StackPane tableArea = new StackPane();
        tableArea.getStyleClass().add("table-area");
        tableArea.setPadding(new Insets(20));

        VBox contentBox = new VBox(12);
        contentBox.setAlignment(Pos.CENTER);

        Label tableHeader = new Label("CÁC LÁ BÀI ĐÃ ĐÁNH TRÊN BÀN");
        tableHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        tableHeader.setTextFill(Color.web("#a5d6a7"));

        // Thiết lập Banner thông báo chặt bài / chặt hàng vui nhộn
        specialEventBox.setAlignment(Pos.CENTER);
        specialEventBox.setMaxWidth(Region.USE_PREF_SIZE);
        specialEventBox.setMaxHeight(Region.USE_PREF_SIZE);
        specialEventBox.setStyle("-fx-background-color: linear-gradient(to right, #b91c1c, #d97706, #b91c1c); -fx-background-radius: 20; -fx-padding: 8 22; -fx-border-color: #fde047; -fx-border-width: 2; -fx-border-radius: 20;");
        specialEventBox.setEffect(new DropShadow(22, Color.rgb(234, 179, 8, 0.9)));
        specialEventLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        specialEventLabel.setTextFill(Color.WHITE);
        specialEventLabel.setWrapText(true);
        specialEventLabel.setAlignment(Pos.CENTER);
        specialEventBox.setVisible(false);
        specialEventBox.setManaged(false);

        contentBox.getChildren().addAll(tableHeader, specialEventBox, playedCardsView, startGameButton);
        startGameButton.setVisible(false);
        startGameButton.setManaged(false);
        startGameButton.getStyleClass().addAll("game-button", "primary-button");
        tableArea.getChildren().add(contentBox);
        return tableArea;
    }

    private VBox createBottomArea() {
        VBox bottomArea = new VBox(10);
        bottomArea.setAlignment(Pos.CENTER);
        bottomArea.setPadding(new Insets(10, 0, 0, 0));

        VBox handContainer = new VBox(6);
        handContainer.setAlignment(Pos.CENTER);
        handContainer.getChildren().add(currentHandView);

        HBox actionBox = new HBox(12);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        playButton.getStyleClass().addAll("game-button", "primary-button");
        passButton.getStyleClass().addAll("game-button", "secondary-button");
        hintButton.getStyleClass().addAll("game-button", "secondary-button");
        hintButton.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffd166;");
        sortButton.getStyleClass().addAll("game-button", "secondary-button");
        clearButton.getStyleClass().addAll("game-button", "secondary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionBox.getChildren().addAll(bottomPlayerView, spacer, hintButton, sortButton, clearButton, passButton, playButton);

        bottomArea.getChildren().addAll(handContainer, actionBox);
        return bottomArea;
    }

    public void setOnHandSelectionChanged(Runnable handler) {
        currentHandView.setOnSelectionChanged(handler);
    }

    public void setPlayButtonState(boolean enabled, boolean lit) {
        playButton.setDisable(!enabled);
        if (enabled && lit) {
            playButton.setStyle("-fx-background-color: #2ea44f; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 20; -fx-effect: dropshadow(three-pass-box, rgba(46,164,79,0.85), 14, 0, 0, 0); -fx-cursor: hand;");
        } else {
            // Không sáng: màu tối mờ, không đổ bóng neon, không thể nhấn
            playButton.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 20; -fx-opacity: 0.65; -fx-cursor: default;");
        }
    }

    public void setPassButtonState(boolean enabled, boolean highlighted) {
        passButton.setDisable(!enabled);
        if (!enabled) {
            passButton.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #64748b; -fx-font-size: 13px; -fx-padding: 8 18; -fx-opacity: 0.6; -fx-cursor: default;");
        } else if (highlighted) {
            passButton.setStyle("-fx-background-color: #e65100; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 18; -fx-effect: dropshadow(three-pass-box, rgba(230,81,0,0.9), 12, 0, 0, 0); -fx-cursor: hand;");
        } else {
            passButton.setStyle("-fx-cursor: hand;");
        }
    }

    public void setTurnActive(boolean isMyTurn) {
        setTurnActive(isMyTurn, true);
    }

    public void setTurnActive(boolean isMyTurn, boolean hasValidMoves) {
        if (!isMyTurn) {
            setPlayButtonState(false, false);
            setPassButtonState(false, false);
            hintButton.setDisable(true);
            return;
        }

        hintButton.setDisable(!hasValidMoves);

        if (!hasValidMoves) {
            // Tự động nhận diện: Người chơi KHÔNG có bài hợp lệ để chặn
            setPlayButtonState(false, false);
            setPassButtonState(true, true);
            matchStatusLabel.setText("Hệ thống nhận diện: Bạn không có bài hợp lệ để đè bài trên bàn. Hãy bấm BỎ LƯỢT.");
        } else {
            // Có nước đi trong tay, nhưng nút Đánh bài chỉ sáng khi người chơi đã chọn nước đi hợp lệ
            setPassButtonState(true, false);
        }
    }

    public void setTurnBannerForMe(int remainingSeconds, boolean hasValidMoves) {
        if (!hasValidMoves) {
            turnAlertLabel.setText("⚠️ BẠN KHÔNG CÓ BÀI ĐỂ CHẶN [ " + remainingSeconds + "s ] -> BẤM 'BỎ LƯỢT'");
            turnAlertLabel.setStyle("-fx-background-color: #e65100; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 7 20; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(230,81,0,0.85), 14, 0, 0, 0);");
        } else if (remainingSeconds <= 10) {
            turnAlertLabel.setText("🚨 SẮP HẾT GIỜ! ĐẾN LƯỢT BẠN ĐÁNH BÀI [ " + remainingSeconds + "s ] 🚨");
            turnAlertLabel.setStyle("-fx-background-color: #ff3b30; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 7 22; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(255,59,48,0.95), 16, 0, 0, 0);");
        } else {
            turnAlertLabel.setText("🚨 ĐẾN LƯỢT BẠN ĐÁNH BÀI! [ " + remainingSeconds + "s ] 🚨");
            turnAlertLabel.setStyle("-fx-background-color: #2ea44f; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 7 22; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(46,164,79,0.85), 14, 0, 0, 0);");
        }
    }

    public void setTurnBannerForOpponent(String opponentName, int remainingSeconds) {
        String opp = opponentName != null ? opponentName.toUpperCase() : "ĐỐI THỦ";
        if (remainingSeconds <= 10) {
            turnAlertLabel.setText("⏳ ĐANG CHỜ [" + opp + "] — SẮP HẾT GIỜ [ " + remainingSeconds + "s ]");
            turnAlertLabel.setStyle("-fx-background-color: #b91c1c; -fx-text-fill: #fee2e2; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 7 20; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(239,68,68,0.85), 14, 0, 0, 0);");
        } else {
            turnAlertLabel.setText("⏳ ĐANG CHỜ [" + opp + "] ĐÁNH BÀI [ " + remainingSeconds + "s ]");
            turnAlertLabel.setStyle("-fx-background-color: #1e3a8a; -fx-text-fill: #93c5fd; -fx-font-size: 15px; -fx-font-weight: bold; -fx-padding: 7 20; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.75), 12, 0, 0, 0);");
        }
    }

    public void setHintButtonText(String text) {
        hintButton.setText(text);
    }

    public boolean isPlayButtonEnabled() {
        return !playButton.isDisabled();
    }

    public boolean isPassButtonEnabled() {
        return !passButton.isDisabled();
    }

    public boolean isHintButtonEnabled() {
        return !hintButton.isDisabled();
    }

    public boolean isPassButtonHighlighted() {
        return passButton.getStyle() != null && passButton.getStyle().contains("#e65100");
    }

    public void setTurnMessage(String message) {
        turnAlertLabel.setText(message);
    }

    public void setMatchStatus(String status) {
        matchStatusLabel.setText("Trạng thái: " + status);
    }

    public void setTableStatus(String status) {
        tableStatusLabel.setText(status);
    }

    public void setRoomTitle(String title) {
        roomTitleLabel.setText(title);
    }

    public void setPlayers(List<Player> players) {
        playerViews.values().forEach(playerView -> playerView.setPlayer(null));
        for (Player player : players) {
            PlayerView playerView = playerViews.get(player.getPosition());
            if (playerView != null) {
                playerView.setPlayer(player);
            }
        }
    }

    public void setHandCards(List<Card> cards) {
        currentHandCards.clear();
        if (cards != null) {
            currentHandCards.addAll(cards);
        }
        sortHandCards();
    }

    public void removePlayedCards(List<Card> played) {
        for (Card c : played) {
            currentHandCards.remove(c);
        }
        currentHandView.setCards(currentHandCards);
    }

    public void sortHandCards() {
        Collections.sort(currentHandCards);
        currentHandView.setCards(currentHandCards);
    }

    public List<Card> getSelectedCards() {
        return currentHandView.getSelectedCards();
    }

    public void clearSelectedCards() {
        currentHandView.clearSelectedCards();
    }

    public void setPlayedCards(List<Card> cards) {
        playedCardsView.setCards(cards != null ? cards : Collections.emptyList());
    }

    private Dialog<ButtonType> activeCelebrationDialog;

    public void closeCelebrationDialog() {
        if (activeCelebrationDialog != null && activeCelebrationDialog.isShowing()) {
            activeCelebrationDialog.close();
            activeCelebrationDialog = null;
        }
    }

    public void showCongratulationsDialog(boolean isWinner, String winnerName, String summaryText, Runnable onPlayAgain, Runnable onReturnLobby) {
        closeCelebrationDialog();

        Dialog<ButtonType> dialog = new Dialog<>();
        activeCelebrationDialog = dialog;

        if (getScene() != null && getScene().getWindow() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(isWinner ? "🎉 CHÚC MỪNG CHIẾN THẮNG!" : "🏆 KẾT THÚC VÁN ĐẤU");

        VBox dialogLayout = new VBox(14);
        dialogLayout.setAlignment(Pos.CENTER);
        dialogLayout.setPadding(new Insets(20, 24, 16, 24));
        dialogLayout.setPrefWidth(440);

        // Biểu tượng cúp & Tiêu đề
        Label trophyIcon = new Label(isWinner ? "🏆" : "🎖️");
        trophyIcon.setFont(Font.font("Segoe UI Emoji", 48));

        Label mainTitle = new Label(isWinner ? "🎉 CHIẾN THẮNG XUẤT SẮC! 🎉" : "🏆 " + winnerName.toUpperCase() + " ĐÃ VỀ NHẤT!");
        mainTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        mainTitle.setTextFill(isWinner ? Color.web("#fbbf24") : Color.web("#f1f5f9"));
        mainTitle.setAlignment(Pos.CENTER);

        Label subTitle = new Label(isWinner ? "👑 Bạn đã đánh hết bài đầu tiên và giành ngôi Quán Quân!" : "Ván đấu kết thúc theo luật Nhất Ăn Tất.");
        subTitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        subTitle.setTextFill(Color.web("#94a3b8"));
        subTitle.setAlignment(Pos.CENTER);

        VBox headerBox = new VBox(4, trophyIcon, mainTitle, subTitle);
        headerBox.setAlignment(Pos.CENTER);

        // Bảng tổng kết ván đấu
        VBox summaryBox = new VBox(6);
        summaryBox.setPadding(new Insets(12, 16, 12, 16));
        summaryBox.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 8;");

        Label summaryHeader = new Label("📊 BẢNG TỔNG KẾT VÁN ĐẤU");
        summaryHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        summaryHeader.setTextFill(Color.web("#38bdf8"));
        summaryBox.getChildren().add(summaryHeader);

        if (summaryText != null && !summaryText.isBlank()) {
            String[] lines = summaryText.split("\n");
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("🏆") || line.startsWith("📊")) continue;
                Label lineLabel = new Label(line.trim());
                lineLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
                if (line.contains("VỀ NHẤT") || line.contains("🥇")) {
                    lineLabel.setTextFill(Color.web("#fbbf24"));
                    lineLabel.setStyle("-fx-font-weight: bold;");
                } else {
                    lineLabel.setTextFill(Color.web("#cbd5e1"));
                }
                summaryBox.getChildren().add(lineLabel);
            }
        }

        dialogLayout.getChildren().addAll(headerBox, summaryBox);

        ButtonType playAgainType = new ButtonType("🚀 CHƠI TIẾP", ButtonBar.ButtonData.OK_DONE);
        ButtonType returnLobbyType = new ButtonType("🚪 VỀ SẢNH", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().setContent(dialogLayout);
        dialog.getDialogPane().getButtonTypes().setAll(playAgainType, returnLobbyType);

        dialog.getDialogPane().setStyle("-fx-background-color: #1e293b; -fx-border-color: " + (isWinner ? "#fbbf24" : "#475569") + "; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

        dialog.setOnShowing(event -> {
            Button playAgainBtn = (Button) dialog.getDialogPane().lookupButton(playAgainType);
            if (playAgainBtn != null) {
                playAgainBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
            }
            Button returnLobbyBtn = (Button) dialog.getDialogPane().lookupButton(returnLobbyType);
            if (returnLobbyBtn != null) {
                returnLobbyBtn.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
            }
        });

        dialog.showAndWait().ifPresent(response -> {
            activeCelebrationDialog = null;
            if (response == playAgainType) {
                if (onPlayAgain != null) onPlayAgain.run();
            } else {
                if (onReturnLobby != null) onReturnLobby.run();
            }
        });
    }

    public void showGameOverDialog(String message, Runnable onOk) {
        showCongratulationsDialog(false, "Đối thủ", message, onOk, onOk);
    }

    public void showErrorAlert(String message) {
        matchStatusLabel.setText("⚠️ " + message);
        turnAlertLabel.setText("⚠️ " + message);
        tableStatusLabel.setText("⚠️ " + message);
        tableStatusLabel.setTextFill(Color.web("#ef4444"));
    }

    public void setOnPlayRequested(EventHandler<ActionEvent> handler) {
        playButton.setOnAction(handler);
    }

    public void setOnPassRequested(EventHandler<ActionEvent> handler) {
        passButton.setOnAction(handler);
    }

    public void updateWaitingStatus(boolean isHost, int currentPlayers, int maxPlayers) {
        if (isHost) {
            startGameButton.setVisible(true);
            startGameButton.setManaged(true);
            if (currentPlayers >= 2) {
                startGameButton.setDisable(false);
                startGameButton.setText("🚀 BẮT ĐẦU VÁN NGAY (" + currentPlayers + " người)");
                startGameButton.setStyle("-fx-background-color: #2ea44f; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 9 22; -fx-background-radius: 8;");
            } else {
                startGameButton.setDisable(true);
                startGameButton.setText("⏳ ĐANG CHỜ NGƯỜI CHƠI THỨ 2... (" + currentPlayers + "/" + maxPlayers + ")");
                startGameButton.setStyle("-fx-background-color: #444444; -fx-text-fill: #aaaaaa; -fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 8;");
            }
        } else {
            startGameButton.setVisible(false);
            startGameButton.setManaged(false);
        }
    }

    public void hideStartGameButton() {
        startGameButton.setVisible(false);
        startGameButton.setManaged(false);
    }

    public void setOnStartGameRequested(EventHandler<ActionEvent> handler) {
        startGameButton.setOnAction(handler);
    }

    public void selectCards(List<Card> cards) {
        currentHandView.selectCards(cards);
    }

    public void setOnHintRequested(EventHandler<ActionEvent> handler) {
        hintButton.setOnAction(handler);
    }

    public void setOnLeaveRequested(EventHandler<ActionEvent> handler) {
        leaveButton.setOnAction(handler);
    }

    public void updateTurnTimer(String activePlayerId, int remainingSeconds, int totalSeconds) {
        for (PlayerView pv : playerViews.values()) {
            if (activePlayerId != null && activePlayerId.equals(pv.getPlayerId())) {
                pv.setTurnTimer(remainingSeconds, totalSeconds);
            } else {
                pv.stopTurnTimer();
            }
        }
    }

    public void stopAllTurnTimers() {
        for (PlayerView pv : playerViews.values()) {
            pv.stopTurnTimer();
        }
    }

    public void showSpecialEventBanner(String message) {
        if (message == null || message.isBlank()) return;

        tableStatusLabel.setText(message);
        tableStatusLabel.setTextFill(Color.web("#fde047"));
        matchStatusLabel.setText("💥 " + message);

        specialEventLabel.setText(message);
        specialEventBox.setVisible(true);
        specialEventBox.setManaged(true);
        specialEventBox.setOpacity(1.0);
        specialEventBox.setScaleX(0.3);
        specialEventBox.setScaleY(0.3);

        if (specialEventAnimation != null) {
            specialEventAnimation.stop();
        }

        ScaleTransition popIn = new ScaleTransition(Duration.millis(280), specialEventBox);
        popIn.setToX(1.0);
        popIn.setToY(1.0);

        PauseTransition stay = new PauseTransition(Duration.seconds(4.2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), specialEventBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        specialEventAnimation = new SequentialTransition(popIn, stay, fadeOut);
        specialEventAnimation.setOnFinished(e -> {
            specialEventBox.setVisible(false);
            specialEventBox.setManaged(false);
            specialEventAnimation = null;
        });
        specialEventAnimation.play();
    }
}
