package com.example.tienlen.server.rmi;

import com.example.tienlen.common.network.NetworkUtil;
import com.example.tienlen.common.rmi.ITienLenService;
import com.example.tienlen.server.RoomManager;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * Khởi động và quản lý Java RMI Registry và Service cho Tiến Lên Online.
 * Tự động gắn IP LAN của máy để các máy tính khác trong cùng mạng có thể gọi RMI được.
 */
public class TienLenRmiServer {
    private final int port;
    private final RoomManager roomManager;
    private Registry registry;
    private TienLenServiceImpl service;
    private String primaryLanIp;

    public TienLenRmiServer(int port) {
        this(port, new RoomManager());
    }

    public TienLenRmiServer(int port, RoomManager roomManager) {
        this.port = port;
        this.roomManager = roomManager;
    }

    public void start() {
        try {
            // Thiết lập hostname cho RMI để stub trả về chứa đúng IP LAN của máy chủ
            this.primaryLanIp = NetworkUtil.getLocalLanIp();
            System.setProperty("java.rmi.server.hostname", primaryLanIp);

            try {
                registry = LocateRegistry.createRegistry(port);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry(port);
            }

            service = new TienLenServiceImpl(roomManager);
            registry.rebind(ITienLenService.SERVICE_NAME, service);

            printBanner();
        } catch (Exception e) {
            System.err.println("[RMI SERVER] Lỗi khởi động RMI Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (registry != null) {
                registry.unbind(ITienLenService.SERVICE_NAME);
            }
        } catch (Exception ignored) {
        }
        System.out.println("[RMI SERVER] Đã dừng RMI server.");
    }

    private void printBanner() {
        List<String> allIps = NetworkUtil.getAllLanIps();
        System.out.println("==================================================");
        System.out.println("  TIẾN LÊN ONLINE - JAVA RMI SERVER SẴN SÀNG      ");
        System.out.println("  Cổng RMI Registry: " + port);
        System.out.println("  Tên dịch vụ RMI: " + ITienLenService.SERVICE_NAME);
        System.out.println("  Địa chỉ IP LAN chính: " + primaryLanIp);
        System.out.println("--------------------------------------------------");
        System.out.println("  HƯỚNG DẪN KẾT NỐI CHO CÁC MÁY TRONG MẠNG:");
        System.out.println("  -> Chạy trên cùng máy: Nhập IP 127.0.0.1");
        System.out.println("  -> Máy khác trong cùng Wi-Fi/LAN: Nhập IP " + primaryLanIp);
        if (allIps.size() > 1) {
            System.out.println("  -> Hoặc các IP khác của máy chủ: " + String.join(", ", allIps));
        }
        System.out.println("  Hỗ trợ bàn chơi từ 2 đến 4 người");
        System.out.println("==================================================");
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }
}
