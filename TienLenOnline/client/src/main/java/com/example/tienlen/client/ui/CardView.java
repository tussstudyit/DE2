package com.example.tienlen.client.ui;

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

/** Lá bài tối giản, được vẽ hoàn toàn bằng JavaFX. */
public class CardView extends StackPane {
    private static final Set<String> RED_SUITS = Set.of("♥", "♦");

    private final Card card;
    private final Rectangle background = new Rectangle();
    private final Label topCorner = new Label();
    private final Label centerSuit = new Label();
    private final Label bottomCorner = new Label();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    private final double cardWidth;
    private final double cardHeight;
    private boolean selectable;

    public CardView(Card card) {
        this(card, 70, 100, true);
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
        Color cardColor = RED_SUITS.contains(card.getSuit())
                ? Color.web("#d93025")
                : Color.web("#182230");

        background.setFill(Color.WHITE);
        background.setStroke(Color.web("#d4dbe3"));
        background.setStrokeWidth(1.0);

        topCorner.setText(card.getRank() + "\n" + card.getSuit());
        topCorner.setTextFill(cardColor);
        topCorner.setAlignment(Pos.TOP_LEFT);

        centerSuit.setText(card.getSuit());
        centerSuit.setTextFill(cardColor);
        centerSuit.setAlignment(Pos.CENTER);

        bottomCorner.setText(card.getRank() + "\n" + card.getSuit());
        bottomCorner.setTextFill(cardColor);
        bottomCorner.setAlignment(Pos.BOTTOM_RIGHT);
        bottomCorner.setRotate(180);

        getChildren().addAll(background, centerSuit, topCorner, bottomCorner);
        StackPane.setAlignment(topCorner, Pos.TOP_LEFT);
        StackPane.setAlignment(centerSuit, Pos.CENTER);
        StackPane.setAlignment(bottomCorner, Pos.BOTTOM_RIGHT);
        setCursor(selectable ? Cursor.HAND : Cursor.DEFAULT);
    }

    private void updateSize() {
        setMinSize(cardWidth, cardHeight);
        setPrefSize(cardWidth, cardHeight);
        setMaxSize(cardWidth, cardHeight);

        background.setWidth(cardWidth);
        background.setHeight(cardHeight);
        background.setArcWidth(10);
        background.setArcHeight(10);

        double cornerFontSize = Math.max(11, cardWidth * 0.19);
        topCorner.setFont(Font.font("System", FontWeight.BOLD, cornerFontSize));
        bottomCorner.setFont(Font.font("System", FontWeight.BOLD, cornerFontSize));
        centerSuit.setFont(Font.font("System", FontWeight.NORMAL, Math.max(28, cardWidth * 0.43)));

        Insets cornerPadding = new Insets(cardHeight * 0.055, cardWidth * 0.09,
                cardHeight * 0.055, cardWidth * 0.09);
        StackPane.setMargin(topCorner, cornerPadding);
        StackPane.setMargin(bottomCorner, cornerPadding);
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
            if (selectable && !isSelected()) {
                setScaleX(hovering ? 1.025 : 1.0);
                setScaleY(hovering ? 1.025 : 1.0);
            }
        });
    }

    private void updateSelectionStyle() {
        setTranslateY(isSelected() ? -16 : 0);
        setScaleX(isSelected() ? 1.025 : 1.0);
        setScaleY(isSelected() ? 1.025 : 1.0);
        background.setStroke(isSelected() ? Color.web("#f4c542") : Color.web("#d4dbe3"));
        background.setStrokeWidth(isSelected() ? 2.4 : 1.0);
        background.setEffect(new DropShadow(isSelected() ? 13 : 6,
                Color.rgb(0, 0, 0, isSelected() ? 0.35 : 0.18)));
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
}
