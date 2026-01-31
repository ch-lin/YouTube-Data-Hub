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
package ch.lin.platform.web.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A servlet filter that manages a correlation ID for each incoming HTTP
 * request.
 * <p>
 * This filter ensures every request is assigned a unique correlation ID, which
 * is crucial for tracing a request's lifecycle across various services and
 * through logs. It first checks for an existing {@code X-Correlation-ID}
 * header. If one is not present or is blank, a new UUID is generated to serve
 * as the ID.
 * <p>
 * The resulting correlation ID is then:
 * <ol>
 * <li>Placed into the SLF4J Mapped Diagnostic Context (MDC) with the key
 * "correlationId", making it available for structured logging throughout the
 * request's processing. 2. Added to the {@code X-Correlation-ID} header in the
 * HTTP response, allowing clients to correlate their requests with server-side
 * logs.
 * </ol>
 * <p>
 * This filter is ordered with the highest precedence to ensure the correlation
 * ID is available for all subsequent processing steps.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /**
     * The name of the HTTP header used to carry the correlation ID.
     */
    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";
    /**
     * The key used to store the correlation ID in the SLF4J Mapped Diagnostic
     * Context (MDC).
     */
    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    /**
     * {@inheritDoc}
     * <p>
     * This method handles the core logic of retrieving or generating a
     * correlation ID, setting it in the MDC and the response header, and then
     * passing the request down the filter chain. A {@code try...finally} block
     * ensures the MDC is cleaned up after the request is processed, preventing
     * memory leaks in a thread-pooled environment.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String correlationId = request.getHeader(CORRELATION_ID_HEADER_NAME);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            MDC.put(CORRELATION_ID_LOG_VAR_NAME, correlationId);
            response.setHeader(CORRELATION_ID_HEADER_NAME, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_LOG_VAR_NAME);
        }
    }
}
