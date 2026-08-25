package com.example.tienlen.client.network;

import com.example.tienlen.common.network.Message;

public class GameClient {
    private boolean connected;

    public void connect(String serverUrl) {
        connected = true;
    }

    public void send(Message message) {
        if (!connected) {
            return;
        }
    }

    public void disconnect() {
        connected = false;
    }

    public boolean isConnected() {
        return connected;
    }
}
