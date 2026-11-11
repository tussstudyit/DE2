package com.example.tienlen.common.rule;

import com.example.tienlen.common.model.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bộ luật Tiến Lên Miền Nam chuẩn.
 * Hỗ trợ nhận diện các kiểu bài: Rác, Đôi, Sám, Sảnh, Tứ quý, 3 đôi thông, 4 đôi thông
 * và kiểm tra tính hợp lệ của nước đánh đè lên bài trên bàn (bao gồm luật chặt heo).
 */
public class TienLenRule {

    /**
     * Nhận diện kiểu bài của danh sách lá bài.
     */
    public static CardCombinationType getCombinationType(List<Card> rawCards) {
        if (rawCards == null || rawCards.isEmpty()) {
            return CardCombinationType.INVALID;
        }

        List<Card> cards = new ArrayList<>(rawCards);
        Collections.sort(cards);
        int size = cards.size();

        // 1 lá: Rác
        if (size == 1) {
            return CardCombinationType.SINGLE;
        }

        // 2 lá: Đôi
        if (size == 2) {
            if (cards.get(0).getRankValue() == cards.get(1).getRankValue()) {
                return CardCombinationType.PAIR;
            }
            return CardCombinationType.INVALID;
        }

        // 3 lá: Sám cô hoặc Sảnh 3
        if (size == 3) {
            if (cards.get(0).getRankValue() == cards.get(1).getRankValue()
                    && cards.get(1).getRankValue() == cards.get(2).getRankValue()) {
                return CardCombinationType.TRIPLE;
            }
            if (isStraight(cards)) {
                return CardCombinationType.STRAIGHT;
            }
            return CardCombinationType.INVALID;
        }

        // 4 lá: Tứ quý hoặc Sảnh 4
        if (size == 4) {
            if (cards.get(0).getRankValue() == cards.get(1).getRankValue()
                    && cards.get(1).getRankValue() == cards.get(2).getRankValue()
                    && cards.get(2).getRankValue() == cards.get(3).getRankValue()) {
                return CardCombinationType.FOUR_OF_A_KIND;
            }
            if (isStraight(cards)) {
                return CardCombinationType.STRAIGHT;
            }
            return CardCombinationType.INVALID;
        }

        // 6 lá: Có thể là Sảnh 6 hoặc Ba đôi thông
        if (size == 6) {
            if (isConsecutivePairs(cards, 3)) {
                return CardCombinationType.THREE_CONSECUTIVE_PAIRS;
            }
            if (isStraight(cards)) {
                return CardCombinationType.STRAIGHT;
            }
            return CardCombinationType.INVALID;
        }

        // 8 lá: Có thể là Sảnh 8 hoặc Bốn đôi thông
        if (size == 8) {
            if (isConsecutivePairs(cards, 4)) {
                return CardCombinationType.FOUR_CONSECUTIVE_PAIRS;
            }
            if (isStraight(cards)) {
                return CardCombinationType.STRAIGHT;
            }
            return CardCombinationType.INVALID;
        }

        // Các kích thước còn lại (5, 7, 9, 10, 11, 12, 13 lá): Kiểm tra Sảnh
        if (isStraight(cards)) {
            return CardCombinationType.STRAIGHT;
        }

        return CardCombinationType.INVALID;
    }

    /**
     * Sảnh trong Tiến Lên Miền Nam: từ 3 lá trở lên, liên tiếp nhau theo thứ tự giá trị.
     * Đặc biệt: Quân 2 (Heo, rankValue = 15) KHÔNG ĐƯỢC nằm trong sảnh.
     */
    public static boolean isStraight(List<Card> sortedCards) {
        if (sortedCards.size() < 3) {
            return false;
        }
        // Kiểm tra không chứa heo
        for (Card card : sortedCards) {
            if (card.isHeo()) {
                return false;
            }
        }
        for (int i = 0; i < sortedCards.size() - 1; i++) {
            if (sortedCards.get(i + 1).getRankValue() - sortedCards.get(i).getRankValue() != 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Kiểm tra các đôi thông liên tiếp (ví dụ 3 đôi thông: 33-44-55).
     * Đặc biệt: Không chứa đôi heo.
     */
    public static boolean isConsecutivePairs(List<Card> sortedCards, int pairCount) {
        if (sortedCards.size() != pairCount * 2) {
            return false;
        }
        for (int i = 0; i < pairCount; i++) {
            Card c1 = sortedCards.get(i * 2);
            Card c2 = sortedCards.get(i * 2 + 1);
            if (c1.getRankValue() != c2.getRankValue()) {
                return false;
            }
            if (c1.isHeo()) {
                return false;
            }
            if (i > 0) {
                Card prevC1 = sortedCards.get((i - 1) * 2);
                if (c1.getRankValue() - prevC1.getRankValue() != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Kiểm tra nước đánh newCards có đè được tableCards không.
     * @return null nếu hợp lệ, chuỗi thông báo lỗi nếu không hợp lệ.
     */
    public static String validateMove(List<Card> rawNewCards, List<Card> rawTableCards) {
        if (rawNewCards == null || rawNewCards.isEmpty()) {
            return "Bạn chưa chọn lá bài nào!";
        }

        List<Card> newCards = new ArrayList<>(rawNewCards);
        Collections.sort(newCards);
        CardCombinationType newType = getCombinationType(newCards);

        if (newType == CardCombinationType.INVALID) {
            return "Tổ hợp các lá bài bạn chọn không hợp lệ!";
        }

        // Bàn trống (người đánh đầu ván hoặc người bắt đầu vòng mới)
        if (rawTableCards == null || rawTableCards.isEmpty()) {
            return null; // Hợp lệ
        }

        List<Card> tableCards = new ArrayList<>(rawTableCards);
        Collections.sort(tableCards);
        CardCombinationType tableType = getCombinationType(tableCards);

        Card newHighest = newCards.get(newCards.size() - 1);
        Card tableHighest = tableCards.get(tableCards.size() - 1);

        // Trường hợp 1: Cùng kiểu kết hợp
        if (newType == tableType) {
            // Sảnh phải cùng độ dài
            if (newType == CardCombinationType.STRAIGHT) {
                if (newCards.size() != tableCards.size()) {
                    return "Sảnh đánh ra phải có cùng số lượng lá bài với sảnh trên bàn (" + tableCards.size() + " lá)!";
                }
                if (newHighest.compareTo(tableHighest) > 0) {
                    return null;
                }
                return "Sảnh của bạn phải có lá bài cao nhất lớn hơn " + tableHighest.displayText() + "!";
            }

            // Tứ quý, Ba đôi thông, Bốn đôi thông, Đôi, Sám, Rác: So sánh lá bài cao nhất
            if (newCards.size() == tableCards.size()) {
                if (newHighest.compareTo(tableHighest) > 0) {
                    return null;
                }
                return "Bài của bạn không đủ lớn để đè bài trên bàn!";
            }
        }

        // Trường hợp 2: Luật Chặt bài đặc biệt (Chặt Heo & Hàng)
        // 1. Chặt 1 con Heo (tableType == SINGLE và tableHighest.isHeo())
        if (tableType == CardCombinationType.SINGLE && tableHighest.isHeo()) {
            if (newType == CardCombinationType.THREE_CONSECUTIVE_PAIRS
                    || newType == CardCombinationType.FOUR_OF_A_KIND
                    || newType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
                return null; // Chặt thành công!
            }
        }

        // 2. Chặt Đôi Heo (tableType == PAIR và tableHighest.isHeo())
        if (tableType == CardCombinationType.PAIR && tableHighest.isHeo()) {
            if (newType == CardCombinationType.FOUR_OF_A_KIND
                    || newType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
                return null; // Chặt đôi heo thành công!
            }
        }

        // 3. Chặt Ba đôi thông
        if (tableType == CardCombinationType.THREE_CONSECUTIVE_PAIRS) {
            if (newType == CardCombinationType.FOUR_OF_A_KIND
                    || newType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
                return null;
            }
        }

        // 4. Chặt Tứ quý
        if (tableType == CardCombinationType.FOUR_OF_A_KIND) {
            if (newType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
                return null;
            }
        }

        return "Bài bạn đánh (" + newType.getDisplayName() + ") không thể đè được (" + tableType.getDisplayName() + ") trên bàn!";
    }

    /**
     * Tìm tất cả các nước đánh hợp lệ mà người chơi có thể ra được từ bài trên tay
     * để đè lên bài trên bàn (hoặc tất cả các nước mở đầu nếu bàn đang trống).
     * Kết quả được sắp xếp theo thứ tự độ mạnh tăng dần (nước đi nhỏ nhất xếp trước).
     */
    public static List<List<Card>> findValidMoves(List<Card> rawHand, List<Card> rawTableCards) {
        if (rawHand == null || rawHand.isEmpty()) {
            return Collections.emptyList();
        }

        List<Card> hand = new ArrayList<>(rawHand);
        Collections.sort(hand);

        // Đếm số lượng quân theo rank để nhận diện bài rác lẻ, đôi, sám, tứ quý
        java.util.Map<Integer, Long> rankCounts = hand.stream()
                .collect(Collectors.groupingBy(Card::getRankValue, Collectors.counting()));

        List<List<Card>> candidates = new ArrayList<>();
        CardCombinationType tableType = (rawTableCards == null || rawTableCards.isEmpty())
                ? null
                : getCombinationType(rawTableCards);

        if (tableType == null) {
            // Bàn trống (Mở vòng mới):
            Card baBich = new Card("3", "♠");
            boolean hasBaBich = hand.contains(baBich);

            List<List<Card>> openMoves = new ArrayList<>();
            openMoves.addAll(generateSingles(hand));
            openMoves.addAll(generatePairs(hand));
            openMoves.addAll(generateTriples(hand));
            for (int len = 3; len <= Math.min(12, hand.size()); len++) {
                openMoves.addAll(generateStraights(hand, len));
            }
            openMoves.addAll(generateThreeConsecutivePairs(hand));
            openMoves.addAll(generateFourOfAKinds(hand));
            openMoves.addAll(generateFourConsecutivePairs(hand));

            if (hasBaBich) {
                // Nếu có 3 Bích (lượt đầu tiên ván mới), ưu tiên đưa các tổ hợp chứa 3 Bích lên trước
                List<List<Card>> baBichMoves = openMoves.stream().filter(m -> m.contains(baBich)).toList();
                List<List<Card>> otherMoves = openMoves.stream().filter(m -> !m.contains(baBich)).toList();
                candidates.addAll(baBichMoves);
                candidates.addAll(otherMoves);
            } else {
                candidates.addAll(openMoves);
            }
        } else {
            switch (tableType) {
                case SINGLE -> {
                    candidates.addAll(generateSingles(hand));
                    Card tableHighest = rawTableCards.get(rawTableCards.size() - 1);
                    if (tableHighest.isHeo()) {
                        candidates.addAll(generateThreeConsecutivePairs(hand));
                        candidates.addAll(generateFourOfAKinds(hand));
                        candidates.addAll(generateFourConsecutivePairs(hand));
                    }
                }
                case PAIR -> {
                    candidates.addAll(generatePairs(hand));
                    Card tableHighest = rawTableCards.get(rawTableCards.size() - 1);
                    if (tableHighest.isHeo()) {
                        candidates.addAll(generateFourOfAKinds(hand));
                        candidates.addAll(generateFourConsecutivePairs(hand));
                    }
                }
                case TRIPLE -> candidates.addAll(generateTriples(hand));
                case STRAIGHT -> candidates.addAll(generateStraights(hand, rawTableCards.size()));
                case THREE_CONSECUTIVE_PAIRS -> {
                    candidates.addAll(generateThreeConsecutivePairs(hand));
                    candidates.addAll(generateFourOfAKinds(hand));
                    candidates.addAll(generateFourConsecutivePairs(hand));
                }
                case FOUR_OF_A_KIND -> {
                    candidates.addAll(generateFourOfAKinds(hand));
                    candidates.addAll(generateFourConsecutivePairs(hand));
                }
                case FOUR_CONSECUTIVE_PAIRS -> candidates.addAll(generateFourConsecutivePairs(hand));
                default -> {}
            }
        }

        // Lọc lại các nước đi thực sự hợp lệ theo luật Tiến Lên Miền Nam
        List<List<Card>> validMoves = new ArrayList<>();
        for (List<Card> candidate : candidates) {
            if (validateMove(candidate, rawTableCards) == null) {
                validMoves.add(candidate);
            }
        }

        // Sắp xếp thông minh (Smart Hint Ranking - chuẩn Facebook & ZingPlay):
        // 1. Ưu tiên quân bài nhỏ nhất đè được bài trên bàn.
        // 2. Bảo vệ Heo: Không tự ý đẩy Heo nếu còn bài thường hợp lệ.
        // 3. Bảo vệ đôi/sám: Khi chặn rác lẻ, ưu tiên rác đơn lẻ (không xé đôi/sám).
        boolean tableHasHeo = rawTableCards != null && !rawTableCards.isEmpty()
                && rawTableCards.get(rawTableCards.size() - 1).isHeo();

        validMoves.sort((m1, m2) -> {
            int score1 = calculateMoveScore(m1, rankCounts, tableHasHeo);
            int score2 = calculateMoveScore(m2, rankCounts, tableHasHeo);
            if (score1 != score2) {
                return Integer.compare(score1, score2);
            }
            Card high1 = m1.get(m1.size() - 1);
            Card high2 = m2.get(m2.size() - 1);
            int comp = high1.compareTo(high2);
            if (comp != 0) return comp;
            return Integer.compare(m1.size(), m2.size());
        });

        return validMoves;
    }

    private static int calculateMoveScore(List<Card> move, java.util.Map<Integer, Long> rankCounts, boolean tableHasHeo) {
        int penalty = 0;
        Card highest = move.get(move.size() - 1);

        // Nếu bàn không đánh Heo mà dùng Heo -> Phạt để không phí Heo
        if (!tableHasHeo && highest.isHeo()) {
            penalty += 10000;
        }

        // Nếu là đánh rác lẻ (1 lá):
        if (move.size() == 1) {
            Card c = move.get(0);
            long count = rankCounts.getOrDefault(c.getRankValue(), 1L);
            if (count == 2) {
                penalty += 1000; // Xé đôi để đánh lẻ -> Phạt để ưu tiên rác đơn
            } else if (count >= 3) {
                penalty += 2000; // Xé sám/tứ quý để đánh lẻ -> Phạt nặng hơn
            }
        } else if (move.size() == 2) {
            Card c = move.get(0);
            long count = rankCounts.getOrDefault(c.getRankValue(), 2L);
            if (count >= 3) {
                penalty += 1000; // Xé sám để đánh đôi
            }
        }

        int baseScore = highest.getRankValue() * 10 + highest.getSuitValue();
        return penalty + baseScore;
    }

    /**
     * Kiểm tra xem người chơi có bất kỳ nước bài nào hợp lệ để đánh hay không.
     */
    public static boolean hasValidMove(List<Card> hand, List<Card> tableCards) {
        return !findValidMoves(hand, tableCards).isEmpty();
    }

    /**
     * Gợi ý nước đi nhỏ nhất, tối ưu nhất.
     */
    public static List<Card> suggestMove(List<Card> hand, List<Card> tableCards) {
        List<List<Card>> validMoves = findValidMoves(hand, tableCards);
        return validMoves.isEmpty() ? null : validMoves.get(0);
    }

    /**
     * Gợi ý nước đi xoay vòng qua các phương án hợp lệ.
     */
    public static List<Card> suggestNextMove(List<Card> hand, List<Card> tableCards, int cycleIndex) {
        List<List<Card>> validMoves = findValidMoves(hand, tableCards);
        if (validMoves.isEmpty()) return null;
        int idx = Math.abs(cycleIndex) % validMoves.size();
        return validMoves.get(idx);
    }

    public static List<List<Card>> generateSingles(List<Card> sortedHand) {
        List<List<Card>> result = new ArrayList<>();
        for (Card c : sortedHand) {
            result.add(List.of(c));
        }
        return result;
    }

    public static List<List<Card>> generatePairs(List<Card> sortedHand) {
        List<List<Card>> result = new ArrayList<>();
        for (int i = 0; i < sortedHand.size(); i++) {
            for (int j = i + 1; j < sortedHand.size(); j++) {
                Card c1 = sortedHand.get(i);
                Card c2 = sortedHand.get(j);
                if (c1.getRankValue() == c2.getRankValue()) {
                    result.add(List.of(c1, c2));
                }
            }
        }
        return result;
    }

    public static List<List<Card>> generateTriples(List<Card> sortedHand) {
        List<List<Card>> result = new ArrayList<>();
        for (int i = 0; i < sortedHand.size() - 2; i++) {
            for (int j = i + 1; j < sortedHand.size() - 1; j++) {
                for (int k = j + 1; k < sortedHand.size(); k++) {
                    Card c1 = sortedHand.get(i);
                    Card c2 = sortedHand.get(j);
                    Card c3 = sortedHand.get(k);
                    if (c1.getRankValue() == c2.getRankValue() && c2.getRankValue() == c3.getRankValue()) {
                        result.add(List.of(c1, c2, c3));
                    }
                }
            }
        }
        return result;
    }

    public static List<List<Card>> generateFourOfAKinds(List<Card> sortedHand) {
        List<List<Card>> result = new ArrayList<>();
        for (int i = 0; i <= sortedHand.size() - 4; i++) {
            if (sortedHand.get(i).getRankValue() == sortedHand.get(i + 1).getRankValue()
                    && sortedHand.get(i + 1).getRankValue() == sortedHand.get(i + 2).getRankValue()
                    && sortedHand.get(i + 2).getRankValue() == sortedHand.get(i + 3).getRankValue()) {
                result.add(List.of(sortedHand.get(i), sortedHand.get(i + 1), sortedHand.get(i + 2), sortedHand.get(i + 3)));
            }
        }
        return result;
    }

    public static List<List<Card>> generateStraights(List<Card> sortedHand, int length) {
        List<List<Card>> result = new ArrayList<>();
        if (length < 3 || sortedHand.size() < length) return result;

        java.util.Map<Integer, List<Card>> rankMap = new java.util.TreeMap<>();
        for (Card c : sortedHand) {
            if (!c.isHeo()) {
                rankMap.computeIfAbsent(c.getRankValue(), k -> new ArrayList<>()).add(c);
            }
        }

        List<Integer> ranks = new ArrayList<>(rankMap.keySet());
        for (int i = 0; i <= ranks.size() - length; i++) {
            boolean consecutive = true;
            for (int j = 0; j < length - 1; j++) {
                if (ranks.get(i + j + 1) - ranks.get(i + j) != 1) {
                    consecutive = false;
                    break;
                }
            }
            if (consecutive) {
                List<Card> straight = new ArrayList<>();
                for (int j = 0; j < length; j++) {
                    straight.add(rankMap.get(ranks.get(i + j)).get(0));
                }
                result.add(straight);
            }
        }
        return result;
    }

    public static List<List<Card>> generateThreeConsecutivePairs(List<Card> sortedHand) {
        return generateConsecutivePairs(sortedHand, 3);
    }

    public static List<List<Card>> generateFourConsecutivePairs(List<Card> sortedHand) {
        return generateConsecutivePairs(sortedHand, 4);
    }

    private static List<List<Card>> generateConsecutivePairs(List<Card> sortedHand, int pairCount) {
        List<List<Card>> result = new ArrayList<>();
        java.util.Map<Integer, List<Card>> pairMap = new java.util.TreeMap<>();
        for (int i = 0; i < sortedHand.size() - 1; i++) {
            Card c1 = sortedHand.get(i);
            Card c2 = sortedHand.get(i + 1);
            if (!c1.isHeo() && c1.getRankValue() == c2.getRankValue()) {
                pairMap.computeIfAbsent(c1.getRankValue(), k -> new ArrayList<>()).addAll(List.of(c1, c2));
                i++;
            }
        }

        List<Integer> pairRanks = new ArrayList<>(pairMap.keySet());
        for (int i = 0; i <= pairRanks.size() - pairCount; i++) {
            boolean consecutive = true;
            for (int j = 0; j < pairCount - 1; j++) {
                if (pairRanks.get(i + j + 1) - pairRanks.get(i + j) != 1) {
                    consecutive = false;
                    break;
                }
            }
            if (consecutive) {
                List<Card> combo = new ArrayList<>();
                for (int j = 0; j < pairCount; j++) {
                    List<Card> p = pairMap.get(pairRanks.get(i + j));
                    combo.add(p.get(0));
                    combo.add(p.get(1));
                }
                result.add(combo);
            }
        }
        return result;
    }
}
