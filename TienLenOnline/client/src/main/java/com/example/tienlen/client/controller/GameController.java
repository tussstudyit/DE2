package com.example.tienlen.client.controller;

import com.example.tienlen.client.view.GameView;
import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.model.Player;
import com.example.tienlen.common.model.PlayerPosition;
import com.example.tienlen.common.model.PlayerStatus;
import java.util.List;
import java.util.stream.Collectors;

public class GameController {
    private final GameView view = new GameView();

    public GameController() {
        registerEvents();
        loadDemoGame(4);
    }

    public GameView getView() {
        return view;
    }

    private void registerEvents() {
        view.setOnPlayRequested(event -> handlePlayButton());
        view.setOnPassRequested(event -> handlePassButton());
    }

    private void handlePlayButton() {
        List<Card> selectedCards = view.getSelectedCards();
        if (selectedCards.isEmpty()) {
            view.setMatchStatus("Hãy chọn ít nhất một lá bài trước khi đánh.");
            view.setTurnMessage("ĐẾN LƯỢT BẠN");
            return;
        }

        view.setPlayedCards(selectedCards);
        view.clearSelectedCards();
        view.setMatchStatus("Bạn vừa đánh " + formatCards(selectedCards) + " (event giả lập).");
        view.setTurnMessage("ĐANG CHỜ NGƯỜI CHƠI KHÁC");
    }

    private void handlePassButton() {
        view.clearSelectedCards();
        view.setMatchStatus("Bạn vừa bỏ lượt (event giả lập).");
        view.setTurnMessage("ĐANG CHỜ NGƯỜI CHƠI KHÁC");
    }

    private void loadDemoGame(int playerCount) {
        view.setPlayers(createDemoPlayers(playerCount));
        view.setHandCards(createDemoHand());
        view.setPlayedCards(List.of(
                new Card("7", "♣"),
                new Card("7", "♥")
        ));
        view.setTurnMessage("ĐẾN LƯỢT BẠN");
        view.setMatchStatus("Dữ liệu giả đã sẵn sàng. Chưa có luật chơi hoặc networking.");
    }

    private List<Player> createDemoPlayers(int playerCount) {
        List<Player> players = List.of(
                new Player("p1", "Bạn", 11, PlayerPosition.BOTTOM, PlayerStatus.PLAYING),
                new Player("p2", "Player 2", 10, PlayerPosition.TOP, PlayerStatus.WAITING),
                new Player("p3", "Player 3", 9, PlayerPosition.LEFT, PlayerStatus.WAITING),
                new Player("p4", "Player 4", 8, PlayerPosition.RIGHT, PlayerStatus.WAITING)
        );
        return players.stream()
                .limit(Math.max(2, Math.min(playerCount, 4)))
                .toList();
    }

    private List<Card> createDemoHand() {
        return List.of(
                new Card("3", "♠"),
                new Card("4", "♥"),
                new Card("5", "♦"),
                new Card("7", "♣"),
                new Card("9", "♠"),
                new Card("10", "♥"),
                new Card("J", "♦"),
                new Card("Q", "♣"),
                new Card("K", "♠"),
                new Card("A", "♥"),
                new Card("2", "♦")
        );
    }

    private String formatCards(List<Card> cards) {
        return cards.stream()
                .map(Card::displayText)
                .collect(Collectors.joining(" "));
    }
}
