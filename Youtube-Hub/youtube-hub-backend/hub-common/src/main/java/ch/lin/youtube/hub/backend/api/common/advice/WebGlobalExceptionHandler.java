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
package ch.lin.youtube.hub.backend.api.common.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import ch.lin.platform.api.ApiError;
import ch.lin.platform.api.ApiResponse;
import ch.lin.platform.web.advice.BaseGlobalExceptionHandler;
import ch.lin.youtube.hub.backend.api.common.exception.ChannelAlreadyExistsException;
import ch.lin.youtube.hub.backend.api.common.exception.ChannelNotFoundException;
import ch.lin.youtube.hub.backend.api.common.exception.ItemAlreadyExistsException;
import ch.lin.youtube.hub.backend.api.common.exception.ItemNotFoundException;
import ch.lin.youtube.hub.backend.api.common.exception.PlaylistAlreadyExistsException;
import ch.lin.youtube.hub.backend.api.common.exception.PlaylistNotFoundException;
import ch.lin.youtube.hub.backend.api.common.exception.TagAlreadyExistsException;
import ch.lin.youtube.hub.backend.api.common.exception.TagNotFoundException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;

/**
 * Global exception handler for the web layer.
 * <p>
 * This class uses {@link ControllerAdvice} to intercept exceptions thrown by
 * controllers and maps them to a standardized {@link ApiResponse} format. This
 * ensures that clients receive consistent and structured error responses for
 * different failure scenarios.
 */
@ControllerAdvice
public class WebGlobalExceptionHandler extends BaseGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebGlobalExceptionHandler.class);

    /**
     * Handles exceptions for resources that could not be found.
     * <p>
     * Catches various "Not Found" exceptions (e.g.,
     * {@link ChannelNotFoundException}) and returns an {@link ApiResponse} with
     * a "NOT_FOUND" error code. The HTTP status is expected to be set to 404
     * (Not Found) via the {@code @ResponseStatus} annotation on the exception
     * classes.
     *
     * @param ex The caught exception (e.g., {@link ChannelNotFoundException}).
     * @return An {@link ApiResponse} containing the error details.
     */
    @ExceptionHandler({
        ChannelNotFoundException.class,
        PlaylistNotFoundException.class,
        ItemNotFoundException.class,
        TagNotFoundException.class
    })
    public ResponseEntity<ApiResponse<ApiError>> handleNotFoundExceptions(RuntimeException ex) {
        logger.error("Resource not found: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError("NOT_FOUND", ex.getMessage());
        ApiResponse<ApiError> response = ApiResponse.failure(apiError);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles exceptions for resources that already exist.
     * <p>
     * Catches various "Already Exists" exceptions (e.g.,
     * {@link ChannelAlreadyExistsException}) and returns an {@link ApiResponse}
     * with a "CONFLICT" error code. The HTTP status is expected to be set to
     * 409 (Conflict) via the {@code @ResponseStatus} annotation on the
     * exception classes.
     *
     * @param ex The caught exception (e.g.,
     * {@link ChannelAlreadyExistsException}).
     * @return An {@link ApiResponse} containing the error details.
     */
    @ExceptionHandler({
        ChannelAlreadyExistsException.class,
        ItemAlreadyExistsException.class,
        PlaylistAlreadyExistsException.class,
        TagAlreadyExistsException.class
    })
    public ResponseEntity<ApiResponse<ApiError>> handleConflictExceptions(RuntimeException ex) {
        logger.error("Conflict detected: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError("CONFLICT", ex.getMessage());
        ApiResponse<ApiError> response = ApiResponse.failure(apiError);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handles exceptions related to external YouTube API requests.
     * <p>
     * Catches {@link YoutubeApiRequestException} and returns an
     * {@link ApiResponse} with an "INTERNAL_SERVER_ERROR" error code. This
     * prevents leaking internal API error details to the client. The HTTP
     * status is expected to be set to 500 (Internal Server Error) via the
     * {@code @ResponseStatus} annotation on the exception class.
     *
     * @param ex The caught {@link YoutubeApiRequestException}.
     * @return An {@link ApiResponse} containing a generic error message.
     */
    @ExceptionHandler({
        YoutubeApiRequestException.class
    })
    public ResponseEntity<ApiResponse<ApiError>> handleYoutubeApiRequestException(RuntimeException ex) {
        logger.error("Youtube fetch error: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError("INTERNAL_SERVER_ERROR", ex.getMessage());
        ApiResponse<ApiError> response = ApiResponse.failure(apiError);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles exceptions related to YouTube Data API authentication failures.
     *
     * @param ex The caught {@link YoutubeApiAuthException}.
     * @return A {@link ResponseEntity} with a 401 Unauthorized status and a
     * structured error message.
     */
    @ExceptionHandler(YoutubeApiAuthException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleYoutubeApiAuthException(YoutubeApiAuthException ex) {
        logger.error("YouTube API authentication failed: {}", ex.getMessage());
        ApiError error = new ApiError("YOUTUBE_API_AUTH_ERROR", ex.getMessage());
        ApiResponse<ApiError> response = ApiResponse.failure(error);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
}
