package com.example.tienlen.common.network;

public enum MessageType {
    CONNECT_REQ,        // Client gửi tên người chơi khi kết nối
    CONNECT_ACK,        // Server xác nhận kết nối và trả về playerId
    CREATE_ROOM,        // Client tạo phòng kèm cấu hình maxPlayers (2-4)
    JOIN_ROOM,          // Client vào phòng có sẵn
    ROOM_INFO,          // Server gửi thông tin phòng (danh sách người chơi, trạng thái)
    GAME_STARTED,       // Bắt đầu ván (đủ số người)
    PLAY_CARDS,         // Client gửi bài muốn đánh / Server thông báo bài đã đánh
    PASS_TURN,          // Client bỏ lượt / Server thông báo người bỏ lượt
    ROUND_UPDATE,       // Server cập nhật bàn chơi, lượt tiếp theo
    NEW_ROUND,          // Hết vòng (mọi người đều pass), bắt đầu vòng đánh mới
    TURN_ALERT,         // Server cảnh báo trực tiếp đến client khi đến lượt đánh
    GAME_OVER,          // Kết thúc ván, thông báo thứ hạng / người thắng
    ERROR_MSG,          // Server gửi thông báo lỗi (đánh bài sai luật, chưa tới lượt...)
    START_GAME          // Chủ phòng yêu cầu bắt đầu ván đấu khi đã có từ 2 người trở lên
}

