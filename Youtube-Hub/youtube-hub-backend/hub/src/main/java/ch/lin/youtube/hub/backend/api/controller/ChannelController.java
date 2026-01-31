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
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.lin.youtube.hub.backend.api.app.service.ChannelService;
import ch.lin.youtube.hub.backend.api.app.service.model.AddChannelsResult;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.dto.AddChannelsByUrlRequest;
import ch.lin.youtube.hub.backend.api.dto.AddChannelsResponse;
import ch.lin.youtube.hub.backend.api.dto.ChannelResponse;
import jakarta.validation.Valid;

/**
 * REST controller for managing YouTube channels.
 * <p>
 * This controller provides API endpoints for creating, retrieving, and deleting
 * YouTube channel data within the application. It delegates business logic to
 * the {@link ChannelService}.
 */
@RestController
@RequestMapping("/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    /**
     * Retrieves a list of all channels currently stored in the database.
     *
     * @return A {@link ResponseEntity} containing a list of
     * {@link ChannelResponse} objects and an HTTP 200 OK status.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChannelResponse>> getAllChannels() {
        List<Channel> channels = channelService.getAllChannels();
        List<ChannelResponse> response = channels.stream()
                .map(ChannelResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Adds new channels by parsing their handles from a list of YouTube URLs.
     * It fetches channel details from the YouTube API and persists them.
     *
     * @param request The request body containing the API key and a list of
     * channel URLs.
     * @return A {@link ResponseEntity} with an HTTP 200 OK status. The body
     * contains an {@link AddChannelsResponse} object detailing which channels
     * were successfully added and which URLs failed processing.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X POST http://localhost:8080/channels \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "apiKey": "your-youtube-api-key",
     *   "urls": [
     *     "https://www.youtube.com/@GoogleDevelopers",
     *     "https://www.youtube.com/@SpringSourceDev"
     *   ]
     * }'
     * }
     * </pre>
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddChannelsResponse> addChannelsByUrl(
            @Valid @RequestBody final AddChannelsByUrlRequest request) {
        AddChannelsResult result = channelService.addChannelsByUrl(request.getApiKey(), request.getConfigName(), request.getUrls());

        List<ChannelResponse> addedChannels = result.getAddedChannels().stream()
                .map(ChannelResponse::new)
                .collect(Collectors.toList());

        List<AddChannelsResponse.FailedUrlResponse> failedUrls = result.getFailedUrls().stream()
                .map(failed -> new AddChannelsResponse.FailedUrlResponse(failed.getUrl(), failed.getReason()))
                .collect(Collectors.toList());

        AddChannelsResponse response = new AddChannelsResponse(addedChannels, failedUrls);

        // Return 200 OK with the list of created/updated channels.
        return ResponseEntity.ok(response);
    }

    /**
     * Fetches detailed information for a specific channel directly from the
     * YouTube Data API. This endpoint acts as a proxy to the YouTube API.
     *
     * @param channelId The unique ID of the YouTube channel.
     * @param apiKey An optional YouTube Data API key. If not provided, the
     * system will use a configured default key.
     * @param configName An optional configuration name to use for resolving the
     * API key.
     * @return A {@link ResponseEntity} containing the raw JSON string response
     * from the YouTube API and an HTTP 200 OK status.
     */
    @GetMapping(value = "/{channelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getChannelDetails(@PathVariable final String channelId,
            @RequestParam(name = "apiKey", required = false) final String apiKey,
            @RequestParam(name = "configName", required = false) final String configName) {
        String channelDetailsJson = channelService.getChannelDetailsFromApi(channelId, apiKey, configName);
        return ResponseEntity.ok(channelDetailsJson);
    }

    /**
     * Deletes a specific channel from the database.
     *
     * @param channelId The unique ID of the channel to be deleted.
     * @return A {@link ResponseEntity} with an HTTP 204 No Content status. An
     * error status (e.g., 404 Not Found) will be returned if the channel does
     * not exist.
     */
    @DeleteMapping(value = "/{channelId}")
    public ResponseEntity<Void> deleteChannel(@PathVariable final String channelId) {
        channelService.deleteChannel(channelId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
