package com.nedap.retail.example.websocket.client;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nedap.retail.example.rest.MessageParsingException;
import com.nedap.retail.renos.api.v2.ws.MessageParser;
import com.nedap.retail.renos.api.v2.ws.message.*;

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
        } catch (final MessageParsingException e) {
            LOG.info("The incoming message could not be parsed.");
        } catch (final Exception e) {
            LOG.error("There was an error while handling the Renos API message", e);
        }
    }

    private static Response parseResponse(final String message) throws MessageParsingException {
        try {
            return Optional.ofNullable(MessageParser.parseResponse(message))
                    .filter(response -> response.getResponse() != null)
                    .orElseThrow(() -> new MessageParsingException("response"));
        } catch (final IOException e) {
            throw new MessageParsingException("response");
        }
    }

    private static Event parseEvent(final String message) throws MessageParsingException {
        try {
            return Optional.ofNullable(MessageParser.parseEvent(message)).filter(event -> event.getType() != null)
                    .orElseThrow(() -> new MessageParsingException("event"));
        } catch (final IOException e) {
            throw new MessageParsingException("event");
        }
    }

    private void handleResponse(final String message) throws MessageParsingException {
        final Response response = parseResponse(message);
        switch (response.getResponse()) {
            case UNKNOWN:
                LOG.error("There was an error with the sent request: {}", response.getContent().getMessage());
                break;
            default:
                LOG.info("Received response: {} {}", response.getResponse(), response.getContent().getMessage());
        }
    }

    private void handleEvent(final String message) throws MessageParsingException {
        final Event event = parseEvent(message);
        final StringBuilder sb = new StringBuilder();
        sb.append("Received ").append(event.getType()).append(",");
        sb.append(" detected");
        if (event.getUnits() != null) {
            sb.append(" by unit(s) ").append(event.getUnits());
        }
        if (event.getAisle() != null) {
            sb.append(" in aisle ").append(event.getAisle());
        }
        if (event.getGroup() != null) {
            sb.append(" in group ").append(event.getGroup());
        }
        sb.append(" at ").append(event.getTime());
        if (event.getDirection() != null) {
            sb.append(" with direction ").append(event.getDirection());
        }

        List<Epc> epcs = null;

        switch (event.getType()) {
            case INPUT_OBSERVATION:
                final InputObservationEvent inputEvent = (InputObservationEvent) event;
                sb.append(" with status: ").append(inputEvent.getStatus());
                break;
            case RFID_OBSERVATION:
                final RfidObservationEvent rfidObservationEvent = (RfidObservationEvent) event;
                sb.append(" with EPCs:");
                epcs = rfidObservationEvent.getEpcs();
                break;
            case RFID_MOVE:
                final RfidMoveEvent rfidMoveEvent = (RfidMoveEvent) event;
                sb.append(" with EPCs:");
                epcs = rfidMoveEvent.getEpcs();
                break;
            case SD_LABEL_DETECT:
                final SdLabelDetectEvent sdLabelDetectEvent = (SdLabelDetectEvent) event;
                sb.append(" SD address ").append(sdLabelDetectEvent.getSdAddress());
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
            final StringBuilder sb = new StringBuilder();
            sb.append(epc.getEpc());
            if (epc.getEasStatus() != null) {
                sb.append(" with EAS status ").append(epc.getEasStatus());
            }
            sb.append(" detected");
            if (epc.getUnits() != null) {
                sb.append(" by unit(s) ").append(epc.getUnits());
            }
            if (epc.getAisle() != null) {
                sb.append(" in aisle ").append(epc.getAisle());
            }
            if (epc.getGroup() != null) {
                sb.append(" in group ").append(epc.getGroup());
            }
            sb.append(" at ").append(epc.getTime());

            LOG.info("   {}", sb.toString());
        }
    }

    @Override
    public void onConnectionError() {
        this.client.reconnect();
    }
}
