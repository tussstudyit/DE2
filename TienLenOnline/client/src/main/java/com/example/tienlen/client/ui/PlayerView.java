package com.example.tienlen.client.ui;

import com.example.tienlen.common.model.Player;
import com.example.tienlen.common.model.PlayerStatus;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

/** Hiển thị thông tin người chơi theo dạng nhỏ gọn quanh bàn. */
public class PlayerView extends StackPane {
    private final boolean horizontal;
    private String playerId;
    private String playerName = "";
    private PlayerStatus currentStatus = PlayerStatus.WAITING;

    private final Circle avatarCircle = new Circle(24);
    private final Canvas timerCanvas = new Canvas(60, 60);
    private final Label avatarLabel = new Label("?");
    private final Label timerLabel = new Label();
    private final Label nameLabel = new Label();
    private final Label cardCountLabel = new Label();
    private final Label statusLabel = new Label();

    public PlayerView() {
        this(false);
    }

    public PlayerView(boolean horizontal) {
        this.horizontal = horizontal;
        getStyleClass().add("player-view");

        avatarCircle.getStyleClass().add("player-avatar");
        avatarLabel.getStyleClass().add("avatar-initial");
        timerLabel.getStyleClass().add("timer-label");
        timerLabel.setVisible(false);

        StackPane avatar = new StackPane(avatarCircle, timerCanvas, avatarLabel, timerLabel);
        avatar.setMinSize(60, 60);
        avatar.setPrefSize(60, 60);
        avatar.setMaxSize(60, 60);
        StackPane.setAlignment(timerLabel, Pos.BOTTOM_CENTER);
        timerLabel.setTranslateY(7);

        nameLabel.getStyleClass().add("player-name");
        cardCountLabel.getStyleClass().add("player-card-count");
        statusLabel.getStyleClass().add("player-status");

        VBox details = new VBox(1, nameLabel, cardCountLabel, statusLabel);
        details.setAlignment(horizontal ? Pos.CENTER_LEFT : Pos.CENTER);

        if (horizontal) {
            HBox content = new HBox(8, avatar, details);
            content.setAlignment(Pos.CENTER_LEFT);
            getChildren().add(content);
        } else {
            VBox content = new VBox(3, avatar, details);
            content.setAlignment(Pos.CENTER);
            getChildren().add(content);
        }
        setPlayer(null);
    }

    public void setPlayer(Player player) {
        if (player == null) {
            setVisible(false);
            setManaged(false);
            playerId = null;
            playerName = "";
            stopTurnTimer();
            return;
        }

        setVisible(true);
        setManaged(true);
        playerId = player.getId();
        playerName = player.getName();
        currentStatus = player.getStatus() == null ? PlayerStatus.WAITING : player.getStatus();

        nameLabel.setText(playerName);
        cardCountLabel.setText(player.getCardCount() + " lá");
        statusLabel.setText(currentStatus.getDisplayName());
        avatarLabel.setText(initials(playerName));
        applyStatusStyle();
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] words = name.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        }
        return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase();
    }

    private void applyStatusStyle() {
        avatarCircle.setOpacity(currentStatus == PlayerStatus.PASSED ? 0.55 : 1.0);
        if (currentStatus == PlayerStatus.WINNER) {
            statusLabel.setText("Về nhất");
            statusLabel.setTextFill(Color.web("#f4c542"));
            avatarCircle.setStroke(Color.web("#f4c542"));
        } else if (currentStatus == PlayerStatus.PLAYING) {
            statusLabel.setTextFill(Color.web("#e6f7ec"));
            avatarCircle.setStroke(Color.web("#65c985"));
        } else if (currentStatus == PlayerStatus.PASSED) {
            statusLabel.setTextFill(Color.web("#d9dfe5"));
            avatarCircle.setStroke(Color.web("#aab5bf"));
        } else {
            statusLabel.setTextFill(Color.web("#d9e6dd"));
            avatarCircle.setStroke(Color.web("#cad6ce"));
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setTurnTimer(int remainingSeconds, int totalSeconds) {
        if (remainingSeconds <= 0 || totalSeconds <= 0) {
            stopTurnTimer();
            return;
        }
        timerLabel.setText(remainingSeconds + "s");
        timerLabel.setVisible(true);
        drawTimer(remainingSeconds, totalSeconds);
        statusLabel.setText("Đang đánh");
        statusLabel.setTextFill(Color.web("#eaf6ed"));
        avatarCircle.setStroke(Color.web("#65c985"));
    }

    private void drawTimer(int remainingSeconds, int totalSeconds) {
        GraphicsContext graphics = timerCanvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, 60, 60);
        graphics.setStroke(Color.rgb(255, 255, 255, 0.25));
        graphics.setLineWidth(3);
        graphics.strokeOval(5, 5, 50, 50);
        graphics.setStroke(remainingSeconds <= 10 ? Color.web("#ef6b62") : Color.web("#65c985"));
        graphics.setLineWidth(3.5);
        graphics.setLineCap(StrokeLineCap.ROUND);
        double ratio = Math.max(0, Math.min(1, (double) remainingSeconds / totalSeconds));
        graphics.strokeArc(5, 5, 50, 50, 90, -360 * ratio, ArcType.OPEN);
    }

    public void stopTurnTimer() {
        timerCanvas.getGraphicsContext2D().clearRect(0, 0, 60, 60);
        timerLabel.setVisible(false);
        if (currentStatus != null) {
            statusLabel.setText(currentStatus.getDisplayName());
            applyStatusStyle();
        }
    }
}
