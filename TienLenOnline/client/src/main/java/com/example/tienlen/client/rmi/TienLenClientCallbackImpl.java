package com.example.tienlen.client.rmi;

import com.example.tienlen.common.network.Message;
import com.example.tienlen.common.rmi.ITienLenClientCallback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

/**
 * Hiện thực đối tượng RMI Callback phía Client.
 * Nhận thông điệp được Server gọi từ xa qua RMI và chuyển tiếp tới Consumer xử lý.
 */
public class TienLenClientCallbackImpl extends UnicastRemoteObject implements ITienLenClientCallback {
    private final Consumer<Message> messageConsumer;

    public TienLenClientCallbackImpl(Consumer<Message> messageConsumer) throws RemoteException {
        super(0); // Cổng ngẫu nhiên
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void onMessage(Message message) throws RemoteException {
        if (messageConsumer != null) {
            messageConsumer.accept(message);
        }
    }
}
