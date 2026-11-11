package com.example.tienlen.server;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.network.JsonUtil;
import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.network.MessageType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TienLenIntegrationTest {
    private static final int TEST_PORT = 9999;
    private static TienLenTcpServer server;
    private static Thread serverThread;

    @BeforeAll
    public static void setup() throws Exception {
        server = new TienLenTcpServer(TEST_PORT);
        serverThread = new Thread(server::start, "TestServer");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(300); // Đợi server khởi động
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop();
        }
    }

    static class MockClient {
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        final List<Message> inbox = new CopyOnWriteArrayList<>();
        volatile boolean running = true;
        String playerId;
        List<Card> hand = new ArrayList<>();

        public MockClient(String name) throws Exception {
            socket = new Socket("127.0.0.1", TEST_PORT);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while (running && (line = in.readLine()) != null) {
                        Message m = JsonUtil.fromJson(line, Message.class);
                        inbox.add(m);
                    }
                } catch (Exception ignored) {
                }
            });
            reader.setDaemon(true);
            reader.start();

            // Handshake
            Message req = new Message(MessageType.CONNECT_REQ);
            req.setPlayerName(name);
            send(req);

            Message ack = waitFor(MessageType.CONNECT_ACK, 2);
            assertNotNull(ack);
            assertEquals(MessageType.CONNECT_ACK, ack.getType());
            this.playerId = ack.getPlayerId();
        }

        void send(Message m) {
            out.println(JsonUtil.toJson(m));
        }

        Message waitFor(MessageType type, int timeoutSeconds) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
            while (System.currentTimeMillis() < deadline) {
                for (int i = 0; i < inbox.size(); i++) {
                    Message m = inbox.get(i);
                    if (m != null && m.getType() == type) {
                        inbox.remove(i);
                        return m;
                    }
                }
                Thread.sleep(50);
            }
            return null;
        }

        void close() {
            running = false;
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testTwoPlayerGameFlow() throws Exception {
        MockClient c1 = new MockClient("Alice");
        MockClient c2 = new MockClient("Bob");

        try {
            // Alice tạo phòng 2 người
            Message createMsg = new Message(MessageType.CREATE_ROOM);
            createMsg.setRoomId("TEST_ROOM_2P");
            createMsg.setMaxPlayers(2);
            c1.send(createMsg);

            Message rInfo = c1.waitFor(MessageType.ROOM_INFO, 2);
            assertNotNull(rInfo, "Alice phải nhận ROOM_INFO sau khi tạo phòng");

            // Bob vào phòng 2 người
            Message joinMsg = new Message(MessageType.JOIN_ROOM);
            joinMsg.setRoomId("TEST_ROOM_2P");
            joinMsg.setMaxPlayers(2);
            c2.send(joinMsg);

            // Vì đủ 2 người, server tự động bắt đầu ván và chia bài
            Message start1 = c1.waitFor(MessageType.GAME_STARTED, 3);
            Message start2 = c2.waitFor(MessageType.GAME_STARTED, 3);

            assertNotNull(start1, "Alice phải nhận GAME_STARTED");
            assertNotNull(start2, "Bob phải nhận GAME_STARTED");

            // Yêu cầu: Chia bài tự động 13 lá
            assertEquals(13, start1.getCards().size(), "Alice phải nhận đúng 13 lá bài");
            assertEquals(13, start2.getCards().size(), "Bob phải nhận đúng 13 lá bài");
            c1.hand = start1.getCards();
            c2.hand = start2.getCards();

            // Yêu cầu: Người đánh đầu tiên ngẫu nhiên
            String turnPlayerId = start1.getCurrentTurnPlayerId();
            assertNotNull(turnPlayerId);

            MockClient firstPlayer = turnPlayerId.equals(c1.playerId) ? c1 : c2;
            MockClient secondPlayer = firstPlayer == c1 ? c2 : c1;

            // Yêu cầu: Màn hình cảnh báo khi đến lượt
            Message alert = firstPlayer.waitFor(MessageType.TURN_ALERT, 2);
            assertNotNull(alert, "Người đi đầu phải nhận TURN_ALERT cảnh báo đến lượt");

            // Người đầu đánh 1 lá bài rác (lá nhỏ nhất trên tay)
            Card cardToPlay = firstPlayer.hand.get(0);
            Message playMsg = new Message(MessageType.PLAY_CARDS);
            playMsg.setCards(List.of(cardToPlay));
            firstPlayer.send(playMsg);

            // Cả hai người phải nhận ROUND_UPDATE với lá bài vừa đánh
            Message update1 = firstPlayer.waitFor(MessageType.ROUND_UPDATE, 2);
            Message update2 = secondPlayer.waitFor(MessageType.ROUND_UPDATE, 2);
            assertNotNull(update1, "Người 1 phải nhận ROUND_UPDATE");
            assertNotNull(update2, "Người 2 phải nhận ROUND_UPDATE");
            assertEquals(1, update1.getCards().size());
            assertEquals(cardToPlay.displayText(), update1.getCards().get(0).displayText());

            // Lượt tiếp theo chuyển sang người thứ hai
            assertEquals(secondPlayer.playerId, update1.getCurrentTurnPlayerId());

            // Người thứ hai nhận TURN_ALERT
            Message alert2 = secondPlayer.waitFor(MessageType.TURN_ALERT, 2);
            assertNotNull(alert2, "Người thứ hai phải nhận TURN_ALERT khi đến lượt");

        } finally {
            c1.close();
            c2.close();
        }
    }

    @Test
    public void testFourPlayerGameFlow() throws Exception {
        MockClient c1 = new MockClient("Player1");
        MockClient c2 = new MockClient("Player2");
        MockClient c3 = new MockClient("Player3");
        MockClient c4 = new MockClient("Player4");

        try {
            // Player 1 tạo phòng 4 người (mã 6 số 100004)
            Message createMsg = new Message(MessageType.CREATE_ROOM);
            createMsg.setRoomId("100004");
            createMsg.setMaxPlayers(4);
            c1.send(createMsg);
            assertNotNull(c1.waitFor(MessageType.ROOM_INFO, 2));

            // Player 2, 3, 4 lần lượt vào phòng
            Message join = new Message(MessageType.JOIN_ROOM);
            join.setRoomId("100004");
            join.setMaxPlayers(4);

            c2.send(join);
            c3.send(join);
            c4.send(join);

            // Đủ 4 người -> Tự động bắt đầu ván
            Message s1 = c1.waitFor(MessageType.GAME_STARTED, 3);
            Message s2 = c2.waitFor(MessageType.GAME_STARTED, 3);
            Message s3 = c3.waitFor(MessageType.GAME_STARTED, 3);
            Message s4 = c4.waitFor(MessageType.GAME_STARTED, 3);

            assertNotNull(s1, "P1 nhận GAME_STARTED");
            assertNotNull(s2, "P2 nhận GAME_STARTED");
            assertNotNull(s3, "P3 nhận GAME_STARTED");
            assertNotNull(s4, "P4 nhận GAME_STARTED");

            assertEquals(13, s1.getCards().size());
            assertEquals(13, s2.getCards().size());
            assertEquals(13, s3.getCards().size());
            assertEquals(13, s4.getCards().size());
            assertEquals(4, s1.getPlayers().size());
        } finally {
            c1.close();
            c2.close();
            c3.close();
            c4.close();
        }
    }

    @Test
    public void testStartGameEarlyWithTwoPlayersInFourPlayerRoom() throws Exception {
        MockClient c1 = new MockClient("Host");
        MockClient c2 = new MockClient("Guest");

        try {
            // Tạo phòng tối đa 4 người
            Message createMsg = new Message(MessageType.CREATE_ROOM);
            createMsg.setRoomId("100005");
            createMsg.setMaxPlayers(4);
            c1.send(createMsg);
            assertNotNull(c1.waitFor(MessageType.ROOM_INFO, 2));

            // Người thứ 2 vào phòng
            Message join = new Message(MessageType.JOIN_ROOM);
            join.setRoomId("100005");
            join.setMaxPlayers(4);
            c2.send(join);
            assertNotNull(c2.waitFor(MessageType.ROOM_INFO, 2));

            // Bàn mới có 2 người, chủ phòng bấm BẮT ĐẦU VÁN NGAY
            Message startReq = new Message(MessageType.START_GAME);
            c1.send(startReq);

            // Cả hai người đều nhận GAME_STARTED vì từ 2 người trở lên là chơi được
            Message s1 = c1.waitFor(MessageType.GAME_STARTED, 3);
            Message s2 = c2.waitFor(MessageType.GAME_STARTED, 3);

            assertNotNull(s1, "Chủ phòng phải nhận GAME_STARTED");
            assertNotNull(s2, "Khách phải nhận GAME_STARTED");
            assertEquals(13, s1.getCards().size());
            assertEquals(13, s2.getCards().size());
        } finally {
            c1.close();
            c2.close();
        }
    }

    @Test
    public void testRoomCodeDuplicateAndNotFoundErrors() throws Exception {
        MockClient c1 = new MockClient("UserA");
        MockClient c2 = new MockClient("UserB");

        try {
            // Tạo phòng 100006
            Message create = new Message(MessageType.CREATE_ROOM);
            create.setRoomId("100006");
            create.setMaxPlayers(4);
            c1.send(create);
            assertNotNull(c1.waitFor(MessageType.ROOM_INFO, 2));

            // Cố tình tạo phòng trùng mã 100006
            Message dupCreate = new Message(MessageType.CREATE_ROOM);
            dupCreate.setRoomId("100006");
            dupCreate.setMaxPlayers(4);
            c2.send(dupCreate);

            Message errMsg = c2.waitFor(MessageType.ERROR_MSG, 2);
            assertNotNull(errMsg, "Phải nhận ERROR_MSG khi tạo trùng mã phòng");
            assertTrue(errMsg.getPayload().contains("đã tồn tại"));

            // Cố tình vào phòng không tồn tại 999999
            Message notFoundJoin = new Message(MessageType.JOIN_ROOM);
            notFoundJoin.setRoomId("999999");
            c2.send(notFoundJoin);

            Message errNotFound = c2.waitFor(MessageType.ERROR_MSG, 2);
            assertNotNull(errNotFound, "Phải nhận ERROR_MSG khi vào phòng không tồn tại");
            assertTrue(errNotFound.getPayload().contains("Không tìm thấy"));
        } finally {
            c1.close();
            c2.close();
        }
    }

    @Test
    public void testInvalidMoveRejectedByServer() throws Exception {
        MockClient c1 = new MockClient("Tester1");
        MockClient c2 = new MockClient("Tester2");

        try {
            Message createMsg = new Message(MessageType.CREATE_ROOM);
            createMsg.setRoomId("100007");
            createMsg.setMaxPlayers(2);
            c1.send(createMsg);
            assertNotNull(c1.waitFor(MessageType.ROOM_INFO, 2));

            Message joinMsg = new Message(MessageType.JOIN_ROOM);
            joinMsg.setRoomId("100007");
            joinMsg.setMaxPlayers(2);
            c2.send(joinMsg);

            Message s1 = c1.waitFor(MessageType.GAME_STARTED, 3);
            Message s2 = c2.waitFor(MessageType.GAME_STARTED, 3);
            assertNotNull(s1);
            assertNotNull(s2);

            MockClient firstPlayer = s1.getCurrentTurnPlayerId().equals(c1.playerId) ? c1 : c2;
            List<Card> firstHand = s1.getCurrentTurnPlayerId().equals(c1.playerId) ? s1.getCards() : s2.getCards();

            // Cố tình tạo nước đánh 2 lá bài không cùng rank (không phải đôi hay tổ hợp hợp lệ)
            Card cA = firstHand.get(0);
            Card cB = null;
            for (Card c : firstHand) {
                if (c.getRankValue() != cA.getRankValue()) {
                    cB = c;
                    break;
                }
            }

            if (cB != null) {
                Message invalidPlay = new Message(MessageType.PLAY_CARDS);
                invalidPlay.setCards(List.of(cA, cB));
                firstPlayer.send(invalidPlay);

                Message err = firstPlayer.waitFor(MessageType.ERROR_MSG, 2);
                assertNotNull(err, "Server phải trả về ERROR_MSG khi đánh tổ hợp không hợp lệ");
                assertTrue(err.getPayload().contains("không hợp lệ") || err.getPayload().contains("không thể đè"));
            }
        } finally {
            c1.close();
            c2.close();
        }
    }
}
