package com.example.tienlen.client.component;

import com.example.tienlen.common.model.Card;
import java.util.Set;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class CardView extends StackPane {
    private static final Set<String> RED_SUITS = Set.of("♥", "♦");

    private final Card card;
    private final Rectangle background = new Rectangle();
    private final Label topCorner = new Label();
    private final Label centerSuit = new Label();
    private final Label bottomCorner = new Label();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    private double cardWidth;
    private double cardHeight;
    private boolean selectable;

    public CardView(Card card) {
        this(card, 72, 104, true);
    }

    public CardView(Card card, double cardWidth, double cardHeight) {
        this(card, cardWidth, cardHeight, true);
    }

    public CardView(Card card, double cardWidth, double cardHeight, boolean selectable) {
        this.card = card;
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;
        this.selectable = selectable;

        getStyleClass().add("card-view");
        buildView();
        updateSize();
        updateSelectionStyle();
        registerEvents();
    }

    private void buildView() {
        background.setFill(Color.WHITE);
        background.setStroke(Color.web("#d4d8de"));
        background.setStrokeWidth(1.2);

        Color textColor = RED_SUITS.contains(card.getSuit()) ? Color.web("#d4202f") : Color.web("#15171a");

        topCorner.setText(card.getRank() + "\n" + card.getSuit());
        topCorner.setTextFill(textColor);
        topCorner.setAlignment(Pos.TOP_LEFT);

        centerSuit.setText(card.getSuit());
        centerSuit.setTextFill(textColor);
        centerSuit.setAlignment(Pos.CENTER);

        bottomCorner.setText(card.getRank() + "\n" + card.getSuit());
        bottomCorner.setTextFill(textColor);
        bottomCorner.setAlignment(Pos.BOTTOM_RIGHT);
        bottomCorner.setRotate(180);

        getChildren().addAll(background, centerSuit, topCorner, bottomCorner);
        StackPane.setAlignment(topCorner, Pos.TOP_LEFT);
        StackPane.setAlignment(centerSuit, Pos.CENTER);
        StackPane.setAlignment(bottomCorner, Pos.BOTTOM_RIGHT);
        setPickOnBounds(false);
        setCursor(selectable ? Cursor.HAND : Cursor.DEFAULT);
    }

    private void registerEvents() {
        selected.addListener((observable, oldValue, newValue) -> updateSelectionStyle());

        setOnMouseClicked(event -> {
            if (selectable) {
                setSelected(!isSelected());
                event.consume();
            }
        });

        hoverProperty().addListener((observable, oldValue, hovering) -> {
            if (!selectable) {
                return;
            }
            setScaleX(hovering ? 1.04 : 1.0);
            setScaleY(hovering ? 1.04 : 1.0);
        });
    }

    private void updateSize() {
        setMinSize(cardWidth, cardHeight + 20);
        setPrefSize(cardWidth, cardHeight + 20);
        setMaxSize(cardWidth, cardHeight + 20);

        background.setWidth(cardWidth);
        background.setHeight(cardHeight);
        background.setArcWidth(cardWidth * 0.14);
        background.setArcHeight(cardWidth * 0.14);

        double cornerFontSize = Math.max(12, cardWidth * 0.22);
        topCorner.setFont(Font.font("System", FontWeight.BOLD, cornerFontSize));
        bottomCorner.setFont(Font.font("System", FontWeight.BOLD, cornerFontSize));
        centerSuit.setFont(Font.font("System", FontWeight.BOLD, Math.max(24, cardWidth * 0.42)));

        Insets cornerPadding = new Insets(cardHeight * 0.06, cardWidth * 0.1, cardHeight * 0.06, cardWidth * 0.1);
        StackPane.setMargin(topCorner, cornerPadding);
        StackPane.setMargin(bottomCorner, cornerPadding);
    }

    private void updateSelectionStyle() {
        setTranslateY(isSelected() ? -18 : 0);
        background.setStroke(isSelected() ? Color.web("#ffd166") : Color.web("#d4d8de"));
        background.setStrokeWidth(isSelected() ? 3 : 1.2);
        background.setEffect(new DropShadow(isSelected() ? 18 : 10, Color.rgb(0, 0, 0, isSelected() ? 0.38 : 0.24)));
    }

    public Card getCard() {
        return card;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        setCursor(selectable ? Cursor.HAND : Cursor.DEFAULT);
        if (!selectable) {
            setSelected(false);
        }
    }

    public void setCardSize(double cardWidth, double cardHeight) {
        this.cardWidth = cardWidth;
        this.cardHeight = cardHeight;
        updateSize();
    }
}
