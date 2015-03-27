package com.nedap.retail.example.websocket.client;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Used to connect to Renos V2 API WebSocket server.
 */
@WebSocket
public class RenosApiSocket extends ClientWebSocket {

    private List<WebSocketListener> listeners = new ArrayList<>();

    public void subscribe(final WebSocketListener listener) {
        listeners.add(listener);
    }

    @OnWebSocketMessage
    public void onMessage(final String message) {
        LOGGER.debug("Received from server: {}", message);
        for (final WebSocketListener listener : listeners) {
            listener.onMessage(message);
        }
    }

    @Override
    public void onError(final Throwable e) {
        if ((e instanceof ConnectException) || (e instanceof SocketTimeoutException)) {
            LOGGER.warn("Could not connect to Renos WebSocket.");
            for (final WebSocketListener listener : listeners) {
                listener.onConnectionError();
            }
            return;
        }
        super.onError(e);
    }
}
