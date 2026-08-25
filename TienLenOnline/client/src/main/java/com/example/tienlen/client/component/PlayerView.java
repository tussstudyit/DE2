package com.example.tienlen.client.component;

import com.example.tienlen.common.model.Player;
import com.example.tienlen.common.model.PlayerStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PlayerView extends VBox {
    private final Circle avatar = new Circle(28);
    private final Label nameLabel = new Label();
    private final Label cardCountLabel = new Label();
    private final Label statusLabel = new Label();

    public PlayerView() {
        getStyleClass().add("player-view");
        setAlignment(Pos.CENTER);
        setSpacing(6);
        setMinSize(128, 132);
        setPrefSize(148, 142);
        setMaxSize(180, 160);

        avatar.getStyleClass().add("player-avatar");
        avatar.setFill(Color.web("#e6edf3"));

        nameLabel.getStyleClass().add("player-name");
        cardCountLabel.getStyleClass().add("player-card-count");
        statusLabel.getStyleClass().add("player-status");

        getChildren().addAll(avatar, nameLabel, cardCountLabel, statusLabel);
        setPlayer(null);
    }

    public void setPlayer(Player player) {
        if (player == null) {
            setVisible(false);
            nameLabel.setText("");
            cardCountLabel.setText("");
            statusLabel.setText("");
            return;
        }

        setVisible(true);
        nameLabel.setText(player.getName());
        cardCountLabel.setText(player.getCardCount() + " lá bài");
        PlayerStatus status = player.getStatus() == null ? PlayerStatus.WAITING : player.getStatus();
        statusLabel.setText(status.getDisplayName());
    }
}
