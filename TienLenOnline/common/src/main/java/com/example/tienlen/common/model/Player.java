package com.example.tienlen.common.model;

public class Player {
    private String id;
    private String name;
    private int cardCount;
    private PlayerPosition position;
    private PlayerStatus status;

    public Player() {
    }

    public Player(String id, String name, int cardCount, PlayerPosition position, PlayerStatus status) {
        this.id = id;
        this.name = name;
        this.cardCount = cardCount;
        this.position = position;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCardCount() {
        return cardCount;
    }

    public void setCardCount(int cardCount) {
        this.cardCount = cardCount;
    }

    public PlayerPosition getPosition() {
        return position;
    }

    public void setPosition(PlayerPosition position) {
        this.position = position;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }
}
