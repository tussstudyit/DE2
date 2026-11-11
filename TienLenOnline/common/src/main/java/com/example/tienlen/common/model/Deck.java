package com.example.tienlen.common.model;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quản lý bộ bài 52 lá tiêu chuẩn cho Tiến Lên Miền Nam.
 * Áp dụng thuật toán xáo bài Fisher-Yates đa vòng kết hợp SecureRandom (CSPRNG)
 * và kỹ thuật cắt bài ngẫu nhiên để đạt độ ngẫu nhiên tối đa, không thiên vị.
 */
public class Deck {
    public static final String[] RANKS = {"3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2"};
    public static final String[] SUITS = {"♠", "♣", "♦", "♥"};

    private final List<Card> cards = new ArrayList<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public Deck() {
        reset();
    }

    public void reset() {
        cards.clear();
        for (String rank : RANKS) {
            for (String suit : SUITS) {
                cards.add(new Card(rank, suit));
            }
        }
    }

    /**
     * Thuật toán xáo bài ngẫu nhiên tối đa:
     * - Chạy 3 vòng Fisher-Yates sử dụng nguồn entropy của SecureRandom
     * - Giữa các vòng thực hiện thao tác cắt bài (Cut Deck) tại điểm ngẫu nhiên
     */
    public void shuffle() {
        for (int pass = 0; pass < 3; pass++) {
            // Fisher-Yates shuffle
            for (int i = cards.size() - 1; i > 0; i--) {
                int j = secureRandom.nextInt(i + 1);
                Card temp = cards.get(i);
                cards.set(i, cards.get(j));
                cards.set(j, temp);
            }

            // Cắt bài ngẫu nhiên (Cut Deck) giữa các vòng xáo
            int cutPoint = 12 + secureRandom.nextInt(28); // Cắt từ vị trí 12 đến 40
            List<Card> top = new ArrayList<>(cards.subList(0, cutPoint));
            List<Card> bottom = new ArrayList<>(cards.subList(cutPoint, cards.size()));
            cards.clear();
            cards.addAll(bottom);
            cards.addAll(top);
        }
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /**
     * Chia 13 lá bài cho mỗi người chơi (từ 2 đến 4 người).
     * Các lá bài của mỗi người chơi được tự động sắp xếp theo thứ tự Tiến Lên.
     */
    public List<List<Card>> deal(int numPlayers) {
        if (numPlayers < 2 || numPlayers > 4) {
            throw new IllegalArgumentException("Số người chơi phải từ 2 đến 4 người: " + numPlayers);
        }

        reset();
        shuffle();

        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            hands.add(new ArrayList<>());
        }

        int cardsPerPlayer = 13;
        int cardIndex = 0;
        for (int round = 0; round < cardsPerPlayer; round++) {
            for (int p = 0; p < numPlayers; p++) {
                hands.get(p).add(cards.get(cardIndex++));
            }
        }

        // Tự động sắp xếp tăng dần cho từng bộ bài
        for (List<Card> hand : hands) {
            Collections.sort(hand);
        }

        return hands;
    }
}
