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

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.lin.platform.api.ApiError;
import ch.lin.platform.api.ApiResponse;
import ch.lin.youtube.hub.backend.api.common.exception.ChannelAlreadyExistsException;
import ch.lin.youtube.hub.backend.api.common.exception.ChannelNotFoundException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;

class WebGlobalExceptionHandlerTest {

    private WebGlobalExceptionHandler exceptionHandler;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        exceptionHandler = new WebGlobalExceptionHandler();
    }

    @Test
    void handleNotFoundExceptions_ShouldReturnNotFound() {
        ChannelNotFoundException ex = new ChannelNotFoundException("Channel not found");
        ResponseEntity<ApiResponse<ApiError>> response = exceptionHandler.handleNotFoundExceptions(ex);

        ApiResponse<ApiError> apiResponse = response.getBody();
        Objects.requireNonNull(apiResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(apiResponse.getStatus()).isEqualTo("failure");
        assertThat(apiResponse.getData().getCode()).isEqualTo("NOT_FOUND");
        assertThat(apiResponse.getData().getMessage()).isEqualTo("Channel not found");
    }

    @Test
    void handleConflictExceptions_ShouldReturnConflict() {
        ChannelAlreadyExistsException ex = new ChannelAlreadyExistsException("Channel already exists");
        ResponseEntity<ApiResponse<ApiError>> response = exceptionHandler.handleConflictExceptions(ex);

        ApiResponse<ApiError> apiResponse = response.getBody();
        Objects.requireNonNull(apiResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(apiResponse.getStatus()).isEqualTo("failure");
        assertThat(apiResponse.getData().getCode()).isEqualTo("CONFLICT");
        assertThat(apiResponse.getData().getMessage()).isEqualTo("Channel already exists");
    }

    @Test
    void handleYoutubeApiRequestException_ShouldReturnInternalServerError() {
        YoutubeApiRequestException ex = new YoutubeApiRequestException("API Error");
        ResponseEntity<ApiResponse<ApiError>> response = exceptionHandler.handleYoutubeApiRequestException(ex);

        ApiResponse<ApiError> apiResponse = response.getBody();
        Objects.requireNonNull(apiResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(apiResponse.getStatus()).isEqualTo("failure");
        assertThat(apiResponse.getData().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(apiResponse.getData().getMessage()).isEqualTo("API Error");
    }

    @Test
    void handleYoutubeApiAuthException_ShouldReturnUnauthorized() {
        YoutubeApiAuthException ex = new YoutubeApiAuthException("Auth Failed", new RuntimeException());
        ResponseEntity<ApiResponse<ApiError>> response = exceptionHandler.handleYoutubeApiAuthException(ex);

        ApiResponse<ApiError> apiResponse = response.getBody();
        Objects.requireNonNull(apiResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(apiResponse.getStatus()).isEqualTo("failure");
        assertThat(apiResponse.getData().getCode()).isEqualTo("YOUTUBE_API_AUTH_ERROR");
        assertThat(apiResponse.getData().getMessage()).isEqualTo("Auth Failed");
    }
}
