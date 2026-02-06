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

import java.time.OffsetDateTime;
import java.util.List;
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

import ch.lin.platform.api.ApiResponse;
import ch.lin.youtube.hub.backend.api.app.service.YoutubeHubService;
import ch.lin.youtube.hub.backend.api.dto.DownloadItemsRequest;
import ch.lin.youtube.hub.backend.api.dto.MarkAllDoneRequest;
import ch.lin.youtube.hub.backend.api.dto.ProcessRequest;
import ch.lin.youtube.hub.backend.api.dto.VerifyItemsRequest;
import ch.lin.youtube.hub.backend.api.dto.VerifyItemsResponse;

@ExtendWith(MockitoExtension.class)
class YoutubeHubControllerTest {

    @Mock
    private YoutubeHubService youtubeHubService;

    private YoutubeHubController youtubeHubController;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        youtubeHubController = new YoutubeHubController(youtubeHubService);
    }

    @Test
    void processJob_ShouldCallServiceAndReturnResult() {
        ProcessRequest request = new ProcessRequest();
        request.setApiKey("key");
        request.setConfigName("config");
        request.setDelayInMilliseconds(100L);
        OffsetDateTime now = OffsetDateTime.now();
        request.setPublishedAfter(now);
        request.setForcePublishedAfter(true);
        request.setChannelIds(List.of("ch1"));

        Map<String, Object> serviceResult = Map.of("newItems", 5);
        when(youtubeHubService.processJob("key", "config", 100L, now, true, List.of("ch1")))
                .thenReturn(serviceResult);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = youtubeHubController.processJob(request);
        ApiResponse<Map<String, Object>> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getData()).isEqualTo(serviceResult);
    }

    @Test
    void markAllDone_ShouldCallServiceAndReturnCount() {
        MarkAllDoneRequest request = new MarkAllDoneRequest();
        request.setChannelIds(List.of("ch1"));

        when(youtubeHubService.markAllManuallyDownloaded(List.of("ch1"))).thenReturn(10);

        ResponseEntity<ApiResponse<Map<String, Integer>>> response = youtubeHubController.markAllDone(request);
        ApiResponse<Map<String, Integer>> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getData().get("updatedItems")).isEqualTo(10);
    }

    @Test
    void markAllDone_ShouldCallServiceWithNull_WhenRequestIsNull() {
        when(youtubeHubService.markAllManuallyDownloaded(null)).thenReturn(5);

        ResponseEntity<ApiResponse<Map<String, Integer>>> response = youtubeHubController.markAllDone(null);
        ApiResponse<Map<String, Integer>> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getData().get("updatedItems")).isEqualTo(5);
    }

    @Test
    void verifyItemsExistence_ShouldCallServiceAndReturnResult() {
        VerifyItemsRequest request = new VerifyItemsRequest();
        request.setUrls(List.of("url1", "url2"));

        Map<String, List<String>> serviceResult = Map.of(
                "new", List.of("url1"),
                "undownloaded", List.of("url2")
        );
        when(youtubeHubService.verifyNewItems(List.of("url1", "url2"))).thenReturn(serviceResult);

        ResponseEntity<VerifyItemsResponse> response = youtubeHubController.verifyItemsExistence(request);
        VerifyItemsResponse body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(body.newUrls()).containsExactly("url1");
        assertThat(body.undownloadedUrls()).containsExactly("url2");
    }

    @Test
    void cleanUp_ShouldCallService() {
        ResponseEntity<Void> response = youtubeHubController.cleanUp();

        verify(youtubeHubService).cleanup();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void downloadItems_ShouldCallServiceAndReturnResult() {
        DownloadItemsRequest request = new DownloadItemsRequest();
        request.setVideoIds(List.of("vid1"));
        request.setConfigName("default");
        String authHeader = "Bearer token";

        Map<String, Object> serviceResult = Map.of("createdTasks", 1);
        when(youtubeHubService.downloadItems(List.of("vid1"), "default", authHeader))
                .thenReturn(serviceResult);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = youtubeHubController.downloadItems(request, authHeader);
        ApiResponse<Map<String, Object>> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getData()).isEqualTo(serviceResult);
    }
}
