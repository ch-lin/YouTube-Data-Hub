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
import java.lang.reflect.Field;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.lin.platform.http.exception.HttpException;

class HttpClientTest {

    private HttpClient httpClient;
    private CloseableHttpClient mockApacheClient;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() throws Exception {
        // Initialize with dummy values, the internal client will be replaced
        httpClient = new HttpClient(Scheme.HTTP, "localhost", 8080);
        mockApacheClient = mock(CloseableHttpClient.class);

        // Use reflection to inject the mock client into the private final field
        Field clientField = HttpClient.class.getDeclaredField("apacheClient");
        clientField.setAccessible(true);
        // Close the real client created in constructor to avoid resource leak warnings
        ((CloseableHttpClient) clientField.get(httpClient)).close();
        clientField.set(httpClient, mockApacheClient);
    }

    @AfterEach
    @SuppressWarnings("unused")
    void tearDown() throws IOException {
        httpClient.close();
    }

    @Test
    void constructor_ShouldThrowException_WhenInputsAreInvalid() {
        assertThatThrownBy(() -> new HttpClient(null, "host", 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scheme cannot be null.");

        assertThatThrownBy(() -> new HttpClient(Scheme.HTTP, null, 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Host cannot be null or blank.");

        assertThatThrownBy(() -> new HttpClient(Scheme.HTTP, "  ", 80))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Host cannot be null or blank.");

        assertThatThrownBy(() -> new HttpClient(Scheme.HTTP, "host", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Port number must be between 1 and 65535.");

        assertThatThrownBy(() -> new HttpClient(Scheme.HTTP, "host", 65536))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Port number must be between 1 and 65535.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_ShouldReturnResponse_WhenSuccess() throws Exception {
        mockResponse(200, "{\"status\":\"ok\"}");

        HttpClient.Response response = httpClient.get("/api/test", Map.of("q", "1"), Map.of("Auth", "token"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"ok\"}");

        ArgumentCaptor<ClassicHttpRequest> captor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(mockApacheClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        ClassicHttpRequest request = captor.getValue();

        assertThat(request).isInstanceOf(HttpGet.class);
        assertThat(request.getUri().toString()).isEqualTo("http://localhost:8080/api/test?q=1");
        assertThat(request.getHeader("Auth").getValue()).isEqualTo("token");
    }

    @Test
    void get_ShouldThrowHttpException_WhenStatusCodeIsError() throws IOException {
        mockResponse(404, "Not Found");

        assertThatThrownBy(() -> httpClient.get("/api/test", null, null))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP GET request failed");
    }

    @Test
    void get_ShouldThrowHttpException_WhenStatusCodeIsLessThan200() throws IOException {
        mockResponse(199, "Info");

        assertThatThrownBy(() -> httpClient.get("/api/test", null, null))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP GET request failed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void post_ShouldReturnResponse_WhenSuccess() throws Exception {
        mockResponse(201, "Created");

        String jsonBody = "{\"name\":\"test\"}";
        HttpClient.Response response = httpClient.post("/api/create", null, jsonBody, Map.of("Content-Type", "application/json"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("Created");

        ArgumentCaptor<ClassicHttpRequest> captor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(mockApacheClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        ClassicHttpRequest request = captor.getValue();

        assertThat(request).isInstanceOf(HttpPost.class);
        assertThat(request.getEntity()).isNotNull();
        assertThat(request.getHeader("Content-Type").getValue()).isEqualTo("application/json");
    }

    @Test
    void post_ShouldThrowHttpException_WhenStatusCodeIsError() throws IOException {
        mockResponse(500, "Server Error");

        assertThatThrownBy(() -> httpClient.post("/api/create", null, "{}", null))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP POST request failed");
    }

    @Test
    void post_ShouldThrowHttpException_WhenStatusCodeIsLessThan200() throws IOException {
        mockResponse(199, "Info");

        assertThatThrownBy(() -> httpClient.post("/api/create", null, "{}", null))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP POST request failed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void post_ShouldReturnResponse_WhenJsonBodyIsNull() throws Exception {
        mockResponse(200, "OK");

        HttpClient.Response response = httpClient.post("/api/create", null, null, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");

        ArgumentCaptor<ClassicHttpRequest> captor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(mockApacheClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        ClassicHttpRequest request = captor.getValue();

        assertThat(request).isInstanceOf(HttpPost.class);
        assertThat(request.getEntity()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void put_ShouldReturnResponse_WhenSuccess() throws Exception {
        mockResponse(200, "Updated");

        HttpClient.Response response = httpClient.put("/api/update", null, "{}", Map.of("X-Custom", "val"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Updated");

        ArgumentCaptor<ClassicHttpRequest> captor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(mockApacheClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        ClassicHttpRequest request = captor.getValue();
        assertThat(request).isInstanceOf(HttpPut.class);
        assertThat(request.getHeader("X-Custom").getValue()).isEqualTo("val");
    }

    @Test
    void put_ShouldThrowHttpException_WhenStatusCodeIsError() throws IOException {
        mockResponse(400, "Bad Request");

        assertThatThrownBy(() -> httpClient.put("/api/update", null, "{}", null))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP PUT request failed");
    }

    @Test
    void put_ShouldThrowHttpException_WhenStatusCodeIsLessThan200() throws IOException {
        mockResponse(199, "Info");

        assertThatThrownBy(() -> httpClient.put("/api/update", null, "{}", null))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP PUT request failed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void put_ShouldReturnResponse_WhenJsonBodyIsNull() throws Exception {
        mockResponse(200, "OK");

        HttpClient.Response response = httpClient.put("/api/update", null, null, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");

        ArgumentCaptor<ClassicHttpRequest> captor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(mockApacheClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        ClassicHttpRequest request = captor.getValue();

        assertThat(request).isInstanceOf(HttpPut.class);
        assertThat(request.getEntity()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void patch_ShouldReturnResponse_WhenSuccess() throws Exception {
        mockResponse(200, "Patched");

        HttpClient.Response response = httpClient.patch("/api/patch", null, "{}", Map.of("If-Match", "123"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Patched");

        ArgumentCaptor<ClassicHttpRequest> captor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(mockApacheClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        ClassicHttpRequest request = captor.getValue();
        assertThat(request).isInstanceOf(HttpPatch.class);
        assertThat(request.getHeader("If-Match").getValue()).isEqualTo("123");
    }

    @Test
    void patch_ShouldThrowHttpException_WhenStatusCodeIsError() throws IOException {
        mockResponse(500, "Internal Error");

        assertThatThrownBy(() -> httpClient.patch("/api/patch", null, "{}", null))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP PATCH request failed");
    }

    @Test
    void patch_ShouldThrowHttpException_WhenStatusCodeIsLessThan200() throws IOException {
        mockResponse(199, "Info");

        assertThatThrownBy(() -> httpClient.patch("/api/patch", null, "{}", null))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP PATCH request failed");
    }

    @Test
    @SuppressWarnings("unchecked")
    void patch_ShouldReturnResponse_WhenJsonBodyIsNull() throws Exception {
        mockResponse(200, "OK");

        HttpClient.Response response = httpClient.patch("/api/patch", null, null, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");

        ArgumentCaptor<ClassicHttpRequest> captor = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(mockApacheClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        ClassicHttpRequest request = captor.getValue();

        assertThat(request).isInstanceOf(HttpPatch.class);
        assertThat(request.getEntity()).isNull();
    }

    @Test
    void close_ShouldCloseApacheClient() throws Exception {
        httpClient.close();
        verify(mockApacheClient).close();
    }

    @Test
    void responseRecord_ShouldHoldData() {
        HttpClient.Response response = new HttpClient.Response(200, "OK");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("OK");
    }

    @SuppressWarnings("unchecked")
    private void mockResponse(int statusCode, String body) throws IOException {
        when(mockApacheClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                    ClassicHttpResponse mockResponse = mock(ClassicHttpResponse.class);
                    when(mockResponse.getCode()).thenReturn(statusCode);
                    // Use StringEntity to allow EntityUtils.toString() to work
                    when(mockResponse.getEntity()).thenReturn(new StringEntity(body));

                    // Simulate the handler behavior (which is implemented as a lambda in HttpClient)
                    // The lambda in HttpClient expects the response to be passed to it.
                    // However, since we can't easily invoke the lambda logic without duplicating it or 
                    // relying on the implementation detail that the lambda IS the handler,
                    // we rely on the fact that HttpClient passes a lambda that implements HttpClientResponseHandler.
                    // We invoke that handler with our mocked response.
                    return handler.handleResponse(mockResponse);
                });
    }
}
