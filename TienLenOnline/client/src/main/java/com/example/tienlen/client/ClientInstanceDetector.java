package com.example.tienlen.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tự động phát hiện thứ tự instance của Client (Người chơi 1, 2, 3, 4...)
 * khi người dùng bấm Run nhiều lần trên cùng một file ClientLauncher.
 * Sử dụng loopback port lock (28881-28890) an toàn, tự giải phóng ngay khi đóng cửa sổ.
 */
public class ClientInstanceDetector {
    private static ServerSocket lockSocket;
    private static int instanceNumber = 1;

    public static int detectInstanceNumber() {
        for (int i = 1; i <= 10; i++) {
            try {
                lockSocket = new ServerSocket(28880 + i, 1, InetAddress.getByName("127.0.0.1"));
                instanceNumber = i;
                return i;
            } catch (IOException e) {
                // Port đang được cửa sổ client khác nắm giữ, thử số tiếp theo
            }
        }
        return 1;
    }

    public static int getInstanceNumber() {
        return instanceNumber;
    }

    /**
     * Lưu mã phòng vừa tạo vào file tạm để các Client sau tự động nhận diện và điền sẵn
     */
    public static void saveLastRoomCode(String code) {
        if (code == null || code.isBlank()) return;
        try {
            Path path = Paths.get(System.getProperty("java.io.tmpdir"), "tienlen_last_room.txt");
            Files.writeString(path, code.trim());
        } catch (Exception ignored) {
        }
    }

    /**
     * Lấy mã phòng gần nhất đã tạo để tiện bấm Vào phòng ngay
     */
    public static String getLastRoomCode() {
        try {
            Path path = Paths.get(System.getProperty("java.io.tmpdir"), "tienlen_last_room.txt");
            if (Files.exists(path)) {
                String code = Files.readString(path).trim();
                if (code.matches("\\d{6}")) {
                    return code;
                }
            }
        } catch (Exception ignored) {
        }
        return "100001";
    }

    /**
     * Lưu địa chỉ IP của server vào file tạm để lần mở sau không cần nhập lại
     */
    public static void saveLastServerHost(String host) {
        if (host == null || host.isBlank()) return;
        try {
            Path path = Paths.get(System.getProperty("java.io.tmpdir"), "tienlen_last_host.txt");
            Files.writeString(path, host.trim());
        } catch (Exception ignored) {
        }
    }

    /**
     * Lấy địa chỉ IP server gần nhất đã sử dụng (mặc định 127.0.0.1)
     */
    public static String getLastServerHost() {
        try {
            Path path = Paths.get(System.getProperty("java.io.tmpdir"), "tienlen_last_host.txt");
            if (Files.exists(path)) {
                String host = Files.readString(path).trim();
                if (!host.isBlank()) {
                    return host;
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }
}
