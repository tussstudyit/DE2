package com.example.tienlen.common.rule;

import com.example.tienlen.common.model.Card;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SpecialMoveDetectorTest {

    @Test
    public void testChatHeoWith3DoiThong() {
        List<Card> heo = List.of(new Card("2", "♠"));
        List<Card> baDoiThong = List.of(
                new Card("3", "♠"), new Card("3", "♥"),
                new Card("4", "♠"), new Card("4", "♥"),
                new Card("5", "♠"), new Card("5", "♥")
        );

        String msg = SpecialMoveDetector.detectSpecialEvent("Alice", baDoiThong, "Bob", heo);
        assertNotNull(msg);
        assertTrue(msg.contains("Alice"));
        assertTrue(msg.contains("Bob"));
        assertTrue(msg.contains("3 ĐÔI THÔNG"));
    }

    @Test
    public void testChatHeoWithTuQuy() {
        List<Card> heoDo = List.of(new Card("2", "♥"));
        List<Card> tuQuy = List.of(
                new Card("8", "♠"), new Card("8", "♣"),
                new Card("8", "♦"), new Card("8", "♥")
        );

        String msg = SpecialMoveDetector.detectSpecialEvent("PlayerA", tuQuy, "PlayerB", heoDo);
        assertNotNull(msg);
        assertTrue(msg.contains("PlayerA"));
        assertTrue(msg.contains("PlayerB"));
        assertTrue(msg.contains("TỨ QUÝ"));
    }

    @Test
    public void testChatDoiHeoWithTuQuy() {
        List<Card> doiHeo = List.of(new Card("2", "♠"), new Card("2", "♦"));
        List<Card> tuQuy = List.of(
                new Card("9", "♠"), new Card("9", "♣"),
                new Card("9", "♦"), new Card("9", "♥")
        );

        String msg = SpecialMoveDetector.detectSpecialEvent("AnhNam", tuQuy, "ChiLan", doiHeo);
        assertNotNull(msg);
        assertTrue(msg.contains("ĐÔI HEO"));
        assertTrue(msg.contains("TỨ QUÝ"));
    }

    @Test
    public void testChat4DoiThong() {
        List<Card> heo = List.of(new Card("2", "♥"));
        List<Card> bonDoiThong = List.of(
                new Card("6", "♠"), new Card("6", "♥"),
                new Card("7", "♠"), new Card("7", "♥"),
                new Card("8", "♠"), new Card("8", "♥"),
                new Card("9", "♠"), new Card("9", "♥")
        );

        String msg = SpecialMoveDetector.detectSpecialEvent("Master", bonDoiThong, "Noob", heo);
        assertNotNull(msg);
        assertTrue(msg.contains("4 ĐÔI THÔNG"));
    }

    @Test
    public void testNormalMoveReturnsNull() {
        List<Card> singleBa = List.of(new Card("3", "♠"));
        List<Card> singleBon = List.of(new Card("4", "♠"));

        String msg = SpecialMoveDetector.detectSpecialEvent("Alice", singleBon, "Bob", singleBa);
        assertNull(msg, "Nước đánh rác thông thường không phải sự kiện chặt bài");
    }
}
