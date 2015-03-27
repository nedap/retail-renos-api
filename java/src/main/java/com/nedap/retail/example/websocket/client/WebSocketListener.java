package com.nedap.retail.example.websocket.client;

/**
 * Listener used to handle events triggered on WebSocket client.
 */
public interface WebSocketListener {

    /**
     * Triggered when a new message is received on WebSocket from Renos.
     * @param message which was received
     */
    void onMessage(String message);

    /**
     * Triggered when there is an error in WebSocket connection to Renos.
     */
    void onConnectionError();
}
