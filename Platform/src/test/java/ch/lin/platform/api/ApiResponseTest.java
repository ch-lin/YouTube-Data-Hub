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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class ApiResponseTest {

    @AfterEach
    @SuppressWarnings("unused")
    void tearDown() {
        MDC.clear();
    }

    @Test
    void success_ShouldCreateSuccessResponse() {
        String data = "test data";
        MDC.put("correlationId", "12345");

        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getCorrelationId()).isEqualTo("12345");
        assertThat(response.getWarnings()).isNull();
    }

    @Test
    void success_WithWarnings_ShouldCreateSuccessResponseWithWarnings() {
        String data = "test data";
        List<String> warnings = List.of("warning 1", "warning 2");

        ApiResponse<String> response = ApiResponse.success(data, warnings);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getWarnings()).containsExactlyElementsOf(warnings);
    }

    @Test
    void failure_ShouldCreateFailureResponse() {
        ApiError error = new ApiError("CODE", "Message");
        ApiResponse<ApiError> response = ApiResponse.failure(error);
        assertThat(response.getStatus()).isEqualTo("failure");
        assertThat(response.getData()).isEqualTo(error);
    }
}
