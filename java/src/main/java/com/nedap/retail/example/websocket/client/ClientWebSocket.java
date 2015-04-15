package com.nedap.retail.example.websocket.client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common class for all WebSocket classes.
 */
public abstract class ClientWebSocket {
    
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected Session session;

    private CountDownLatch connectLatch;

    private boolean connected = false;

    public ClientWebSocket() {
        connectLatch = new CountDownLatch(1);
    }

    @OnWebSocketConnect
    public void onConnect(final Session session) {
        LOGGER.info("Connected: {}", session.getRemoteAddress().getHostName());
        this.session = session;
        session.setIdleTimeout(0);
        connected = true;
        connectLatch.countDown();
    }

    @OnWebSocketClose
    public void onClose(final int statusCode, final String reason) {
        LOGGER.info("Connection closed on {}: {} - {}", session.getRemoteAddress(), statusCode, reason);
        connected = false;
        this.session = null;
    }

    @OnWebSocketError
    public void onError(final Throwable e) {
        LOGGER.error("An error occurred while communicating via WebSocket", e);
    }

    public void awaitConnect() throws InterruptedException {
        connectLatch = new CountDownLatch(1);
        connectLatch.await(10, TimeUnit.SECONDS);
    }

    public void sendMessage(final String message) {
        try {
            if (session != null) {
                session.getRemote().sendString(message);
            } else {
                LOGGER.warn("Client is not connected. No session will be sent.");
            }
        } catch (IOException e) {
            LOGGER.error("There was an error", e);
        }
    }

    public void close() {
        if (session != null) {
            session.close(StatusCode.NORMAL, "Application exit.");
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
