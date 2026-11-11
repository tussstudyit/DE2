package com.example.tienlen.common.model;

public enum PlayerStatus {
    WAITING("Đang chờ"),
    PLAYING("Đang đánh"),
    PASSED("Đã bỏ lượt"),
    FINISHED("Đã hết bài"),
    WINNER("Về nhất");

    private final String displayName;

    PlayerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
