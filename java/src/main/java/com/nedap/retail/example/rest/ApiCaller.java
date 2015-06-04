package com.nedap.retail.example.rest;

import com.nedap.retail.renos.api.v2.rest.RestMessageParser;
import com.nedap.retail.renos.api.v2.rest.message.BlinkRequest;
import com.nedap.retail.renos.api.v2.rest.message.Settings;
import com.nedap.retail.renos.api.v2.rest.message.SystemInfo;
import com.nedap.retail.renos.api.v2.rest.message.SystemStatus;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

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
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
    }

    public void heartbeat() throws Exception {
        doHttpRequest("/api/v2/heartbeat", GET, null);
    }

    public SystemInfo retrieveSystemInfo() throws Exception {
        final String info = doHttpRequest("/api/v2/info", GET, null);
        return RestMessageParser.parse(info, SystemInfo.class);
    }

    public SystemStatus retrieveSystemStatus() throws Exception {
        final String status = doHttpRequest("/api/v2/status", GET, null);
        return RestMessageParser.parse(status, SystemStatus.class);
    }

    public Settings retrieveSystemSettings() throws Exception {
        final String status = doHttpRequest("/api/v2/settings", GET, null);
        return RestMessageParser.parse(status, Settings.class);
    }

    public void sendBlink(final BlinkRequest request) throws Exception {
        final String json = RestMessageParser.toJson(request);
        doHttpRequest("/api/v2/blink", POST, json);
    }

    public void updateSettings(final Settings settings) throws Exception {
        final String json = RestMessageParser.toJson(settings);
        doHttpRequest("/api/v2/settings", PUT, json);
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    private String doHttpRequest(final String url, final String requestMethod, final String data) throws Exception {
        LOG.debug("JSON {}", data);

        final String encodedAuthCredentials = Base64.encodeBase64String((username + ":" + password).getBytes());
        final URL device = new URL(this.baseUrl + url);
        final HttpURLConnection connection = (HttpURLConnection) device.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Authorization", "Basic " + encodedAuthCredentials);

        // at the moment we only need GET method for heartbeat, but the there will be functionality that requires POST
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
            try (final BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                final StringBuilder result = new StringBuilder();
                String line;
                while ((line = inputBuffer.readLine()) != null) {
                    result.append(line);
                }
                LOG.debug("Result: {}", result.toString());
                return result.toString();
            }
        } else {
            LOG.debug("Message: {}", connection.getResponseMessage());
            return connection.getResponseMessage();
        }
    }

}
