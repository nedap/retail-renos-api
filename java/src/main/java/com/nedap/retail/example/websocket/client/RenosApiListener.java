package com.nedap.retail.example.websocket.client;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nedap.retail.renos.api.v2.ws.MessageParser;
import com.nedap.retail.renos.api.v2.ws.message.Epc;
import com.nedap.retail.renos.api.v2.ws.message.Event;
import com.nedap.retail.renos.api.v2.ws.message.Response;
import com.nedap.retail.renos.api.v2.ws.message.RfidAlarmEvent;
import com.nedap.retail.renos.api.v2.ws.message.RfidMoveEvent;
import com.nedap.retail.renos.api.v2.ws.message.RfidObservationEvent;

/**
 * Listener for messages received from Renos V2 API.
 */
public class RenosApiListener implements WebSocketListener {

    private static final Logger LOG = LoggerFactory.getLogger(RenosApiListener.class);

    private final RenosWebSocketClient client;

    public RenosApiListener(final RenosWebSocketClient client) {
        this.client = client;
    }

    @Override
    public void onMessage(final String message) {
        try {
            if (message.contains("response")) {
                handleResponse(message);
            } else if (message.contains("event")) {
                handleEvent(message);
            }
        } catch (IOException e) {
            LOG.error("There was an error while handling the Renos API message", e);
        }
    }

    private void handleResponse(final String message) throws IOException {
        final Response response = MessageParser.parseResponse(message);
        switch (response.response) {
            case UNKNOWN:
                LOG.error("There was an error with the sent request: {}", response.content.message);
        }
    }

    private void handleEvent(final String message) throws IOException {
        final Event event = MessageParser.parseEvent(message);
        switch (event.type) {
            case RF_ALARM:
            case IR_DIRECTION:
            case METAL_ALARM:
                LOG.info("Received {}, id {}, detected by {} at {} {} ", event.type, event.id, event.group,
                        event.time, event.direction == null ? "" : "with direction " + event.direction);
                break;
            case RFID_ALARM: {
                final String epc = ((RfidAlarmEvent) event).epc;
                LOG.info("Received {} with epc {}, id {}, detected by {} at {} {}", event.type, epc, event.id,
                        event.group, event.time, event.direction == null ? "" : "with direction " + event.direction);
                break;
            }
            case RFID_OBSERVATION: {
                final RfidObservationEvent rfidObservationEvent = (RfidObservationEvent) event;
                LOG.info("Received {} with id {} at {} with EPCs:", event.type, event.id, event.time);
                printEpcs(rfidObservationEvent.epcs);
                break;
            }
            case RFID_MOVE:
                final RfidMoveEvent rfidMoveEvent = (RfidMoveEvent) event;
                LOG.info("Received {} with id {} at {}, direction {} with EPCs:", event.type, event.id, event.time, event.direction);
                printEpcs(rfidMoveEvent.epcs);
                break;
            default:
                LOG.info("Unknown event type received {}", event.type);
                break;
        }
    }

    private void printEpcs(final List<Epc> epcs) {
        for (final Epc epc : epcs) {
            LOG.info("   {} at {} {} {}", epc.epc, epc.time,
                    epc.easStatus != null ? "with status " + epc.easStatus : "",
                    epc.group != null ? "by group " + epc.group : "");
        }
    }

    @Override
    public void onConnectionError() {
        client.reconnect();
    }
}
