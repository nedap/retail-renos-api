package com.nedap.retail.example.rest;

import java.io.IOException;

/**
 * Exception possibly thrown when performing an HTTP request.
 */
public class HttpRequestException extends IOException {

    public HttpRequestException(final Throwable throwable) {
        super(throwable);
    }

    public HttpRequestException(final String message) {
        super(message);
    }
}
