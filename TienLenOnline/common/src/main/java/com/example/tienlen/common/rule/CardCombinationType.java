package com.example.tienlen.common.rule;

public enum CardCombinationType {
    INVALID("Không hợp lệ"),
    SINGLE("Rác"),
    PAIR("Đôi"),
    TRIPLE("Sám cô"),
    STRAIGHT("Sảnh"),
    THREE_CONSECUTIVE_PAIRS("Ba đôi thông"),
    FOUR_OF_A_KIND("Tứ quý"),
    FOUR_CONSECUTIVE_PAIRS("Bốn đôi thông");

    private final String displayName;

    CardCombinationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
