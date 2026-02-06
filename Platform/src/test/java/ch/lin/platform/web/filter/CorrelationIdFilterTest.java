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
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        filter = new CorrelationIdFilter();
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    @SuppressWarnings("unused")
    void tearDown() {
        MDC.clear();
    }

    @Test
    void doFilterInternal_ShouldGenerateCorrelationId_WhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            String mdcCorrelationId = MDC.get("correlationId");
            assertThat(mdcCorrelationId).isNotNull().isNotBlank();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, Objects.requireNonNull(filterChain));

        String responseHeader = response.getHeader("X-Correlation-ID");
        assertThat(responseHeader).isNotNull().isNotBlank();
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get("correlationId")).isNull(); // Should be cleared after
    }

    @Test
    void doFilterInternal_ShouldGenerateCorrelationId_WhenHeaderIsBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            String mdcCorrelationId = MDC.get("correlationId");
            assertThat(mdcCorrelationId).isNotNull().isNotBlank();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, Objects.requireNonNull(filterChain));

        String responseHeader = response.getHeader("X-Correlation-ID");
        assertThat(responseHeader).isNotNull().isNotBlank();
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get("correlationId")).isNull(); // Should be cleared after
    }

    @Test
    void doFilterInternal_ShouldUseExistingCorrelationId_WhenHeaderPresent() throws ServletException, IOException {
        String existingCorrelationId = "test-correlation-id";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", existingCorrelationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, Objects.requireNonNull(filterChain));

        String responseHeader = response.getHeader("X-Correlation-ID");
        assertThat(responseHeader).isEqualTo(existingCorrelationId);
        verify(filterChain).doFilter(request, response);
    }
}
