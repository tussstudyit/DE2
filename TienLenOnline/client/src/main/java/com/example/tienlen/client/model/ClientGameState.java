package com.example.tienlen.client.model;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.model.Player;
import java.util.ArrayList;
import java.util.List;

public class ClientGameState {
    private final List<Player> players = new ArrayList<>();
    private final List<Card> handCards = new ArrayList<>();
    private final List<Card> playedCards = new ArrayList<>();

    public List<Player> getPlayers() {
        return players;
    }

    public List<Card> getHandCards() {
        return handCards;
    }

    public List<Card> getPlayedCards() {
        return playedCards;
    }
}
