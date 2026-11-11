package com.example.tienlen.common.rmi;

import com.example.tienlen.common.model.Card;
import com.example.tienlen.common.network.Message;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface Remote Service chính của hệ thống Tiến Lên Online qua Java RMI.
 * Cung cấp các phương thức gọi hàm từ xa cho các máy Client trong cùng mạng LAN kết nối vào.
 */
public interface ITienLenService extends Remote {
    String SERVICE_NAME = "TienLenRmiService";
    int DEFAULT_RMI_PORT = 1099;

    /**
     * Đăng ký kết nối người chơi với Server qua RMI
     * @param playerName Tên người chơi
     * @param callback Đối tượng callback RMI để server gửi thông báo về client
     * @return playerId duy nhất được server cấp phát
     */
    String registerPlayer(String playerName, ITienLenClientCallback callback) throws RemoteException;

    /**
     * Tạo phòng chơi mới
     * @param roomId Mã phòng (6 chữ số) hoặc null để server tự sinh mã
     * @param playerId ID người chơi tạo phòng
     * @param maxPlayers Số người chơi tối đa (từ 2 đến 4 người theo đề bài)
     */
    void createRoom(String roomId, String playerId, int maxPlayers) throws RemoteException;

    /**
     * Tham gia phòng chơi cụ thể hoặc ghép phòng nhanh
     * @param roomId Mã phòng muốn vào (hoặc null/trống để tìm bàn nhanh phù hợp)
     * @param playerId ID người chơi
     * @param maxPlayers Số người mong muốn nếu phải tạo phòng mới khi ghép nhanh
     */
    void joinRoom(String roomId, String playerId, int maxPlayers) throws RemoteException;

    /**
     * Bắt đầu ván đấu sớm khi đủ từ 2 người trở lên (dành cho chủ phòng)
     */
    void startGame(String roomId, String playerId) throws RemoteException;

    /**
     * Đánh các lá bài được chọn ra giữa bàn
     */
    void playCards(String roomId, String playerId, List<Card> cards) throws RemoteException;

    /**
     * Bỏ lượt đánh trong vòng hiện tại
     */
    void passTurn(String roomId, String playerId) throws RemoteException;

    /**
     * Rời khỏi phòng chơi
     */
    void leaveRoom(String roomId, String playerId) throws RemoteException;

    /**
     * Ngắt kết nối người chơi khỏi server
     */
    void disconnect(String playerId) throws RemoteException;

    /**
     * Lấy các thông điệp sự kiện đang chờ của người chơi
     * (Hỗ trợ đảm bảo 100% nhận tin nhắn kể cả khi tường lửa Windows Firewall trên máy Client chặn RMI callback)
     */
    List<Message> pollEvents(String playerId) throws RemoteException;
}
