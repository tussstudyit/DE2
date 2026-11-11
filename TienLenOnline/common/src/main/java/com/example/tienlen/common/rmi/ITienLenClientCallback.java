package com.example.tienlen.common.rmi;

import com.example.tienlen.common.network.Message;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface Remote Callback cho Client trong hệ thống Tiến Lên RMI.
 * Server gọi ngược lại Client qua giao thức RMI để gửi cập nhật bàn chơi, ván đấu.
 */
public interface ITienLenClientCallback extends Remote {
    void onMessage(Message message) throws RemoteException;
}
