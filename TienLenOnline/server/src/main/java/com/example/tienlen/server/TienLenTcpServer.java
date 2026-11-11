package com.example.tienlen.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TienLenTcpServer {
    private final int port;
    private final RoomManager roomManager;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public TienLenTcpServer(int port) {
        this(port, new RoomManager());
    }

    public TienLenTcpServer(int port, RoomManager roomManager) {
        this.port = port;
        this.roomManager = roomManager;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("==================================================");
            System.out.println("  TIẾN LÊN ONLINE - TCP SOCKET SERVER SẴN SÀNG   ");
            System.out.println("  Cổng lắng nghe: " + port);
            System.out.println("  Hỗ trợ bàn chơi từ 2 đến 4 người");
            System.out.println("==================================================");

            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, roomManager);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[SERVER] Lỗi ServerSocket: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        threadPool.shutdownNow();
        System.out.println("[SERVER] Đã dừng server.");
    }
}
