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
package ch.lin.youtube.hub.backend.api.app.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.lin.platform.http.HttpClient;
import ch.lin.platform.http.exception.HttpException;
import ch.lin.youtube.hub.backend.api.app.repository.ChannelRepository;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.PlaylistRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.PlaylistProcessingResult;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.Playlist;

/**
 * Implementation of {@link ChannelProcessingService}.
 * <p>
 * This service handles the logic for processing individual YouTube channels.
 */
@Service
public class ChannelProcessingServiceImpl implements ChannelProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelProcessingServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ChannelRepository channelRepository;
    private final PlaylistRepository playlistRepository;
    private final ItemRepository itemRepository;
    private final YoutubeApiUsageService youtubeApiUsageService;
    private final VideoFetchService videoFetchService;
    private final YoutubeCheckpointService youtubeCheckpointService;

    /**
     * Constructs a new ChannelProcessingServiceImpl.
     *
     * @param channelRepository the repository for channel data
     * @param playlistRepository the repository for playlist data
     * @param itemRepository the repository for item data
     * @param youtubeApiUsageService the service for tracking API usage
     * @param videoFetchService the service for fetching video details
     * @param youtubeCheckpointService the service for saving checkpoints
     */
    public ChannelProcessingServiceImpl(ChannelRepository channelRepository, PlaylistRepository playlistRepository,
            ItemRepository itemRepository, YoutubeApiUsageService youtubeApiUsageService,
            VideoFetchService videoFetchService, YoutubeCheckpointService youtubeCheckpointService) {
        this.channelRepository = channelRepository;
        this.playlistRepository = playlistRepository;
        this.itemRepository = itemRepository;
        this.youtubeApiUsageService = youtubeApiUsageService;
        this.videoFetchService = videoFetchService;
        this.youtubeCheckpointService = youtubeCheckpointService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Playlist prepareChannelAndPlaylist(Channel channel, HttpClient client, String apiKey,
            long delayInMilliseconds, long quotaLimit, long quotaThreshold) {

        logger.info("Processing channel: {} ({})", channel.getTitle(), channel.getChannelId());

        // 1. Fetch channel details (Uploads Playlist ID & Title)
        Map<String, String> channelDetails = fetchChannelDetailsFromApi(client, channel.getChannelId(), apiKey,
                delayInMilliseconds, quotaLimit, quotaThreshold);

        String uploadsPlaylistId = channelDetails.get("uploadsPlaylistId");
        String latestTitle = channelDetails.get("title");
        Objects.requireNonNull(latestTitle);

        // 2. Update channel info if changed
        if (!latestTitle.isBlank() && !channel.getTitle().equals(latestTitle)) {
            logger.info("  -> Channel title has changed from '{}' to '{}'. Updating.", channel.getTitle(), latestTitle);
            channel.setTitle(latestTitle);
            channelRepository.save(channel);
        }

        // 3. Get or Create Playlist
        return playlistRepository.findByPlaylistId(uploadsPlaylistId)
                .orElseGet(() -> {
                    logger.info("  -> Uploads playlist with ID {} not found in DB. Creating it.", uploadsPlaylistId);
                    Playlist newPlaylist = new Playlist();
                    newPlaylist.setPlaylistId(uploadsPlaylistId);
                    newPlaylist.setTitle("Uploads from " + channel.getTitle());
                    newPlaylist.setChannel(channel);
                    return playlistRepository.save(newPlaylist);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PlaylistProcessingResult processPlaylistItems(Playlist playlist, HttpClient client, String apiKey,
            OffsetDateTime publishedAfter, boolean forcePublishedAfter,
            long delayInMilliseconds, long quotaLimit, long quotaThreshold) {
        logger.info("  -> Processing playlist: {} ({})", playlist.getTitle(), playlist.getPlaylistId());

        // 4. Fetch items
        return fetchAndProcessPlaylistItems(client, playlist, apiKey,
                publishedAfter, forcePublishedAfter, delayInMilliseconds, quotaLimit, quotaThreshold);
    }

    /**
     * Fetches channel details (uploads playlist ID and title) from the YouTube
     * Data API.
     *
     * @param client The reusable HttpClient for making the request.
     * @param channelId the ID of the YouTube channel
     * @param apiKey the YouTube Data API key
     * @param delayInMilliseconds the delay in milliseconds before making the
     * API request
     * @param quotaLimit the daily quota limit
     * @param quotaThreshold the safety threshold for quota
     * @return a map containing "uploadsPlaylistId" and "title"
     * @throws QuotaExceededException if the daily quota limit is reached.
     * @throws YoutubeApiAuthException if the API key is invalid.
     * @throws YoutubeApiRequestException if the API call fails or the response
     * cannot be parsed.
     */
    private Map<String, String> fetchChannelDetailsFromApi(HttpClient client, String channelId, String apiKey,
            long delayInMilliseconds, long quotaLimit, long quotaThreshold) {
        try {
            if (!youtubeApiUsageService.hasSufficientQuota(quotaLimit, quotaThreshold)) {
                throw new QuotaExceededException("Daily quota limit reached.");
            }
            Map<String, String> params = new HashMap<>();
            params.put("part", "contentDetails,snippet");
            params.put("id", channelId);
            params.put("key", apiKey);

            delayRequest(delayInMilliseconds);
            // Quota cost: 1 unit for channels.list operation
            youtubeApiUsageService.recordUsage(1L);
            String responseBody = client.get("/youtube/v3/channels", params, null).body();

            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode itemNode = root.path("items").path(0);

            if (!itemNode.isMissingNode()) {
                String uploadsId = itemNode.path("contentDetails").path("relatedPlaylists").path("uploads")
                        .asText(null);
                String title = itemNode.path("snippet").path("title").asText(null);

                if (uploadsId != null && title != null) {
                    Map<String, String> details = new HashMap<>();
                    details.put("uploadsPlaylistId", uploadsId);
                    details.put("title", title);
                    return details;
                }
            }
            throw new YoutubeApiRequestException(
                    "Could not parse 'uploads' or 'title' from YouTube API response for channelId " + channelId);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 400
                    && e.getMessage().contains("API key not valid")) {
                throw new YoutubeApiAuthException("The provided YouTube API key is not valid.", e);
            }
            logger.error("Failed to fetch channel details from YouTube API for channelId {}: {}", channelId,
                    e.getMessage(), e);
            throw new YoutubeApiRequestException(
                    "Failed to fetch channel details from YouTube API for channelId " + channelId, e);
        }
    }

    /**
     * Fetches new video items from a playlist since the last processing time
     * and stores them in the database. It handles API pagination and stops
     * fetching when it encounters a video that is older than the playlist's
     * last `processedAt` timestamp.
     *
     * @param client The reusable HttpClient for making the request.
     * @param playlist the playlist to process
     * @param apiKey the YouTube Data API key
     * @param requestPublishedAfter optional datetime to filter videos
     * @param forcePublishedAfter if true, forces the use of
     * {@code requestPublishedAfter}
     * @param delayInMilliseconds the delay in milliseconds before each API
     * request
     * @param quotaLimit the daily quota limit
     * @param quotaThreshold the safety threshold for quota
     * @return a {@link PlaylistProcessingResult} containing the counts of new
     * and updated items
     * @throws QuotaExceededException if the daily quota limit is reached.
     * @throws YoutubeApiAuthException if the API key is invalid.
     * @throws YoutubeApiRequestException if the API call fails.
     */
    private PlaylistProcessingResult fetchAndProcessPlaylistItems(HttpClient client, Playlist playlist, String apiKey,
            OffsetDateTime requestPublishedAfter,
            boolean forcePublishedAfter,
            long delayInMilliseconds,
            long quotaLimit,
            long quotaThreshold) {
        OffsetDateTime lastProcessedAt = playlist.getProcessedAt();

        // Determine the effective cutoff time for fetching videos.
        OffsetDateTime effectivePublishedAfter = lastProcessedAt;
        if (requestPublishedAfter != null) {
            if (forcePublishedAfter || effectivePublishedAfter == null
                    || requestPublishedAfter.isAfter(effectivePublishedAfter)) {
                effectivePublishedAfter = requestPublishedAfter;
                logger.info("    -> Overriding last processed time with provided publishedAfter: {}",
                        requestPublishedAfter);
            }
        }

        logger.info("    -> Last processed at: {}. Using effective cutoff time: {}",
                lastProcessedAt == null ? "Never" : lastProcessedAt.toString(),
                effectivePublishedAfter == null ? "None" : effectivePublishedAfter.toString());

        OffsetDateTime newestVideoPublishedAt = null;
        String nextPageToken = playlist.getLastPageToken();
        if (nextPageToken != null) {
            logger.info("    -> Resuming from checkpoint token: {}", nextPageToken);
        }

        boolean stopFetching = false;
        PlaylistProcessingResult result = new PlaylistProcessingResult();

        try {
            do {
                if (!youtubeApiUsageService.hasSufficientQuota(quotaLimit, quotaThreshold)) {
                    throw new QuotaExceededException("Daily quota limit reached.");
                }

                Map<String, String> params = new HashMap<>();
                params.put("part", "snippet");
                params.put("playlistId", playlist.getPlaylistId());
                params.put("key", apiKey);
                params.put("maxResults", "50"); // Max allowed value
                if (nextPageToken != null) {
                    params.put("pageToken", nextPageToken);
                }

                delayRequest(delayInMilliseconds);
                // Quota cost: 1 unit for playlistItems.list operation
                youtubeApiUsageService.recordUsage(1L);
                String responseBody = client.get("/youtube/v3/playlistItems", params, null).body();
                JsonNode root = OBJECT_MAPPER.readTree(responseBody);

                nextPageToken = root.path("nextPageToken").asText(null);

                JsonNode itemsNode = root.path("items");
                if (itemsNode.isMissingNode() || !itemsNode.isArray()) {
                    logger.warn("    -> No items found in API response for playlist {}", playlist.getPlaylistId());
                    break;
                }

                // Map of new video IDs on this page to their snippet from the playlistItems
                // API call.
                Map<String, JsonNode> newVideoSnippetsOnPage = new LinkedHashMap<>();
                // List of existing items on this page for potential update.
                List<Item> existingItemsToUpdateOnPage = new ArrayList<>();

                for (JsonNode itemNode : itemsNode) {
                    JsonNode snippet = itemNode.path("snippet");
                    String videoId = snippet.path("resourceId").path("videoId").asText("");
                    if (videoId.isBlank()) {
                        continue; // Skip if it's not a video (e.g., deleted video)
                    }
                    OffsetDateTime videoPublishedAt = OffsetDateTime.parse(snippet.path("publishedAt").asText());
                    // The first video in the first page is the newest one overall for this run.
                    logger.debug("    -> Video ID {}, published at {}.", videoId, videoPublishedAt);
                    if (newestVideoPublishedAt == null) {
                        newestVideoPublishedAt = videoPublishedAt;
                    }
                    // If we have a last processed time, and the current video is not newer,
                    // we can stop. The API returns items in reverse chronological order.
                    if (effectivePublishedAfter != null && !videoPublishedAt.isAfter(effectivePublishedAfter)) {
                        logger.info(
                                "    -> Reached video published at {}, which is not newer than last processed time {}. Stopping.",
                                videoPublishedAt, effectivePublishedAfter);
                        stopFetching = true;
                        break;
                    }
                    Optional<Item> existingItemOpt = itemRepository.findByVideoId(videoId);
                    if (existingItemOpt.isPresent()) {
                        logger.debug("    -> Video with ID {} already exists in DB. Queuing for update check.",
                                videoId);
                        existingItemsToUpdateOnPage.add(existingItemOpt.get());
                    } else {
                        // This is a new video, add it to the map for batch processing.
                        logger.debug("    -> Video with ID {} is new. Queuing for creation.", videoId);
                        newVideoSnippetsOnPage.put(videoId, snippet);
                    }
                }

                // After checking all items on the page, fetch full details for the new ones.
                List<Item> createdItems = Collections.emptyList();
                try {
                    createdItems = videoFetchService.fetchAndCreateItemsFromVideoIds(client, apiKey, newVideoSnippetsOnPage,
                            delayInMilliseconds, quotaLimit, quotaThreshold);

                    try {
                        for (Item newItem : createdItems) {
                            newItem.setPlaylist(playlist);
                            result.setNewItemsCount(result.getNewItemsCount() + 1);
                            switch (newItem.getLiveBroadcastContent()) {
                                case NONE ->
                                    result.setStandardVideoCount(result.getStandardVideoCount() + 1);
                                case UPCOMING ->
                                    result.setUpcomingVideoCount(result.getUpcomingVideoCount() + 1);
                                case LIVE ->
                                    result.setLiveVideoCount(result.getLiveVideoCount() + 1);
                                // No default case needed if all enum values are handled.
                                // If new values are added to LiveBroadcastContent, the compiler will warn you.
                            }
                        }
                    } catch (Exception e) {
                        logger.error(
                                "Failed to save a batch of items. This is often a character set issue (e.g., emoji in title). Please ensure DB uses utf8mb4.",
                                e);
                        // Optionally, you could try saving one-by-one here to isolate the bad item.
                    }

                    // After checking all items on the page, fetch full details for existing ones
                    // to check for updates.
                    int updated = videoFetchService.updateExistingItems(client, apiKey, existingItemsToUpdateOnPage,
                            delayInMilliseconds, quotaLimit, quotaThreshold);
                    result.setUpdatedItemsCount(result.getUpdatedItemsCount() + updated);

                    // [HUB-11] Save checkpoint: commit new items and the token for the NEXT page.
                    // If stopFetching is true, we still save the progress of this page, but we might want to clear the token if we are done.
                    // However, if we stop fetching, we are effectively done with the playlist.
                    youtubeCheckpointService.savePageProgress(playlist, createdItems, stopFetching ? null : nextPageToken);
                } catch (QuotaExceededException e) {
                    if (!createdItems.isEmpty()) {
                        logger.warn("Quota exceeded during playlist processing. Saving {} new items before stopping.", createdItems.size());
                        // Save items but keep the current checkpoint (don't advance) so we retry this page next time.
                        youtubeCheckpointService.savePageProgress(playlist, createdItems, playlist.getLastPageToken());
                    }
                    throw e;
                }

            } while (!stopFetching && nextPageToken != null);

            // After fetching all new items, update the playlist's processedAt timestamp
            // to the publish time of the newest video we found in this run.
            // Only update if we finished the whole playlist (nextPageToken is null or we stopped early because we reached old videos).
            if (nextPageToken == null || stopFetching) {
                if (result.getNewItemsCount() > 0) {
                    logger.info("    -> Found {} new video(s) for playlist {}. Updating playlist's processedAt time to {}.",
                            result.getNewItemsCount(), playlist.getPlaylistId(), newestVideoPublishedAt);
                    playlist.setProcessedAt(newestVideoPublishedAt);
                } else {
                    logger.info("    -> No new videos found for playlist {}.", playlist.getPlaylistId());
                }
                // Ensure token is cleared when finished
                if (playlist.getLastPageToken() != null) {
                    playlist.setLastPageToken(null);
                }
                playlistRepository.save(playlist);
            }
        } catch (IOException | URISyntaxException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 400
                    && e.getMessage().contains("API key not valid")) {
                throw new YoutubeApiAuthException("The provided YouTube API key is not valid.", e);
            }
            logger.error("Failed to fetch and process playlist items from YouTube API for playlistId {}: {}",
                    playlist.getPlaylistId(), e.getMessage(), e);
            throw new YoutubeApiRequestException(
                    "Failed to fetch and process playlist items from YouTube API for playlistId "
                    + playlist.getPlaylistId(),
                    e);
        }
        return result;
    }

    /**
     * Pauses execution for a specified duration.
     *
     * @param milliseconds The number of milliseconds to wait.
     * @throws InterruptedException if the thread is interrupted while sleeping.
     */
    private void delayRequest(long milliseconds) throws InterruptedException {
        if (milliseconds > 0) {
            logger.debug("Waiting for {} millisecond(s) before next API request.", milliseconds);
            Thread.sleep(milliseconds);
        } else {
            logger.debug("No delay configured, proceeding with request immediately.");
        }
    }
}
