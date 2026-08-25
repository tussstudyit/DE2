package com.example.tienlen.common.model;

public enum PlayerStatus {
    PLAYING("Đang đánh"),
    WAITING("Đang chờ");

    private final String displayName;

    PlayerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
