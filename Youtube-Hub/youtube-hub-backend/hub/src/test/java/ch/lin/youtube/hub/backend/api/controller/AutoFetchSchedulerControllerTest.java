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
package ch.lin.youtube.hub.backend.api.controller;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.lin.youtube.hub.backend.api.app.service.AutoFetchSchedulerService;

@ExtendWith(MockitoExtension.class)
class AutoFetchSchedulerControllerTest {

    @Mock
    private AutoFetchSchedulerService autoFetchSchedulerService;

    private AutoFetchSchedulerController controller;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        controller = new AutoFetchSchedulerController(autoFetchSchedulerService);
    }

    @Test
    void startScheduler_ShouldCallServiceAndReturnOk() {
        ResponseEntity<String> response = controller.startScheduler();

        verify(autoFetchSchedulerService).start();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Fetch scheduler started.");
    }

    @Test
    void stopScheduler_ShouldCallServiceAndReturnOk() {
        ResponseEntity<String> response = controller.stopScheduler();

        verify(autoFetchSchedulerService).stop();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Fetch scheduler stopped.");
    }

    @Test
    void getSchedulerStatus_ShouldReturnStatus() {
        when(autoFetchSchedulerService.isSchedulerRunning()).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = controller.getSchedulerStatus();
        Map<String, Boolean> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.get("isRunning")).isTrue();
        verify(autoFetchSchedulerService).isSchedulerRunning();
    }
}
