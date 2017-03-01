package com.nedap.retail.example.rest;

/**
 * Error 404, page not found exception
 */
public class NotFoundException extends Exception {

    public NotFoundException(final String message) {
        super(message);
    }
}
