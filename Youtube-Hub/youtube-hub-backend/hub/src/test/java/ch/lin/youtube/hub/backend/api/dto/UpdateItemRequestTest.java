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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UpdateItemRequestTest {

    private Validator validator;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void updateItemRequest_ShouldSetAndGetValues() {
        UpdateItemRequest request = new UpdateItemRequest();
        request.setDownloadTaskId("task-123");
        request.setFileSize(1024L);
        request.setFilePath("/tmp/video.mp4");
        request.setStatus(ProcessingStatus.DOWNLOADED);

        assertThat(request.getDownloadTaskId()).isEqualTo("task-123");
        assertThat(request.getFileSize()).isEqualTo(1024L);
        assertThat(request.getFilePath()).isEqualTo("/tmp/video.mp4");
        assertThat(request.getStatus()).isEqualTo(ProcessingStatus.DOWNLOADED);
    }

    @Test
    void updateItemRequest_ShouldFailValidation_WhenFileSizeIsNegative() {
        UpdateItemRequest request = new UpdateItemRequest();
        request.setFileSize(-1L);

        Set<ConstraintViolation<UpdateItemRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("fileSize cannot be negative.");
    }

    @Test
    void updateItemRequest_ShouldPassValidation_WhenFileSizeIsZeroOrPositive() {
        UpdateItemRequest request = new UpdateItemRequest();
        request.setFileSize(0L);

        Set<ConstraintViolation<UpdateItemRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
