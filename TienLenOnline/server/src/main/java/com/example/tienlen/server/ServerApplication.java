package com.example.tienlen.server;

import com.example.tienlen.common.rmi.ITienLenService;
import com.example.tienlen.server.rmi.TienLenRmiServer;

public class ServerApplication {
    public static final int DEFAULT_RMI_PORT = ITienLenService.DEFAULT_RMI_PORT; // 1099
    public static final int DEFAULT_TCP_PORT = 8888;

    public static void main(String[] args) {
        int rmiPort = DEFAULT_RMI_PORT;
        if (args.length > 0) {
            try {
                rmiPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        RoomManager sharedRoomManager = new RoomManager();

        // 1. Khởi động RMI Server chính cho hệ thống (chuẩn Đề số 2 - RMI LAN)
        TienLenRmiServer rmiServer = new TienLenRmiServer(rmiPort, sharedRoomManager);
        rmiServer.start();

        // 2. Khởi động TCP Socket Server phụ song song để hỗ trợ kiểm thử tương thích
        TienLenTcpServer tcpServer = new TienLenTcpServer(DEFAULT_TCP_PORT, sharedRoomManager);
        Thread tcpThread = new Thread(tcpServer::start, "TienLenTcpServerThread");
        tcpThread.setDaemon(true);
        tcpThread.start();
    }
}
