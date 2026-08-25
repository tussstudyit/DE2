package com.example.tienlen.client.view;

import com.example.tienlen.client.component.HandView;
import com.example.tienlen.client.component.PlayerView;
import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.model.Player;
import com.example.tienlen.common.model.PlayerPosition;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class GameView extends BorderPane {
    private final PlayerView topPlayerView = new PlayerView();
    private final PlayerView leftPlayerView = new PlayerView();
    private final PlayerView rightPlayerView = new PlayerView();
    private final PlayerView bottomPlayerView = new PlayerView();
    private final HandView currentHandView = new HandView(72, 104, true);
    private final HandView playedCardsView = new HandView(50, 72, false);
    private final Label turnMessageLabel = new Label("ĐẾN LƯỢT BẠN");
    private final Label matchStatusLabel = new Label("Trạng thái: Đang chờ thao tác.");
    private final Button playButton = new Button("ĐÁNH");
    private final Button passButton = new Button("BỎ LƯỢT");
    private final Map<PlayerPosition, PlayerView> playerViews = new EnumMap<>(PlayerPosition.class);

    public GameView() {
        getStyleClass().add("game-root");
        setPadding(new Insets(22));

        playerViews.put(PlayerPosition.TOP, topPlayerView);
        playerViews.put(PlayerPosition.LEFT, leftPlayerView);
        playerViews.put(PlayerPosition.RIGHT, rightPlayerView);
        playerViews.put(PlayerPosition.BOTTOM, bottomPlayerView);

        setTop(createTopArea());
        setLeft(createSideArea(leftPlayerView));
        setRight(createSideArea(rightPlayerView));
        setCenter(createTableArea());
        setBottom(createBottomArea());
    }

    private HBox createTopArea() {
        HBox topArea = new HBox(topPlayerView);
        topArea.setAlignment(Pos.CENTER);
        topArea.setMinHeight(120);
        topArea.setPrefHeight(132);
        return topArea;
    }

    private VBox createSideArea(PlayerView playerView) {
        VBox sideArea = new VBox(playerView);
        sideArea.setAlignment(Pos.CENTER);
        sideArea.setMinWidth(160);
        sideArea.setPrefWidth(172);
        return sideArea;
    }

    private StackPane createTableArea() {
        StackPane tableArea = new StackPane();
        tableArea.getStyleClass().add("table-area");
        tableArea.setPadding(new Insets(28));

        VBox playedCardsBox = new VBox(10, playedCardsView);
        playedCardsBox.setAlignment(Pos.CENTER);

        turnMessageLabel.getStyleClass().add("turn-message");
        StackPane.setAlignment(turnMessageLabel, Pos.TOP_CENTER);
        StackPane.setMargin(turnMessageLabel, new Insets(18, 0, 0, 0));

        tableArea.getChildren().addAll(playedCardsBox, turnMessageLabel);
        return tableArea;
    }

    private VBox createBottomArea() {
        VBox bottomArea = new VBox(10);
        bottomArea.setAlignment(Pos.CENTER);
        bottomArea.setPadding(new Insets(12, 0, 0, 0));

        HBox infoAndActions = new HBox(14);
        infoAndActions.setAlignment(Pos.CENTER_LEFT);

        matchStatusLabel.getStyleClass().add("match-status");
        HBox.setHgrow(matchStatusLabel, Priority.ALWAYS);

        playButton.getStyleClass().addAll("game-button", "primary-button");
        passButton.getStyleClass().addAll("game-button", "secondary-button");

        HBox actionButtons = new HBox(10, playButton, passButton);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        infoAndActions.getChildren().addAll(bottomPlayerView, matchStatusLabel, spacer, actionButtons);

        bottomArea.getChildren().addAll(infoAndActions, currentHandView);
        return bottomArea;
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
        currentHandView.setCards(cards);
    }

    public List<Card> getSelectedCards() {
        return currentHandView.getSelectedCards();
    }

    public void clearSelectedCards() {
        currentHandView.clearSelectedCards();
    }

    public void setPlayedCards(List<Card> cards) {
        playedCardsView.setCards(cards);
    }

    public void setTurnMessage(String message) {
        turnMessageLabel.setText(message);
    }

    public void setMatchStatus(String status) {
        matchStatusLabel.setText("Trạng thái: " + status);
    }

    public void setOnPlayRequested(EventHandler<ActionEvent> handler) {
        playButton.setOnAction(handler);
    }

    public void setOnPassRequested(EventHandler<ActionEvent> handler) {
        passButton.setOnAction(handler);
    }
}
