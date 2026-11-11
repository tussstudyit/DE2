package com.example.tienlen.common.rule;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.model.Deck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TienLenRuleTest {

    @Test
    public void testCardComparison() {
        Card baBich = new Card("3", "♠");
        Card baCo = new Card("3", "♥");
        Card heoBich = new Card("2", "♠");
        Card heoCo = new Card("2", "♥");

        assertTrue(baCo.compareTo(baBich) > 0, "3 Cơ phải lớn hơn 3 Bích");
        assertTrue(heoBich.compareTo(baCo) > 0, "2 Bích phải lớn hơn 3 Cơ");
        assertTrue(heoCo.compareTo(heoBich) > 0, "2 Cơ phải lớn hơn 2 Bích");
    }

    @Test
    public void testCombinationTypes() {
        // Rác
        assertEquals(CardCombinationType.SINGLE, TienLenRule.getCombinationType(List.of(new Card("5", "♠"))));

        // Đôi
        assertEquals(CardCombinationType.PAIR, TienLenRule.getCombinationType(List.of(new Card("7", "♠"), new Card("7", "♥"))));

        // Sám cô
        assertEquals(CardCombinationType.TRIPLE, TienLenRule.getCombinationType(List.of(
                new Card("9", "♠"), new Card("9", "♣"), new Card("9", "♦")
        )));

        // Sảnh hợp lệ (3 - 4 - 5)
        assertEquals(CardCombinationType.STRAIGHT, TienLenRule.getCombinationType(List.of(
                new Card("3", "♠"), new Card("4", "♣"), new Card("5", "♦")
        )));

        // Sảnh chứa Heo -> Không hợp lệ trong Tiến Lên Miền Nam
        assertEquals(CardCombinationType.INVALID, TienLenRule.getCombinationType(List.of(
                new Card("A", "♠"), new Card("2", "♣"), new Card("3", "♦")
        )));

        // Tứ quý
        assertEquals(CardCombinationType.FOUR_OF_A_KIND, TienLenRule.getCombinationType(List.of(
                new Card("10", "♠"), new Card("10", "♣"), new Card("10", "♦"), new Card("10", "♥")
        )));

        // Ba đôi thông (4-4, 5-5, 6-6)
        assertEquals(CardCombinationType.THREE_CONSECUTIVE_PAIRS, TienLenRule.getCombinationType(List.of(
                new Card("4", "♠"), new Card("4", "♥"),
                new Card("5", "♣"), new Card("5", "♦"),
                new Card("6", "♠"), new Card("6", "♦")
        )));
    }

    @Test
    public void testMoveValidationAndChop() {
        // Đôi 8 đè đôi 7
        List<Card> doi7 = List.of(new Card("7", "♠"), new Card("7", "♣"));
        List<Card> doi8 = List.of(new Card("8", "♠"), new Card("8", "♥"));
        assertNull(TienLenRule.validateMove(doi8, doi7), "Đôi 8 phải đè được đôi 7");

        // Tứ quý chặt 1 Heo
        List<Card> heo = List.of(new Card("2", "♥"));
        List<Card> tuQuyK = List.of(
                new Card("K", "♠"), new Card("K", "♣"), new Card("K", "♦"), new Card("K", "♥")
        );
        assertNull(TienLenRule.validateMove(tuQuyK, heo), "Tứ quý phải chặt được 2 Cơ");

        // Ba đôi thông chặt 1 Heo
        List<Card> baDoiThong = List.of(
                new Card("5", "♠"), new Card("5", "♥"),
                new Card("6", "♣"), new Card("6", "♦"),
                new Card("7", "♠"), new Card("7", "♦")
        );
        assertNull(TienLenRule.validateMove(baDoiThong, heo), "Ba đôi thông phải chặt được 2 Cơ");
    }

    @Test
    public void testDeckDeal() {
        Deck deck = new Deck();
        assertEquals(52, deck.getCards().size());

        // Chia cho 4 người
        List<List<Card>> hands4 = deck.deal(4);
        assertEquals(4, hands4.size());
        for (List<Card> hand : hands4) {
            assertEquals(13, hand.size(), "Mỗi người phải được chia 13 lá bài");
        }

        // Chia cho 2 người
        List<List<Card>> hands2 = deck.deal(2);
        assertEquals(2, hands2.size());
        for (List<Card> hand : hands2) {
            assertEquals(13, hand.size(), "Mỗi người phải được chia 13 lá bài");
        }
    }

    @Test
    public void testAutomaticTurnRecognitionAndSuggestions() {
        // Trường hợp 1: Trên bàn có Át Cơ (A♥), bài trên tay chỉ có bài nhỏ (3♠, 4♠, 5♠)
        // -> Không thể chặn -> hasValidMove = false, findValidMoves rỗng
        List<Card> tableCards = List.of(new Card("A", "♥"));
        List<Card> handNoMove = List.of(new Card("3", "♠"), new Card("4", "♠"), new Card("5", "♠"));

        assertFalse(TienLenRule.hasValidMove(handNoMove, tableCards), "Không có lá bài nào lớn hơn A Cơ");
        assertTrue(TienLenRule.findValidMoves(handNoMove, tableCards).isEmpty(), "Danh sách nước đi hợp lệ phải rỗng");
        assertNull(TienLenRule.suggestMove(handNoMove, tableCards), "Gợi ý nước đi phải trả về null khi không có bài chặn");

        // Trường hợp 2: Trên bàn có 5♠, bài trên tay có 4♠, 6♠, 10♦
        // -> Có thể chặn, gợi ý nước đi tối ưu (nhỏ nhất) phải là 6♠
        List<Card> table5Bich = List.of(new Card("5", "♠"));
        List<Card> handWithMove = List.of(new Card("4", "♠"), new Card("6", "♠"), new Card("10", "♦"));

        assertTrue(TienLenRule.hasValidMove(handWithMove, table5Bich));
        List<Card> suggested = TienLenRule.suggestMove(handWithMove, table5Bich);
        assertNotNull(suggested);
        assertEquals(1, suggested.size());
        assertEquals("6", suggested.get(0).getRank());
        assertEquals("♠", suggested.get(0).getSuit());

        // Trường hợp 3: Trên bàn có Heo Cơ (2♥), bài trên tay có Tứ Quý 9
        // -> Có thể chặt -> hasValidMove = true, gợi ý Tứ quý
        List<Card> tableHeoCo = List.of(new Card("2", "♥"));
        List<Card> handTuQuy9 = List.of(
                new Card("3", "♠"),
                new Card("9", "♠"), new Card("9", "♣"), new Card("9", "♦"), new Card("9", "♥"),
                new Card("K", "♠")
        );

        assertTrue(TienLenRule.hasValidMove(handTuQuy9, tableHeoCo), "Tứ quý 9 phải nhận diện được là có thể chặt Heo Cơ");
        List<Card> chopSuggestion = TienLenRule.suggestMove(handTuQuy9, tableHeoCo);
        assertNotNull(chopSuggestion);
        assertEquals(4, chopSuggestion.size());
        assertEquals("9", chopSuggestion.get(0).getRank());

        // Trường hợp 4: Vòng mới (bàn rỗng)
        // -> Có thể đánh bất kỳ bộ nào, gợi ý tối ưu bắt đầu từ lá/bộ nhỏ nhất
        List<Card> emptyTable = List.of();
        assertTrue(TienLenRule.hasValidMove(handWithMove, emptyTable));
        List<Card> firstMove = TienLenRule.suggestMove(handWithMove, emptyTable);
        assertNotNull(firstMove);
        assertEquals("4", firstMove.get(0).getRank());
    }

    @Test
    public void testDeckRandomness() {
        Deck deck1 = new Deck();
        deck1.shuffle();
        List<Card> cards1 = deck1.getCards();

        Deck deck2 = new Deck();
        deck2.shuffle();
        List<Card> cards2 = deck2.getCards();

        // Cả 2 bộ đều phải đủ 52 lá không trùng lặp
        assertEquals(52, cards1.stream().distinct().count());
        assertEquals(52, cards2.stream().distinct().count());

        // Xác suất 2 lần xáo bài SecureRandom 3-pass ra thứ tự y hệt nhau là xấp xỉ 1 / (52!) ~ 0
        boolean isIdentical = true;
        for (int i = 0; i < 52; i++) {
            if (!cards1.get(i).equals(cards2.get(i))) {
                isIdentical = false;
                break;
            }
        }
        assertFalse(isIdentical, "Hai lần xáo bài bằng SecureRandom không được trùng thứ tự nhau");
    }

    @Test
    public void testSmartCardPreservationAndOpeningMove() {
        // Test 1: Bảo toàn đôi (Không xé đôi khi có rác đơn lẻ)
        // Bài có đôi 8 (8♠ 8♥) và rác 9♣. Bàn đánh 7♠.
        // Gợi ý thông minh phải chọn 9♣ để không phá vỡ đôi 8!
        List<Card> table7Bich = List.of(new Card("7", "♠"));
        List<Card> handWithPair = List.of(new Card("8", "♠"), new Card("8", "♥"), new Card("9", "♣"));

        List<Card> move1 = TienLenRule.suggestMove(handWithPair, table7Bich);
        assertNotNull(move1);
        assertEquals(1, move1.size());
        assertEquals("9", move1.get(0).getRank(), "Gợi ý thông minh phải ưu tiên rác 9 để không xé đôi 8");

        // Test 2: Bảo vệ Heo (Không phí phạm Heo nếu còn bài thường)
        // Bài có rác 9♣ và Heo 2♠. Bàn đánh 7♠.
        // Gợi ý thông minh phải chọn 9♣, không được đẩy 2♠
        List<Card> handWithHeo = List.of(new Card("9", "♣"), new Card("2", "♠"));
        List<Card> move2 = TienLenRule.suggestMove(handWithHeo, table7Bich);
        assertNotNull(move2);
        assertEquals("9", move2.get(0).getRank(), "Không được tự ý đẩy Heo khi còn bài thường");

        // Test 3: Bàn trống và tay có 3 Bích
        // Gợi ý đầu tiên phải chứa 3 Bích
        List<Card> handWith3Bich = List.of(new Card("3", "♠"), new Card("6", "♦"), new Card("K", "♥"));
        List<Card> move3 = TienLenRule.suggestMove(handWith3Bich, List.of());
        assertNotNull(move3);
        assertTrue(move3.contains(new Card("3", "♠")), "Lượt mở màn ván có 3 Bích phải ưu tiên gợi ý chứa 3 Bích");
    }

    @Test
    public void testValidateMoveInvalidCombinations() {
        // 1. Rỗng
        assertNotNull(TienLenRule.validateMove(List.of(), List.of()));

        // 2. Tổ hợp không hợp lệ (ví dụ: 3♠ và 7♥ không phải đôi hay sảnh)
        List<Card> invalidCombo = List.of(new Card("3", "♠"), new Card("7", "♥"));
        String errCombo = TienLenRule.validateMove(invalidCombo, List.of());
        assertNotNull(errCombo, "Hai lá rác không tạo thành tổ hợp hợp lệ");
        assertTrue(errCombo.contains("không hợp lệ"));

        // 3. Đôi nhỏ đè đôi lớn
        List<Card> doi9 = List.of(new Card("9", "♠"), new Card("9", "♦"));
        List<Card> doi5 = List.of(new Card("5", "♠"), new Card("5", "♥"));
        String errSmallerPair = TienLenRule.validateMove(doi5, doi9);
        assertNotNull(errSmallerPair, "Đôi 5 không thể đè được Đôi 9");
        assertTrue(errSmallerPair.contains("không đủ lớn"));

        // 4. Khác kiểu (ví dụ đánh 1 lá Rác khi bàn đang đánh Đôi)
        List<Card> singleHeo = List.of(new Card("2", "♥"));
        String errDiffType = TienLenRule.validateMove(singleHeo, doi9);
        assertNotNull(errDiffType, "1 lá bài rác không thể đè được Đôi");
        assertTrue(errDiffType.contains("không thể đè được"));

        // 5. Sảnh khác độ dài
        List<Card> straight3 = List.of(new Card("3", "♠"), new Card("4", "♣"), new Card("5", "♦"));
        List<Card> straight4 = List.of(new Card("6", "♠"), new Card("7", "♣"), new Card("8", "♦"), new Card("9", "♥"));
        String errStraightLen = TienLenRule.validateMove(straight3, straight4);
        assertNotNull(errStraightLen, "Sảnh 3 lá không đè được Sảnh 4 lá");
        assertTrue(errStraightLen.contains("cùng số lượng lá bài"));
    }
}
