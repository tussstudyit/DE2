package com.example.tienlen.client;

/**
 * Lớp khởi chạy Client duy nhất dành cho IntelliJ IDEA.
 * Đã bật sẵn "Allow parallel run" (Allow multiple instances).
 * Mỗi lần bấm Run file này, hệ thống tự động mở 1 Client mới (Người chơi 1, 2, 3, 4...).
 */
public class ClientLauncher {
    public static void main(String[] args) {
        int instance = ClientInstanceDetector.detectInstanceNumber();
        Main.setInitialPlayerName("Người chơi " + instance);
        Main.main(args);
    }
}
