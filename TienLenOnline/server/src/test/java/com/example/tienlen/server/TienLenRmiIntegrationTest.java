package com.example.tienlen.server;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.network.MessageType;
import com.example.tienlen.common.rmi.ITienLenClientCallback;
import com.example.tienlen.common.rmi.ITienLenService;
import com.example.tienlen.server.rmi.TienLenRmiServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TienLenRmiIntegrationTest {
    private static final int RMI_TEST_PORT = 10998;
    private static TienLenRmiServer rmiServer;
    private static ITienLenService service;

    @BeforeAll
    public static void setup() throws Exception {
        rmiServer = new TienLenRmiServer(RMI_TEST_PORT);
        rmiServer.start();
        Thread.sleep(300);

        Registry registry = LocateRegistry.getRegistry("127.0.0.1", RMI_TEST_PORT);
        service = (ITienLenService) registry.lookup(ITienLenService.SERVICE_NAME);
        assertNotNull(service, "Phải lookup được TienLenRmiService từ Registry");
    }

    @AfterAll
    public static void teardown() {
        if (rmiServer != null) {
            rmiServer.stop();
        }
    }

    static class MockRmiClient extends UnicastRemoteObject implements ITienLenClientCallback {
        final List<Message> inbox = new CopyOnWriteArrayList<>();
        final String name;
        String playerId;
        List<Card> hand = new ArrayList<>();

        public MockRmiClient(String name) throws RemoteException {
            super(0);
            this.name = name;
            this.playerId = service.registerPlayer(name, this);
        }

        @Override
        public void onMessage(Message message) throws RemoteException {
            inbox.add(message);
        }

        Message waitFor(MessageType type, int timeoutSeconds) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
            while (System.currentTimeMillis() < deadline) {
                // Kiểm tra tin nhắn nhận qua callback
                for (int i = 0; i < inbox.size(); i++) {
                    Message m = inbox.get(i);
                    if (m != null && m.getType() == type) {
                        inbox.remove(i);
                        return m;
                    }
                }
                // Đồng thời thử poll nếu cần
                try {
                    List<Message> polled = service.pollEvents(playerId);
                    for (Message pm : polled) {
                        if (pm != null && pm.getType() == type) {
                            return pm;
                        } else if (pm != null) {
                            inbox.add(pm);
                        }
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(50);
            }
            return null;
        }

        void close() {
            try {
                if (playerId != null) {
                    service.disconnect(playerId);
                }
                UnicastRemoteObject.unexportObject(this, true);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testTwoPlayerRmiGameFlow() throws Exception {
        MockRmiClient c1 = new MockRmiClient("AliceRMI");
        MockRmiClient c2 = new MockRmiClient("BobRMI");

        try {
            // Alice tạo phòng 2 người
            String roomId = "RMI2P1";
            service.createRoom(roomId, c1.playerId, 2);

            Message rInfo = c1.waitFor(MessageType.ROOM_INFO, 2);
            assertNotNull(rInfo, "Alice phải nhận ROOM_INFO sau khi tạo phòng RMI");

            // Bob vào phòng 2 người
            service.joinRoom(roomId, c2.playerId, 2);

            // Vì đủ 2 người, server tự động bắt đầu ván
            Message s1 = c1.waitFor(MessageType.GAME_STARTED, 3);
            Message s2 = c2.waitFor(MessageType.GAME_STARTED, 3);

            assertNotNull(s1, "Alice phải nhận GAME_STARTED qua RMI");
            assertNotNull(s2, "Bob phải nhận GAME_STARTED qua RMI");

            // Kiểm tra tự động chia 13 lá
            assertEquals(13, s1.getCards().size());
            assertEquals(13, s2.getCards().size());
            c1.hand = s1.getCards();
            c2.hand = s2.getCards();

            // Xác định người đi đầu
            String turnPlayerId = s1.getCurrentTurnPlayerId();
            assertNotNull(turnPlayerId);
            MockRmiClient first = turnPlayerId.equals(c1.playerId) ? c1 : c2;
            MockRmiClient second = first == c1 ? c2 : c1;

            // Nhận cảnh báo lượt đánh
            Message alert = first.waitFor(MessageType.TURN_ALERT, 2);
            assertNotNull(alert, "Người đi đầu phải nhận TURN_ALERT qua RMI");

            // Đánh 1 lá bài
            Card toPlay = first.hand.get(0);
            service.playCards(roomId, first.playerId, List.of(toPlay));

            // Cả 2 nhận ROUND_UPDATE qua RMI
            Message up1 = first.waitFor(MessageType.ROUND_UPDATE, 2);
            Message up2 = second.waitFor(MessageType.ROUND_UPDATE, 2);

            assertNotNull(up1, "Người 1 nhận ROUND_UPDATE");
            assertNotNull(up2, "Người 2 nhận ROUND_UPDATE");
            assertEquals(1, up1.getCards().size());
            assertEquals(toPlay.displayText(), up1.getCards().get(0).displayText());
            assertEquals(second.playerId, up1.getCurrentTurnPlayerId());

        } finally {
            c1.close();
            c2.close();
        }
    }

    @Test
    public void testFourPlayerRmiGameFlow() throws Exception {
        MockRmiClient c1 = new MockRmiClient("P1_RMI");
        MockRmiClient c2 = new MockRmiClient("P2_RMI");
        MockRmiClient c3 = new MockRmiClient("P3_RMI");
        MockRmiClient c4 = new MockRmiClient("P4_RMI");

        try {
            String roomId = "RMI4P1";
            service.createRoom(roomId, c1.playerId, 4);
            assertNotNull(c1.waitFor(MessageType.ROOM_INFO, 2));

            service.joinRoom(roomId, c2.playerId, 4);
            service.joinRoom(roomId, c3.playerId, 4);
            service.joinRoom(roomId, c4.playerId, 4);

            // Đủ 4 người -> Game started
            Message s1 = c1.waitFor(MessageType.GAME_STARTED, 3);
            Message s2 = c2.waitFor(MessageType.GAME_STARTED, 3);
            Message s3 = c3.waitFor(MessageType.GAME_STARTED, 3);
            Message s4 = c4.waitFor(MessageType.GAME_STARTED, 3);

            assertNotNull(s1);
            assertNotNull(s2);
            assertNotNull(s3);
            assertNotNull(s4);

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
    public void testRmiErrorHandling() throws Exception {
        MockRmiClient c1 = new MockRmiClient("ErrUserA");
        MockRmiClient c2 = new MockRmiClient("ErrUserB");

        try {
            String roomId = "RMI_ERR";
            service.createRoom(roomId, c1.playerId, 4);
            assertNotNull(c1.waitFor(MessageType.ROOM_INFO, 2));

            // Trùng phòng
            service.createRoom(roomId, c2.playerId, 4);
            Message err = c2.waitFor(MessageType.ERROR_MSG, 2);
            assertNotNull(err);
            assertTrue(err.getPayload().contains("đã tồn tại"));

            // Vào phòng không tồn tại
            service.joinRoom("NON_EXIST", c2.playerId, 4);
            Message notFound = c2.waitFor(MessageType.ERROR_MSG, 2);
            assertNotNull(notFound);
            assertTrue(notFound.getPayload().contains("Không tìm thấy"));
        } finally {
            c1.close();
            c2.close();
        }
    }
}
