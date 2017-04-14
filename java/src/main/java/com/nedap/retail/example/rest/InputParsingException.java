package com.nedap.retail.example.rest;

/**
 * Thrown when a user input string could not be parsed
 */
public class InputParsingException extends Exception {

    public InputParsingException(final String message) {
        super(message);
    }
}
