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

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.lin.platform.api.ApiResponse;
import ch.lin.youtube.hub.backend.api.app.service.YoutubeHubService;
import ch.lin.youtube.hub.backend.api.dto.DownloadItemsRequest;
import ch.lin.youtube.hub.backend.api.dto.MarkAllDoneRequest;
import ch.lin.youtube.hub.backend.api.dto.ProcessRequest;
import ch.lin.youtube.hub.backend.api.dto.VerifyItemsRequest;
import ch.lin.youtube.hub.backend.api.dto.VerifyItemsResponse;
import jakarta.validation.Valid;

/**
 * REST controller for orchestrating the main YouTube processing job.
 * <p>
 * This controller provides endpoints to trigger the primary data fetching and
 * processing tasks, as well as utility functions for managing the state of all
 * items. It delegates the business logic to the {@link YoutubeHubService}.
 */
@RestController
@RequestMapping("/tasks")
public class YoutubeHubController {

    private final YoutubeHubService youtubeHubService;

    public YoutubeHubController(YoutubeHubService youtubeHubService) {
        this.youtubeHubService = youtubeHubService;
    }

    /**
     * Triggers the main processing job, which fetches new videos from all
     * subscribed channels and saves them to the database.
     *
     * @param request The request body containing the YouTube Data API key,
     * optional configuration name, and processing parameters like delay and a
     * `publishedAfter` timestamp.
     * @return A {@link ResponseEntity} with an {@link ApiResponse} containing a
     * map of the job results (e.g., number of new items, failures) and an HTTP
     * 200 OK status.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X POST http://localhost:8080/tasks/fetch \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "apiKey": "your-youtube-api-key",
     *   "configName": "optional-config-name",
     *   "delayInMilliseconds": 200,
     *   "publishedAfter": "2023-10-27T10:00:00Z"
     * }'
     * }
     * </pre>
     */
    @PostMapping(value = "/fetch", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> processJob(
            @Valid @RequestBody final ProcessRequest request) {
        Map<String, Object> result = youtubeHubService.processJob(request.getApiKey(), request.getConfigName(),
                request.getDelayInMilliseconds(), request.getPublishedAfter(), request.getForcePublishedAfter(),
                request.getChannelIds());
        ApiResponse<Map<String, Object>> response = ApiResponse.success(result);
        return ResponseEntity.ok(response);
    }

    /**
     * Marks all unprocessed video items as "manually downloaded". This is a
     * utility endpoint to bulk-update the state of items.
     *
     * @param request An optional request body to scope the operation to
     * specific channel IDs.
     * @return A {@link ResponseEntity} with an {@link ApiResponse} containing
     * the count of updated items and an HTTP 200 OK status.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code curl -X PATCH http://localhost:8080/tasks/mark-all-manually-downloaded}
     * </pre>
     */
    @PatchMapping(value = "/mark-all-manually-downloaded", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllDone(
            @Valid @RequestBody(required = false) final MarkAllDoneRequest request) {
        List<String> channelIds = null;
        if (request != null) {
            channelIds = request.getChannelIds();
        }
        int updatedCount = youtubeHubService.markAllManuallyDownloaded(channelIds);
        Map<String, Integer> detail = Map.of("updatedItems", updatedCount);
        ApiResponse<Map<String, Integer>> response = ApiResponse.success(detail);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks a list of YouTube video URLs against the database and returns the
     * URLs that do not exist as items.
     * <p>
     * This endpoint helps identify which videos from a given list are new to
     * the system and which ones already exist but have not been downloaded.
     *
     * @param request The request body containing a list of URLs to check.
     * @return A {@link ResponseEntity} containing a {@link VerifyItemsResponse}
     * which separates the provided URLs into "new" and "undownloaded" lists.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X POST http://localhost:8080/tasks/verification \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "urls": [
     *     "https://www.youtube.com/watch?v=cWvaZYXzzA8",
     *     "https://www.youtube.com/watch?v=nonExistentId"
     *   ]
     * }'
     * }
     * </pre>
     */
    @PostMapping(value = "/verification", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerifyItemsResponse> verifyItemsExistence(
            @Valid @RequestBody final VerifyItemsRequest request) {
        Map<String, List<String>> verificationResult = youtubeHubService.verifyNewItems(request.getUrls());
        VerifyItemsResponse response = new VerifyItemsResponse(verificationResult.get("new"),
                verificationResult.get("undownloaded"));
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes all data processed by the YouTube Hub, including channels and
     * items.
     *
     * @return A {@link ResponseEntity} with an HTTP 204 No Content status upon
     * successful cleanup.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X DELETE http://localhost:8080/tasks/deletion
     * }
     * </pre>
     */
    @DeleteMapping("/deletion")
    public ResponseEntity<Void> cleanUp() {
        youtubeHubService.cleanup();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Triggers the download of a specific list of video items.
     * <p>
     * This endpoint receives a list of video IDs, retrieves their full details
     * from the database, and then calls the downloader service to perform the
     * actual download.
     *
     * @param request The request body containing the list of video IDs and an
     * optional download configuration name.
     * @param authorizationHeader An optional authorization token, which will be
     * forwarded to the downloader service.
     * @return A {@link ResponseEntity} with an {@link ApiResponse} containing
     * the list of download results from the downloader service.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X POST http://localhost:8080/tasks/download \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "configName": "default",
     *   "videoIds": ["dQw4w9WgXcQ", "C0DPdy98e4c"]
     * }'
     * }
     * </pre>
     */
    @PostMapping(value = "/download", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> downloadItems(
            @Valid @RequestBody final DownloadItemsRequest request,
            @RequestHeader(value = "Authorization", required = false) final String authorizationHeader) {
        Map<String, Object> result = youtubeHubService.downloadItems(request.getVideoIds(),
                request.getConfigName(), authorizationHeader);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
