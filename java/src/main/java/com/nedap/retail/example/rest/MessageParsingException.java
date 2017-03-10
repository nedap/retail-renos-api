package com.nedap.retail.example.rest;

import java.io.IOException;

/**
 * Missing Json elements
 */
public class MessageParsingException extends IOException {

    public MessageParsingException(final String messageType) {
        super("Parsing \"" + messageType + "\" message failed. "
                + "Please make sure you are connected to a compatible device.");
    }
}
