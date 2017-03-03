package com.nedap.retail.example.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nedap.retail.renos.api.v2.rest.RestMessageParser;
import com.nedap.retail.renos.api.v2.rest.message.*;

/**
 * Wraps REST calls to Renos API v2.
 */
public class ApiCaller {

    private static final Logger LOG = LoggerFactory.getLogger(ApiCaller.class);

    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private static final String GET = "GET";

    private final String baseUrl;

    private String username = "";
    private String password = "";

    public ApiCaller(final String baseUrl) {
        // make sure baseUrl does not end with a slash
        if (baseUrl.endsWith("/")) {
            throw new IllegalArgumentException("The given URL should not have a trailing \"/\".");
        }

        this.baseUrl = baseUrl;
    }

    private <T extends RestObject> T parseAndRethrow(final String json, final Class<T> clazz)
            throws MessageParsingException {
        try {
            return Optional.ofNullable(RestMessageParser.parse(json, clazz))
                    .orElseThrow(() -> new MessageParsingException(clazz.getSimpleName()));
        } catch (final IOException e) {
            throw new MessageParsingException(clazz.getSimpleName());
        }
    }

    private <T extends RestObject> String serializeAndRethrow(final T object) {
        try {
            return RestMessageParser.toJson(object);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Serializing to JSON failed.", e);
        }
    }

    public void heartbeat() throws HttpRequestException {
        doHttpRequest("/api/v2/heartbeat", GET, null);
    }

    public SystemInfo retrieveSystemInfo() throws HttpRequestException, MessageParsingException {
        return parseAndRethrow(doHttpRequest("/api/v2/info", GET, null), SystemInfo.class);
    }

    public GroupInfo retrieveGroupInfo() throws HttpRequestException, MessageParsingException {
        return parseAndRethrow(doHttpRequest("/api/v2/group_info", GET, null), GroupInfo.class);
    }

    public SystemStatus retrieveSystemStatus() throws HttpRequestException, MessageParsingException {
        return parseAndRethrow(doHttpRequest("/api/v2/status", GET, null), SystemStatus.class);
    }

    public Settings retrieveSystemSettings() throws HttpRequestException, MessageParsingException {
        return parseAndRethrow(doHttpRequest("/api/v2/settings", GET, null), Settings.class);
    }

    public void sendBlink(final BlinkRequest request) throws HttpRequestException {
        doHttpRequest("/api/v2/blink", POST, serializeAndRethrow(request));
    }

    public void updateSettings(final Settings settings) throws HttpRequestException {
        doHttpRequest("/api/v2/settings", PUT, serializeAndRethrow(settings));
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    private String doHttpRequest(final String url, final String requestMethod, final String data)
            throws HttpRequestException {
        LOG.debug("JSON {}", data);

        final String encodedAuthCredentials = Base64
                .encodeBase64String((this.username + ":" + this.password).getBytes());
        try {
            final URL device = new URL(this.baseUrl + url);
            final HttpURLConnection connection = (HttpURLConnection) device.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthCredentials);

            // at the moment we only need GET method for heartbeat, but the there will be functionality that requires
            // POST
            switch (requestMethod) {
                case POST:
                case PUT:
                    connection.setDoOutput(true);
                    connection.addRequestProperty("Content-Type", "application/json");
                    connection.setRequestMethod(requestMethod);
                    connection.addRequestProperty("Content-Length", String.valueOf(data.length()));
                    try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
                        out.write(data);
                    }
                    break;
                case DELETE:
                    break;
                case GET:
                default:
                    // do nothing
            }

            final int responseCode = connection.getResponseCode();
            LOG.debug("Response code = {}", responseCode);
            if (responseCode < 400) {
                try (final BufferedReader inputBuffer = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    final StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = inputBuffer.readLine()) != null) {
                        result.append(line);
                    }
                    LOG.debug("Result: {}", result.toString());
                    return result.toString();
                }
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new UnauthorizedException("Unauthorized access to Renos API."
                        + " Please authenticate first before making any further requests.");
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new NotFoundException(
                        "Endpoint not found." + " Please make sure you are connected to a compatible device.");
            } else {
                LOG.debug("Message: {}", connection.getResponseMessage());
                return connection.getResponseMessage();
            }
        } catch (final HttpRequestException e) {
            throw e;
        } catch (final IOException e) {
            throw new HttpRequestException(e);
        }
    }
}
