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
package ch.lin.youtube.hub.backend.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ProcessRequestTest {

    private Validator validator;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void processRequest_ShouldSetAndGetValues() {
        ProcessRequest request = new ProcessRequest();
        request.setApiKey("key");
        request.setConfigName("config");
        request.setDelayInMilliseconds(200L);
        OffsetDateTime now = OffsetDateTime.now();
        request.setPublishedAfter(now);
        request.setForcePublishedAfter(true);
        request.setChannelIds(List.of("ch1"));

        assertThat(request.getApiKey()).isEqualTo("key");
        assertThat(request.getConfigName()).isEqualTo("config");
        assertThat(request.getDelayInMilliseconds()).isEqualTo(200L);
        assertThat(request.getPublishedAfter()).isEqualTo(now);
        assertThat(request.getForcePublishedAfter()).isTrue();
        assertThat(request.getChannelIds()).containsExactly("ch1");
    }

    @Test
    void processRequest_ShouldFailValidation_WhenDelayIsNegative() {
        ProcessRequest request = new ProcessRequest();
        request.setDelayInMilliseconds(-1L);

        Set<ConstraintViolation<ProcessRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Delay must be a non-negative number.");
    }

    @Test
    void processRequest_ShouldHaveDefaultValues() {
        ProcessRequest request = new ProcessRequest();
        assertThat(request.getDelayInMilliseconds()).isEqualTo(100L);
        assertThat(request.getForcePublishedAfter()).isFalse();
    }
}
