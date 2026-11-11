package com.example.tienlen.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Objects;

public class Card implements Comparable<Card>, Serializable {
    private static final long serialVersionUID = 1L;

    private String rank;
    private String suit;

    public Card() {
    }

    public Card(String rank, String suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }

    @JsonIgnore
    public int getRankValue() {
        if (rank == null) return 0;
        return switch (rank.toUpperCase()) {
            case "3" -> 3;
            case "4" -> 4;
            case "5" -> 5;
            case "6" -> 6;
            case "7" -> 7;
            case "8" -> 8;
            case "9" -> 9;
            case "10" -> 10;
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            case "A" -> 14;
            case "2" -> 15;
            default -> 0;
        };
    }

    @JsonIgnore
    public int getSuitValue() {
        if (suit == null) return 0;
        return switch (suit) {
            case "♠", "S", "SPADE" -> 1;
            case "♣", "C", "CLUB" -> 2;
            case "♦", "D", "DIAMOND" -> 3;
            case "♥", "H", "HEART" -> 4;
            default -> 0;
        };
    }

    @JsonIgnore
    public boolean isHeo() {
        return getRankValue() == 15;
    }

    @JsonIgnore
    public boolean isBlackHeo() {
        return isHeo() && (getSuitValue() == 1 || getSuitValue() == 2);
    }

    @JsonIgnore
    public boolean isRedHeo() {
        return isHeo() && (getSuitValue() == 3 || getSuitValue() == 4);
    }

    public String displayText() {
        return rank + suit;
    }

    @Override
    public int compareTo(Card other) {
        if (other == null) return 1;
        if (this.getRankValue() != other.getRankValue()) {
            return Integer.compare(this.getRankValue(), other.getRankValue());
        }
        return Integer.compare(this.getSuitValue(), other.getSuitValue());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Card card)) {
            return false;
        }
        return Objects.equals(rank, card.rank) && Objects.equals(suit, card.suit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, suit);
    }

    @Override
    public String toString() {
        return displayText();
    }
}
