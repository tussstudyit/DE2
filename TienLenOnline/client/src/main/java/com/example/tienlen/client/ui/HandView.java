package com.example.tienlen.client.ui;

import com.example.tienlen.common.model.Card;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        setSpacing(selectable ? -24 : 7);
        setMinHeight(cardHeight + (selectable ? 24 : 12));
        setPrefHeight(cardHeight + (selectable ? 24 : 12));
    }

    private Runnable onSelectionChanged;
    private boolean updatingSelection = false;

    public void setOnSelectionChanged(Runnable onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    private void notifySelectionChanged() {
        if (!updatingSelection && onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }

    public void setCards(Collection<Card> cards) {
        cardViews.clear();
        getChildren().clear();

        for (Card card : cards) {
            CardView cardView = new CardView(card, cardWidth, cardHeight, selectable);
            cardView.selectedProperty().addListener((obs, oldVal, newVal) -> notifySelectionChanged());
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
        updatingSelection = true;
        try {
            cardViews.forEach(cardView -> cardView.setSelected(false));
        } finally {
            updatingSelection = false;
        }
        notifySelectionChanged();
    }

    public void selectCards(Collection<Card> cardsToSelect) {
        updatingSelection = true;
        try {
            cardViews.forEach(cardView -> cardView.setSelected(false));
            if (cardsToSelect != null && !cardsToSelect.isEmpty()) {
                for (CardView cv : cardViews) {
                    if (cardsToSelect.contains(cv.getCard())) {
                        cv.setSelected(true);
                    }
                }
            }
        } finally {
            updatingSelection = false;
        }
        notifySelectionChanged();
    }
}
