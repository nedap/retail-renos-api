package com.nedap.retail.example.websocket.client;

import com.nedap.retail.renos.api.v2.ws.MessageParser;
import com.nedap.retail.renos.api.v2.ws.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

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
                break;
            default:
                LOG.info("Received response: {}", response.response);
        }
    }

    private void handleEvent(final String message) throws IOException {
        final Event event = MessageParser.parseEvent(message);

        final StringBuilder sb = new StringBuilder();
        sb.append("Received ").append(event.type).append(",");
        sb.append(" detected");
        if (event.units != null) {
            sb.append(" by unit(s) ").append(event.units);
        }
        sb.append(" in group ").append(event.group);
        sb.append(" at ").append(event.time);
        if (event.direction != null) {
            sb.append(" with direction ").append(event.direction);
        }

        List<Epc> epcs = null;

        switch (event.type) {
            case INPUT_OBSERVATION:
                final InputObservationEvent inputEvent = (InputObservationEvent) event;
                sb.append(" with status: ").append(inputEvent.status);
                break;
            case RFID_OBSERVATION:
                final RfidObservationEvent rfidObservationEvent = (RfidObservationEvent) event;
                sb.append(" with EPCs:");
                epcs = rfidObservationEvent.epcs;
                break;
            case RFID_MOVE:
                final RfidMoveEvent rfidMoveEvent = (RfidMoveEvent) event;
                sb.append(" with EPCs:");
                epcs = rfidMoveEvent.epcs;
                break;
            case SD_LABEL_DETECT:
                final SdLabelDetectEvent sdLabelDetectEvent = (SdLabelDetectEvent) event;
                sb.append(" SD address ").append(sdLabelDetectEvent.sdAddress);
                break;
            default:
                // no additional information
        }

        LOG.info(sb.toString());

        if (epcs != null) {
            printEpcs(epcs);
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
