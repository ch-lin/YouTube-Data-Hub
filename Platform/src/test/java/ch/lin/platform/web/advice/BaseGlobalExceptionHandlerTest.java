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

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.lin.platform.api.ApiError;
import ch.lin.platform.api.ApiResponse;
import ch.lin.platform.exception.ConfigCreationException;
import ch.lin.platform.exception.ConfigNotFoundException;
import ch.lin.platform.exception.InvalidRequestException;

class BaseGlobalExceptionHandlerTest {

    private BaseGlobalExceptionHandler exceptionHandler;

    // Concrete implementation for testing abstract class
    private static class TestGlobalExceptionHandler extends BaseGlobalExceptionHandler {
    }

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        exceptionHandler = new TestGlobalExceptionHandler();
    }

    @Test
    void handleInvalidRequestException_ShouldReturnBadRequest() {
        InvalidRequestException ex = new InvalidRequestException("Invalid input");
        ResponseEntity<ApiResponse<ApiError>> response = exceptionHandler.handleInvalidRequestException(ex);

        ApiResponse<ApiError> apiResponse = response.getBody();
        Objects.requireNonNull(apiResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(apiResponse.getStatus()).isEqualTo("failure");
        assertThat(apiResponse.getData().getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(apiResponse.getData().getMessage()).isEqualTo("Invalid input");
    }

    @Test
    void handleConfigNotFoundException_ShouldReturnNotFound() {
        ConfigNotFoundException ex = new ConfigNotFoundException("Config not found");
        ResponseEntity<ApiResponse<ApiError>> response = exceptionHandler.handleConfigNotFoundException(ex);

        ApiResponse<ApiError> apiResponse = response.getBody();
        Objects.requireNonNull(apiResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(apiResponse.getStatus()).isEqualTo("failure");
        assertThat(apiResponse.getData().getCode()).isEqualTo("NOT_FOUND");
        assertThat(apiResponse.getData().getMessage()).isEqualTo("Config not found");
    }

    @Test
    void handleConfigCreationException_ShouldReturnInternalServerError() {
        ConfigCreationException ex = new ConfigCreationException("Creation failed");
        ResponseEntity<ApiResponse<ApiError>> response = exceptionHandler.handleConfigCreationException(ex);

        ApiResponse<ApiError> apiResponse = response.getBody();
        Objects.requireNonNull(apiResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(apiResponse.getStatus()).isEqualTo("failure");
        assertThat(apiResponse.getData().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(apiResponse.getData().getMessage()).isEqualTo("Creation failed");
    }

    @Test
    void handleGeneralException_ShouldReturnInternalServerError() {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<ApiResponse<ApiError>> response = exceptionHandler.handleGeneralException(ex);

        ApiResponse<ApiError> apiResponse = response.getBody();
        Objects.requireNonNull(apiResponse);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(apiResponse.getStatus()).isEqualTo("failure");
        assertThat(apiResponse.getData().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(apiResponse.getData().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
