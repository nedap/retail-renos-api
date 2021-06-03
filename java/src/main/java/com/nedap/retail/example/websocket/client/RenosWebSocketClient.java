package com.nedap.retail.example.websocket.client;

import com.nedap.retail.renos.api.v2.ws.MessageParser;
import com.nedap.retail.renos.api.v2.ws.message.Heartbeat;
import com.nedap.retail.renos.api.v2.ws.message.Authenticate;
import com.nedap.retail.renos.api.v2.ws.message.Subscribe;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RenosWebSocketClient extends WebSocketClient {

    private static final Logger LOG = LoggerFactory.getLogger(RenosWebSocketClient.class);
    private static final int RECONNECT_DELAY = 5;

    private static final String WS_PROTOCOL_PREFIX = "ws://";
    private static final String EVENTS_SOCKET_SUFFIX = "/api/v2/events";

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final RenosApiSocket eventsSocket;
    private Future<?> reconnectFuture;

    private final String eventsSocketUrl;

    private String token;

    public RenosWebSocketClient(final String socketUrl) {
        if (socketUrl.endsWith("/")) {
            throw new IllegalArgumentException("The given URL should not have a trailing \"/\".");
        }

        eventsSocket = new RenosApiSocket();

        // subscribe to WebSocket events from Renos WebSocket API
        final RenosApiListener listener = new RenosApiListener(this);
        eventsSocket.subscribe(listener);

        this.eventsSocketUrl = WS_PROTOCOL_PREFIX + socketUrl.substring(7) + EVENTS_SOCKET_SUFFIX;
        LOG.info("Events socket URL {}", this.eventsSocketUrl);
    }

    public void setToken(final String token) {
        this.token = token;
    }

    /**
     * Start the client, try to connect to Renos WebSocket server and wait until connection is established.
     * 
     * @throws Exception
     */
    public void run() throws Exception {
        super.start();

        // connect Renos v2 API events socket
        connectToSocket(eventsSocketUrl, eventsSocket);
        LOG.info("Client started.");
    }

    private void connectToSocket(final String url, final ClientWebSocket socket)
            throws URISyntaxException, IOException, InterruptedException {
        final URI renosUri = new URI(url);
        LOG.info("Connecting to {}", renosUri);
        final ClientUpgradeRequest request = new ClientUpgradeRequest();

        this.connect(socket, renosUri, request);
        socket.awaitConnect();
        if (socket.isConnected()) {
            cancelReconnect();
            authenticateIfNeeded();
        }
    }

    private void authenticateIfNeeded(){
        if (token != null && !token.isEmpty()) {
            LOG.info("Sending autentication token: {}", token);
            eventsSocket.sendMessage(MessageParser.toJson(new Authenticate(token)));
        }
    }

    public void disconnect() {
        LOG.info("Closing the connection");
        eventsSocket.close();
    }

    public void finish() {
        try {
            disconnect();
            scheduler.shutdownNow();
            super.stop();
        } catch (final Exception e) {
            LOG.error("There was an error while trying to stop the web socket client", e);
        }
    }

    public void reconnect() {
        cancelReconnect();
        LOG.info("Trying to reconnect to Renos WebSocket, wait {}s", RECONNECT_DELAY);
        reconnectFuture = scheduler.schedule(() -> {
            try {
                connectToSocket(eventsSocketUrl, eventsSocket);
            } catch (final URISyntaxException | IOException | InterruptedException e) {
                LOG.error("Could not reconnect to {}, reason {}", eventsSocketUrl, e.getMessage());
            }
        }, RECONNECT_DELAY, TimeUnit.SECONDS);
    }

    private void cancelReconnect() {
        if (reconnectFuture != null) {
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
    }

    public void heartbeat() {
        eventsSocket.sendMessage(MessageParser.toJson(new Heartbeat()));
    }

    public void sendSubscription(final Subscribe subscribe) {
        eventsSocket.sendMessage(MessageParser.toJson(subscribe));
    }
}
