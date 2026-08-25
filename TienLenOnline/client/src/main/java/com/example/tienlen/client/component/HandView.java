package com.example.tienlen.client.component;

import com.example.tienlen.common.model.Card;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

public class HandView extends HBox {
    private final List<CardView> cardViews = new ArrayList<>();
    private final double cardWidth;
    private final double cardHeight;
    private final boolean selectable;

    public HandView(double cardWidth, double cardHeight, boolean selectable) {
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;
        this.selectable = selectable;

        getStyleClass().add("hand-view");
        setAlignment(Pos.CENTER);
        setSpacing(8);
        setMinHeight(cardHeight + 28);
        setPrefHeight(cardHeight + 28);
    }

    public void setCards(Collection<Card> cards) {
        cardViews.clear();
        getChildren().clear();

        for (Card card : cards) {
            CardView cardView = new CardView(card, cardWidth, cardHeight, selectable);
            cardViews.add(cardView);
            getChildren().add(cardView);
        }
    }

    public List<Card> getSelectedCards() {
        return cardViews.stream()
                .filter(CardView::isSelected)
                .map(CardView::getCard)
                .toList();
    }

    public void clearSelectedCards() {
        cardViews.forEach(cardView -> cardView.setSelected(false));
    }
}
