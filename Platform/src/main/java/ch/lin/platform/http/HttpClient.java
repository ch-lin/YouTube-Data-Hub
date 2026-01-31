/*=============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Che-Hung Lin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *===========================================================================*/
package ch.lin.platform.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;

import ch.lin.platform.http.exception.HttpException;

/**
 * A reusable HTTP client for sending requests to a specific server. This client
 * uses the Apache HttpClient library to efficiently manage and reuse
 * connections. It is designed to be instantiated once for a target server (host
 * and port) and then reused for multiple requests.
 *
 * <p>
 * <b>Example Usage:</b>
 * </p>
 *
 * <pre>{@code
 * try (HttpClient client = new HttpClient(Scheme.HTTPS, "api.example.com", 443)) {
 *     String users = client.get("/users", null, null).body();
 *     String newPostJson = "{\"title\":\"foo\"}";
 *     String createdPost = client.post("/posts", null, newPostJson, null).body();
 * } catch (IOException | URISyntaxException e) {
 *     e.printStackTrace();
 * }
 * }</pre>
 * <p>
 * This class is AutoCloseable and should be used within a try-with-resources
 * block.
 */
public class HttpClient implements AutoCloseable {

    /**
     * A record to hold the HTTP response details.
     */
    public static record Response(int statusCode, String body) {

    }

    /**
     * The underlying Apache HTTP client.
     */
    private final CloseableHttpClient apacheClient;

    /**
     * The protocol scheme (HTTP or HTTPS).
     */
    private final Scheme scheme;

    /**
     * The target server host.
     */
    private final String host;

    /**
     * The target server port.
     */
    private final int port;

    /**
     * Constructs a new HttpClient configured for a specific server.
     *
     * @param scheme The protocol scheme, e.g., Scheme.HTTP or Scheme.HTTPS.
     * @param host The target server hostname or IP address.
     * @param port The target server port.
     */
    public HttpClient(Scheme scheme, String host, int port) {
        if (scheme == null) {
            throw new IllegalArgumentException("Scheme cannot be null.");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host cannot be null or blank.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port number must be between 1 and 65535.");
        }
        this.apacheClient = HttpClients.createDefault();
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }

    /**
     * Builds a URI for a request with the given path and parameters.
     *
     * @param path The resource path.
     * @param params A map of URL parameters. Can be null.
     * @return The constructed URI.
     * @throws URISyntaxException if the resulting URL is invalid.
     */
    private URI buildUri(String path, Map<String, String> params) throws URISyntaxException {
        URIBuilder builder = new URIBuilder()
                .setScheme(scheme.toString())
                .setHost(host)
                .setPort(port)
                .setPath(path);

        if (params != null) {
            params.forEach(builder::addParameter);
        }
        return builder.build();
    }

    /**
     * Sends an HTTP GET request to a specific resource path on the configured
     * server.
     *
     * @param path The resource path (e.g., "/api/resource").
     * @param params A map of URL parameters. Can be null.
     * @param headers A map of request headers. Can be null.
     * @return A {@link Response} object containing the status code and body.
     * @throws IOException if a general I/O error occurs.
     * @throws HttpException if the server returns a non-2xx status code.
     * @throws URISyntaxException if the resulting URL is invalid.
     */
    public Response get(String path, Map<String, String> params, Map<String, String> headers)
            throws IOException, URISyntaxException {
        URI uri = buildUri(path, params);
        HttpGet request = new HttpGet(uri);

        if (headers != null) {
            headers.forEach(request::setHeader);
        }

        return apacheClient.execute(request, response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpException("HTTP GET request failed", statusCode, responseBody);
            }
            return new Response(statusCode, responseBody);
        });
    }

    /**
     * Sends an HTTP POST request with a JSON body to a specific resource path
     * on the configured server.
     *
     * @param path The resource path (e.g., "/api/resource").
     * @param params A map of URL parameters to be appended to the URL. Can be
     * null.
     * @param jsonBody The JSON string to be sent as the request body.
     * @param headers A map of request headers. Can be null.
     * @return A {@link Response} object containing the status code and body.
     * @throws IOException if a general I/O error occurs.
     * @throws HttpException if the server returns a non-2xx status code.
     * @throws URISyntaxException if the resulting URL is invalid.
     */
    public Response post(String path, Map<String, String> params, String jsonBody, Map<String, String> headers)
            throws IOException, URISyntaxException {
        URI uri = buildUri(path, params);
        HttpPost request = new HttpPost(uri);

        if (headers != null) {
            headers.forEach(request::setHeader);
        }

        if (jsonBody != null) {
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        }

        return apacheClient.execute(request, response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpException("HTTP POST request failed", statusCode, responseBody);
            }
            return new Response(statusCode, responseBody);
        });
    }

    /**
     * Sends an HTTP PUT request with a JSON body.
     *
     * @param path The resource path.
     * @param params A map of URL parameters. Can be null.
     * @param jsonBody The JSON string for the request body. Can be null.
     * @param headers A map of request headers. Can be null.
     * @return A {@link Response} object containing the status code and body.
     * @throws IOException if an I/O error occurs.
     * @throws URISyntaxException if the URL is invalid.
     */
    public Response put(String path, Map<String, String> params, String jsonBody, Map<String, String> headers)
            throws IOException, URISyntaxException {
        URI uri = buildUri(path, params);
        HttpPut request = new HttpPut(uri);

        if (headers != null) {
            headers.forEach(request::setHeader);
        }

        if (jsonBody != null) {
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        }

        return apacheClient.execute(request, response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpException("HTTP PUT request failed", statusCode, responseBody);
            }
            return new Response(statusCode, responseBody);
        });
    }

    /**
     * Sends an HTTP PATCH request with a JSON body.
     *
     * @param path The resource path.
     * @param params A map of URL parameters. Can be null.
     * @param jsonBody The JSON string for the request body. Can be null.
     * @param headers A map of request headers. Can be null.
     * @return A {@link Response} object containing the status code and body.
     * @throws IOException if an I/O error occurs.
     * @throws URISyntaxException if the URL is invalid.
     */
    public Response patch(String path, Map<String, String> params, String jsonBody, Map<String, String> headers)
            throws IOException, URISyntaxException {
        URI uri = buildUri(path, params);
        HttpPatch request = new HttpPatch(uri);
        if (headers != null) {
            headers.forEach(request::setHeader);
        }
        if (jsonBody != null) {
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        }
        return apacheClient.execute(request, response -> {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpException("HTTP PATCH request failed", statusCode, responseBody);
            }
            return new Response(statusCode, responseBody);
        });
    }

    /**
     * Closes the underlying HTTP client and releases any system resources. This
     * method is automatically called when the client is used in a
     * try-with-resources statement.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        apacheClient.close();
    }

}
