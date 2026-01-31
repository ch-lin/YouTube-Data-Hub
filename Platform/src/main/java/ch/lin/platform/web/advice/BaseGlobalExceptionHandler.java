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
package ch.lin.platform.web.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import ch.lin.platform.api.ApiError;
import ch.lin.platform.api.ApiResponse;
import ch.lin.platform.exception.ConfigCreationException;
import ch.lin.platform.exception.ConfigNotFoundException;
import ch.lin.platform.exception.InvalidRequestException;

public abstract class BaseGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);

    /**
     * Handles exceptions related to invalid client requests, such as bad input.
     * <p>
     * This handler catches {@link InvalidRequestException}, logs the error, and
     * maps it to a standardized {@link ApiResponse} with a "INVALID_REQUEST"
     * error code and a 400 (Bad Request) HTTP status.
     *
     * @param ex The caught {@link InvalidRequestException}.
     * @return A {@link ResponseEntity} containing the structured error response
     * with an HTTP 400 status.
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleInvalidRequestException(InvalidRequestException ex) {
        logger.error("Invalid request received: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError("INVALID_REQUEST", ex.getMessage());
        ApiResponse<ApiError> response = ApiResponse.failure(apiError);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles {@link ConfigNotFoundException} and returns a 404 Not Found.
     *
     * @param ex The caught {@link ConfigNotFoundException}.
     * @return An {@link ApiResponse} containing the error details.
     */
    @ExceptionHandler(ConfigNotFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConfigNotFoundException(ConfigNotFoundException ex) {
        logger.warn("Configuration not found: {}", ex.getMessage());
        ApiError apiError = new ApiError("NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.failure(apiError), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles {@link ConfigCreationException} and returns a 500 Internal Server
     * Error.
     *
     * @param ex The caught {@link ConfigCreationException}.
     * @return An {@link ApiResponse} containing the error details.
     */
    @ExceptionHandler(ConfigCreationException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConfigCreationException(ConfigCreationException ex) {
        logger.error("Configuration creation error: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError("INTERNAL_SERVER_ERROR", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.failure(apiError), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiError>> handleGeneralException(Exception ex) {
        logger.error("Unexpected system error", ex);
        ApiError apiError = new ApiError("INTERNAL_SERVER_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(apiError));
    }
}
