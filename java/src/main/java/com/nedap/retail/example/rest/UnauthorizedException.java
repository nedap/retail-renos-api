package com.nedap.retail.example.rest;

/**
 * Unauthorized access exception.
 */
public class UnauthorizedException extends HttpRequestException {

    public UnauthorizedException(final String message) {
        super(message);
    }
}
