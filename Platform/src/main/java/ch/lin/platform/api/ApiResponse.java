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
package ch.lin.platform.api;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.MDC;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * A generic and immutable wrapper for all API responses.
 * <p>
 * This class provides a consistent structure for API responses, including a
 * timestamp, correlation ID for tracking, status (success/failure), and the
 * actual data payload. It uses static factory methods for convenient creation
 * of success or failure responses. Fields with null values are omitted from the
 * JSON output thanks to the {@code @JsonInclude(JsonInclude.Include.NON_NULL)}
 * annotation.
 *
 * @param <T> The type of the data payload.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * The key used to retrieve the correlation ID from the SLF4J Mapped
     * Diagnostic Context (MDC).
     */
    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlationId";

    /**
     * The timestamp when the response was generated.
     */
    private final OffsetDateTime timestamp;

    /**
     * The correlation ID for tracing the request through the system. Retrieved
     * from the {@link MDC}.
     */
    private final String correlationId;

    /**
     * The status of the response, either "success" or "failure".
     */
    private final String status;

    /**
     * The generic data payload of the response. For failures, this might
     * contain an {@link ApiError} object.
     */
    private final T data;

    /**
     * An optional list of non-critical warnings that occurred during the
     * operation.
     */
    private final List<String> warnings;

    /**
     * The primary private constructor to create an ApiResponse instance.
     *
     * @param success A boolean indicating if the operation was successful.
     * @param data The data payload.
     * @param warnings A list of non-critical warnings, can be null.
     */
    private ApiResponse(boolean success, T data, List<String> warnings) {
        this.timestamp = OffsetDateTime.now();
        this.correlationId = MDC.get(CORRELATION_ID_LOG_VAR_NAME);
        this.status = success ? "success" : "failure";
        this.data = data;
        this.warnings = warnings;
    }

    /**
     * A private convenience constructor that defaults to having no warnings.
     *
     * @param success A boolean indicating if the operation was successful.
     * @param data The data payload.
     */
    private ApiResponse(boolean success, T data) {
        this(success, data, null);
    }

    /**
     * Creates a successful ApiResponse.
     *
     * @param data The data payload for the successful response.
     * @param <T> The type of the data payload.
     * @return A new ApiResponse instance with a "success" status.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data);
    }

    /**
     * Creates a successful ApiResponse with warnings.
     *
     * @param data The data payload for the successful response.
     * @param warnings A list of non-critical warnings.
     * @param <T> The type of the data payload.
     * @return A new ApiResponse instance with a "success" status and warnings.
     */
    public static <T> ApiResponse<T> success(T data, List<String> warnings) {
        return new ApiResponse<>(true, data, warnings);
    }

    /**
     * Creates a failure ApiResponse.
     *
     * @param data The error payload for the failure response, typically an
     * {@link ApiError}.
     * @param <T> The type of the error payload.
     * @return A new ApiResponse instance with a "failure" status.
     */
    public static <T> ApiResponse<T> failure(T data) {
        return new ApiResponse<>(false, data);
    }
}
