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
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.platform.http.HttpClient;
import ch.lin.platform.http.Scheme;
import ch.lin.platform.http.exception.HttpException;
import ch.lin.youtube.hub.backend.api.app.repository.ChannelRepository;
import ch.lin.youtube.hub.backend.api.app.repository.DownloadInfoRepository;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.PlaylistRepository;
import ch.lin.youtube.hub.backend.api.app.repository.TagRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.DownloadItem;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.domain.model.DownloadInfo;
import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.LiveBroadcastContent;
import ch.lin.youtube.hub.backend.api.domain.model.Playlist;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Main orchestration service for the YouTube Hub application.
 * <p>
 * This class implements the {@link YoutubeHubService} interface and is
 * responsible for coordinating the core business logic. It orchestrates the
 * process of fetching data from the YouTube Data API, processing channels,
 * discovering new video items, and persisting them to the database. It utilizes
 * other repositories and services to perform its tasks.
 */
@Service
public class YoutubeHubServiceImpl implements YoutubeHubService {

    private static final Logger logger = LoggerFactory.getLogger(YoutubeHubServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final ChannelRepository channelRepository;
    private final ItemRepository itemRepository;
    private final PlaylistRepository playlistRepository;
    private final TagRepository tagRepository;
    private final DownloadInfoRepository downloadInfoRepository;
    private final ConfigsService configsService;
    private final YoutubeApiUsageService youtubeApiUsageService;
    private final YoutubeCheckpointService youtubeCheckpointService;
    @Value("${youtube.hub.downloader.url}")
    private String downloaderServiceUrl;

    /**
     * A private static class to hold the results of processing a playlist. This
     * encapsulates the counts of different types of new video items found.
     */
    private static class PlaylistProcessingResult {

        int newItemsCount = 0;
        int standardVideoCount = 0;
        int upcomingVideoCount = 0;
        int liveVideoCount = 0;
        int updatedItemsCount = 0;
    }

    /**
     * Constructs a new YoutubeHubServiceImpl with the required dependencies.
     *
     * @param channelRepository the repository for channel data access
     * @param itemRepository the repository for item data access
     * @param playlistRepository the repository for playlist data access
     * @param tagRepository the repository for tag data access
     * @param downloadInfoRepository the repository for download info data
     * access
     * @param configsService the service for accessing application configuration
     */
    public YoutubeHubServiceImpl(ChannelRepository channelRepository, ItemRepository itemRepository,
            PlaylistRepository playlistRepository, TagRepository tagRepository,
            DownloadInfoRepository downloadInfoRepository, ConfigsService configsService,
            YoutubeApiUsageService youtubeApiUsageService,
            YoutubeCheckpointService youtubeCheckpointService) {
        this.channelRepository = channelRepository;
        this.itemRepository = itemRepository;
        this.playlistRepository = playlistRepository;
        this.tagRepository = tagRepository;
        this.downloadInfoRepository = downloadInfoRepository;
        this.configsService = configsService;
        this.youtubeApiUsageService = youtubeApiUsageService;
        this.youtubeCheckpointService = youtubeCheckpointService;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs a destructive cleanup by deleting all
     * records from the channel, item, playlist, and tag tables, and then resets
     * their primary key sequences.
     */
    @Override
    @Transactional
    public void cleanup() {
        downloadInfoRepository.cleanTable(); // Depends on Item
        itemRepository.cleanTable(); // Depends on Playlist and Tag
        playlistRepository.cleanTable(); // Depends on Channel
        channelRepository.cleanTable(); // No outgoing dependencies
        tagRepository.cleanTable(); // No outgoing dependencies, but Items depend on it.
        downloadInfoRepository.resetSequence();
        itemRepository.resetSequence();
        playlistRepository.resetSequence();
        channelRepository.resetSequence();
        tagRepository.resetSequence();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> processJob(String key, String configName, Long delayInMilliseconds, OffsetDateTime publishedAfter,
            boolean forcePublishedAfter, List<String> channelIds) {
        HubConfig resolvedConfig;
        if (configName != null && !configName.isBlank()) {
            resolvedConfig = configsService.getResolvedConfig(configName);
            if (!configName.equalsIgnoreCase(resolvedConfig.getName())) {
                throw new InvalidRequestException("Configuration with name '" + configName + "' not found.");
            }
        } else {
            resolvedConfig = configsService.getResolvedConfig(null);
        }

        String apiKey = key;
        if (apiKey == null || apiKey.isBlank()) {
            logger.info("No API key provided in request. Using configured key from '{}'.", resolvedConfig.getName());
            apiKey = resolvedConfig.getYoutubeApiKey();
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new InvalidRequestException("A YouTube API key is required. None was provided in the request, and no default key is configured.");
        }

        if (delayInMilliseconds == null || delayInMilliseconds < 0) {
            logger.warn("Invalid delay value provided. Defaulting to 100 milliseconds.");
            delayInMilliseconds = 100L;
        }

        long quotaLimit = resolvedConfig.getQuota();
        long quotaThreshold = resolvedConfig.getQuotaSafetyThreshold();

        List<Channel> channels;
        if (channelIds != null && !channelIds.isEmpty()) {
            channels = channelRepository.findAllByChannelIdIn(channelIds);
        } else {
            channels = channelRepository.findAll();
        }
        logger.info("Starting YouTube processing job.");
        logger.info("Found {} channels to process.", channels.size());

        int processedChannelsCount = 0;
        int newItemsCount = 0;
        int standardVideoCount = 0;
        int upcomingVideoCount = 0;
        int liveVideoCount = 0;
        int updatedItemsCount = 0;
        List<Map<String, String>> failures = new ArrayList<>();

        // Create a single HttpClient to be reused for all API calls in this job.
        try (HttpClient client = new HttpClient(Scheme.HTTPS, "youtube.googleapis.com", 443)) {
            for (Channel channel : channels) {
                try {
                    logger.info("Processing channel: {} ({})", channel.getTitle(), channel.getChannelId());

                    // Fetch the latest channel details from the YouTube Data API.
                    Map<String, String> channelDetails = fetchChannelDetailsFromApi(client, channel.getChannelId(),
                            apiKey,
                            delayInMilliseconds, quotaLimit, quotaThreshold);
                    String uploadsPlaylistId = channelDetails.get("uploadsPlaylistId");
                    String latestTitle = channelDetails.get("title");
                    Objects.requireNonNull(latestTitle);
                    // Update channel title if it has changed on YouTube.
                    if (!latestTitle.isBlank() && !channel.getTitle().equals(latestTitle)) {
                        logger.info("  -> Channel title has changed from '{}' to '{}'. Updating.",
                                channel.getTitle(),
                                latestTitle);
                        channel.setTitle(latestTitle);
                        channelRepository.save(channel);
                    }
                    Playlist uploadsPlaylist = playlistRepository.findByPlaylistId(uploadsPlaylistId)
                            .orElseGet(() -> {
                                logger.info("  -> Uploads playlist with ID {} not found in DB. Creating it.",
                                        uploadsPlaylistId);
                                Playlist newPlaylist = new Playlist();
                                newPlaylist.setPlaylistId(uploadsPlaylistId);
                                newPlaylist.setTitle("Uploads from " + channel.getTitle());
                                newPlaylist.setChannel(channel);
                                return playlistRepository.save(newPlaylist);
                            });

                    logger.info("  -> Processing playlist: {} ({})", uploadsPlaylist.getTitle(),
                            uploadsPlaylist.getPlaylistId());
                    PlaylistProcessingResult channelResult = fetchAndProcessPlaylistItems(client, uploadsPlaylist, apiKey,
                            publishedAfter, forcePublishedAfter, delayInMilliseconds, quotaLimit, quotaThreshold);
                    newItemsCount += channelResult.newItemsCount;
                    standardVideoCount += channelResult.standardVideoCount;
                    upcomingVideoCount += channelResult.upcomingVideoCount;
                    liveVideoCount += channelResult.liveVideoCount;
                    updatedItemsCount += channelResult.updatedItemsCount;
                    processedChannelsCount++;
                } catch (QuotaExceededException e) {
                    logger.warn("Job stopped early: Global quota limit reached while processing {}.", channel.getTitle());
                    break;
                } catch (YoutubeApiRequestException e) {
                    logger.error("Failed to process channel {}: {}", channel.getChannelId(), e.getMessage(), e);
                    Map<String, String> failure = new HashMap<>();
                    failure.put("channelId", channel.getChannelId());
                    failure.put("channelTitle", channel.getTitle());
                    failure.put("reason", e.getMessage());
                    failures.add(failure);
                }
            }
        } catch (IOException e) {
            // This catches potential exceptions from closing the HttpClient.
            throw new YoutubeApiRequestException("An I/O error occurred with the YouTube API client.", e);
        }
        logger.info("Finished YouTube processing job. Processed {} channels and found {} new items.",
                processedChannelsCount, newItemsCount);
        Map<String, Object> result = new HashMap<>();
        result.put("processedChannels", processedChannelsCount);
        result.put("newItems", newItemsCount);
        result.put("standardVideoCount", standardVideoCount);
        result.put("upcomingVideoCount", upcomingVideoCount);
        result.put("liveVideoCount", liveVideoCount);
        result.put("updatedItemsCount", updatedItemsCount);
        result.put("failures", failures);
        return result;
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
                    createdItems = fetchAndCreateItemsFromVideoIds(client, apiKey, newVideoSnippetsOnPage,
                            delayInMilliseconds, quotaLimit, quotaThreshold);

                    try {
                        for (Item newItem : createdItems) {
                            newItem.setPlaylist(playlist);
                            result.newItemsCount++;
                            switch (newItem.getLiveBroadcastContent()) {
                                case NONE ->
                                    result.standardVideoCount++;
                                case UPCOMING ->
                                    result.upcomingVideoCount++;
                                case LIVE ->
                                    result.liveVideoCount++;
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
                    result.updatedItemsCount += updateExistingItems(client, apiKey, existingItemsToUpdateOnPage,
                            delayInMilliseconds, quotaLimit, quotaThreshold);

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
                if (result.newItemsCount > 0) {
                    logger.info("    -> Found {} new video(s) for playlist {}. Updating playlist's processedAt time to {}.",
                            result.newItemsCount, playlist.getPlaylistId(), newestVideoPublishedAt);
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
     * Fetches detailed information for a batch of video IDs from the YouTube
     * Data API and creates Item entities.
     *
     * @param client The reusable HttpClient for making the request.
     * @param apiKey the YouTube Data API key
     * @param newVideoSnippets a map of new video IDs to their corresponding
     * snippet JsonNode from the playlistItems call. This provides the original
     * `publishedAt` timestamp.
     * @param delayInMilliseconds the delay in milliseconds before making the
     * API request
     * @param quotaLimit the daily quota limit
     * @param quotaThreshold the safety threshold for quota
     * @return a list of newly created (but not yet persisted) Item entities
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws QuotaExceededException if the daily quota limit is reached.
     * @throws YoutubeApiAuthException if the API key is invalid.
     * @throws YoutubeApiRequestException if the API call fails.
     */
    private List<Item> fetchAndCreateItemsFromVideoIds(HttpClient client, String apiKey,
            Map<String, JsonNode> newVideoSnippets, long delayInMilliseconds, long quotaLimit, long quotaThreshold) throws InterruptedException {
        if (newVideoSnippets.isEmpty()) {
            return Collections.emptyList();
        }

        List<Item> newItems = new ArrayList<>();
        String videoIds = String.join(",", newVideoSnippets.keySet());

        Map<String, String> params = new HashMap<>();
        params.put("part", "snippet,liveStreamingDetails");
        params.put("id", videoIds);
        params.put("key", apiKey);
        params.put("maxResults", "50");

        try {
            if (!youtubeApiUsageService.hasSufficientQuota(quotaLimit, quotaThreshold)) {
                throw new QuotaExceededException("Daily quota limit reached.");
            }
            delayRequest(delayInMilliseconds);
            youtubeApiUsageService.recordUsage(1L);
            logger.debug("    -> Requested video IDs {}.", videoIds);
            // Quota cost: 1 unit for videos.list operation
            String responseBody = client.get("/youtube/v3/videos", params, null).body();
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode videoItemsNode = root.path("items");

            if (videoItemsNode.isMissingNode() || !videoItemsNode.isArray()) {
                logger.warn("    -> No detailed video items found in API response for video IDs: {}", videoIds);
                return Collections.emptyList();
            }

            for (JsonNode videoItemNode : videoItemsNode) {
                String videoId = videoItemNode.path("id").asText();
                JsonNode originalSnippet = newVideoSnippets.get(videoId);
                if (originalSnippet == null) {
                    continue; // Should not happen if logic is correct
                }

                Item newItem = new Item();
                JsonNode videoSnippet = videoItemNode.path("snippet");
                newItem.setVideoId(videoId);
                newItem.setTitle(videoSnippet.path("title").asText());
                newItem.setDescription(videoSnippet.path("description").asText(null));
                newItem.setKind(videoItemNode.path("kind").asText());
                newItem.setVideoPublishedAt(OffsetDateTime.parse(originalSnippet.path("publishedAt").asText()));
                String liveBroadcastContentStr = videoSnippet.path("liveBroadcastContent").asText("NONE")
                        .toUpperCase();
                newItem.setLiveBroadcastContent(LiveBroadcastContent.valueOf(liveBroadcastContentStr));
                String thumbnailUrl = getBestAvailableThumbnailUrl(videoSnippet.path("thumbnails"));
                newItem.setThumbnailUrl(thumbnailUrl);
                JsonNode liveStreamingDetails = videoItemNode.path("liveStreamingDetails");
                if (!liveStreamingDetails.isMissingNode() && liveStreamingDetails.has("scheduledStartTime")) {
                    String scheduledTimeStr = liveStreamingDetails.path("scheduledStartTime").asText(null);
                    Objects.requireNonNull(scheduledTimeStr);
                    newItem.setScheduledStartTime(OffsetDateTime.parse(scheduledTimeStr));
                }
                logger.info("    -> Creating new video: '{}' ({}) published at {} with status {}",
                        newItem.getTitle(), videoId, newItem.getVideoPublishedAt(), newItem.getLiveBroadcastContent());
                newItems.add(newItem);
            }
        } catch (IOException | URISyntaxException e) {
            if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 400
                    && e.getMessage().contains("API key not valid")) {
                throw new YoutubeApiAuthException("The provided YouTube API key is not valid.", e);
            }
            throw new YoutubeApiRequestException(
                    "Failed to fetch video details from YouTube API for video IDs: " + videoIds, e);
        }
        return newItems;
    }

    /**
     * Fetches detailed information for a batch of existing video IDs from the
     * YouTube Data API and updates the corresponding Item entities if there are
     * changes.
     *
     * @param client The reusable HttpClient for making the request.
     * @param apiKey the YouTube Data API key
     * @param existingItemsToUpdate a map of existing Item entities to their
     * corresponding snippet JsonNode from the playlistItems call
     * @param delayInMilliseconds the delay in milliseconds before making the
     * API request
     * @param quotaLimit the daily quota limit
     * @param quotaThreshold the safety threshold for quota
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws QuotaExceededException if the daily quota limit is reached.
     * @throws YoutubeApiAuthException if the API key is invalid.
     * @throws YoutubeApiRequestException if the API call fails.
     * @return the number of items that were updated
     */
    private int updateExistingItems(HttpClient client, String apiKey, List<Item> existingItemsToUpdate,
            long delayInMilliseconds, long quotaLimit, long quotaThreshold) throws InterruptedException {
        if (existingItemsToUpdate.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        List<String> videoIdsList = existingItemsToUpdate.stream().map(Item::getVideoId).toList();
        String videoIds = String.join(",", videoIdsList);

        Map<String, String> params = new HashMap<>();
        params.put("part", "snippet,liveStreamingDetails");
        params.put("id", videoIds);
        params.put("key", apiKey);
        params.put("maxResults", "50");

        try {
            if (!youtubeApiUsageService.hasSufficientQuota(quotaLimit, quotaThreshold)) {
                throw new QuotaExceededException("Daily quota limit reached.");
            }
            delayRequest(delayInMilliseconds);
            // Quota cost: 1 unit for videos.list operation
            youtubeApiUsageService.recordUsage(1L);
            logger.debug("    -> Checking for updates for video IDs {}.", videoIds);
            String responseBody = client.get("/youtube/v3/videos", params, null).body();
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode videoItemsNode = root.path("items");

            if (videoItemsNode.isMissingNode() || !videoItemsNode.isArray()) {
                logger.warn("    -> No detailed video items found for update check in API response for video IDs: {}",
                        videoIds);
                return 0;
            }

            for (JsonNode videoItemNode : videoItemsNode) {
                String videoId = videoItemNode.path("id").asText();
                Optional<Item> itemOptional = existingItemsToUpdate.stream()
                        .filter(item -> item.getVideoId().equals(videoId)).findFirst();

                if (itemOptional.isEmpty()) {
                    continue;
                }

                Item existingItem = itemOptional.get();
                boolean updated = false;

                JsonNode videoSnippet = videoItemNode.path("snippet");
                String newTitle = videoSnippet.path("title").asText();
                if (!existingItem.getTitle().equals(newTitle)) {
                    logger.info("    -> Updating title for video {}: '{}' -> '{}'", videoId, existingItem.getTitle(),
                            newTitle);
                    existingItem.setTitle(newTitle);
                    updated = true;
                }

                String newDescription = videoSnippet.path("description").asText(null);
                if (!Objects.equals(existingItem.getDescription(), newDescription)) {
                    logger.info("    -> Updating description for video {}", videoId);
                    // Description can be long, so not logging the content.
                    existingItem.setDescription(newDescription);
                    updated = true;
                }

                String liveBroadcastContentStr = videoSnippet.path("liveBroadcastContent").asText("NONE")
                        .toUpperCase();
                LiveBroadcastContent newLiveStatus = LiveBroadcastContent.valueOf(liveBroadcastContentStr);
                if (existingItem.getLiveBroadcastContent() != newLiveStatus) {
                    logger.info("    -> Updating live status for video {}: {} -> {}", videoId,
                            existingItem.getLiveBroadcastContent(), newLiveStatus);
                    existingItem.setLiveBroadcastContent(newLiveStatus);
                    updated = true;
                }

                JsonNode liveStreamingDetails = videoItemNode.path("liveStreamingDetails");
                if (!liveStreamingDetails.isMissingNode() && liveStreamingDetails.has("scheduledStartTime")) {
                    String scheduledTimeStr = liveStreamingDetails.path("scheduledStartTime").asText(null);
                    if (scheduledTimeStr != null) {
                        OffsetDateTime newScheduledTime = OffsetDateTime.parse(scheduledTimeStr);
                        if (!newScheduledTime.equals(existingItem.getScheduledStartTime())) {
                            logger.info("    -> Updating scheduled time for video {}: {} -> {}", videoId,
                                    existingItem.getScheduledStartTime(), newScheduledTime);
                            existingItem.setScheduledStartTime(newScheduledTime);
                            updated = true;
                        }
                    }
                }

                if (updated) {
                    itemRepository.save(existingItem);
                    updatedCount++;
                }
            }
        } catch (IOException | URISyntaxException e) {
            if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 400
                    && e.getMessage().contains("API key not valid")) {
                throw new YoutubeApiAuthException("The provided YouTube API key is not valid.", e);
            }
            throw new YoutubeApiRequestException(
                    "Failed to fetch video details for update check from YouTube API for video IDs: " + videoIds, e);
        }
        return updatedCount;
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

    /**
     * Selects the best available thumbnail URL from the thumbnails JSON object,
     * starting from the highest resolution and falling back to lower ones.
     *
     * @param thumbnailsNode The 'thumbnails' JsonNode from the YouTube API
     * response.
     * @return The URL of the best available thumbnail, or null if none are
     * found.
     */
    private String getBestAvailableThumbnailUrl(JsonNode thumbnailsNode) {
        if (thumbnailsNode == null || thumbnailsNode.isMissingNode()) {
            return null;
        }
        // Order of preference from highest to lowest resolution
        final String[] resolutions = {"maxres", "standard", "high", "medium", "default"};
        for (String res : resolutions) {
            String url = thumbnailsNode.path(res).path("url").asText(null);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int markAllManuallyDownloaded(List<String> channelIds) {
        String channelLog = (channelIds != null && !channelIds.isEmpty()) ? " for channels: " + channelIds
                : " for all channels";
        logger.info("Starting job to mark new, processable items as manually downloaded{}.", channelLog);

        Specification<Item> spec = (root, query, cb) -> {
            final OffsetDateTime now = OffsetDateTime.now();
            // To avoid N+1 queries when accessing item.getPlaylist() later.
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("playlist", JoinType.LEFT);
            }

            // Base condition: Only target items with 'NEW' status.
            Predicate predicate = cb.equal(root.get("status"), ProcessingStatus.NEW);

            // Condition 1: Standard video (liveBroadcastContent is NONE)
            Predicate isStandardVideo = cb.equal(root.get("liveBroadcastContent"), LiveBroadcastContent.NONE);

            // Condition 2: Past live stream/premiere
            Predicate isProcessableLiveStream = cb.and(
                    cb.notEqual(root.get("liveBroadcastContent"), LiveBroadcastContent.NONE),
                    root.get("scheduledStartTime").isNotNull(),
                    cb.lessThan(root.get("scheduledStartTime"), now));

            // An item is processable if it's a standard video OR a past live stream.
            predicate = cb.and(predicate, cb.or(isStandardVideo, isProcessableLiveStream));

            if (channelIds != null && !channelIds.isEmpty()) {
                predicate = cb.and(predicate, root.get("playlist").get("channel").get("channelId").in(channelIds));
            }
            return predicate;
        };
        List<Item> itemsToUpdate = itemRepository.findAll(spec);

        if (itemsToUpdate.isEmpty()) {
            logger.info("No new, processable items found to mark as manually downloaded. No update needed.");
            return 0;
        }

        logger.info("Found {} items to update. Setting status to MANUALLY_DOWNLOADED.", itemsToUpdate.size());

        for (Item item : itemsToUpdate) {
            item.setStatus(ProcessingStatus.MANUALLY_DOWNLOADED);
        }

        itemRepository.saveAll(itemsToUpdate);
        logger.info("Successfully updated {} items.", itemsToUpdate.size());
        return itemsToUpdate.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> verifyNewItems(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Map.of("new", Collections.emptyList(), "undownloaded", Collections.emptyList());
        }

        List<String> newUrls = new ArrayList<>();
        List<String> undownloadedUrls = new ArrayList<>();

        for (String url : urls) {
            String videoId = parseVideoIdFromUrl(url);
            if (videoId != null && !videoId.isBlank()) {
                Optional<Item> itemOptional = itemRepository.findByVideoId(videoId);
                if (itemOptional.isEmpty()) {
                    // Not in DB -> it's both new and undownloaded.
                    newUrls.add(url);
                    undownloadedUrls.add(url);
                } else {
                    Item item = itemOptional.get();
                    // In DB, check if it's new and processable.
                    if (isNewAndProcessable(item)) {
                        newUrls.add(url);
                    }
                    // Also check if it's undownloaded by checking its status.
                    ProcessingStatus status = item.getStatus();
                    if (status != ProcessingStatus.DOWNLOADED && status != ProcessingStatus.MANUALLY_DOWNLOADED) {
                        undownloadedUrls.add(url);
                    }
                }
            } else {
                logger.warn("Could not parse video ID from URL: {}", url);
            }
        }
        return Map.of("new", newUrls, "undownloaded", undownloadedUrls);
    }

    /**
     * Checks if an item is considered "new and processable". An item meets this
     * criteria if its status is {@code NEW} AND it is either a standard video
     * or a past live stream.
     *
     * @param item The item to check.
     * @return true if the item is new and processable, false otherwise.
     */
    private boolean isNewAndProcessable(Item item) {
        if (item.getStatus() != ProcessingStatus.NEW) {
            return false;
        }
        // The logic for "processable" is the same as in isUnprocessed, just without the
        // flag check.
        return isProcessable(item);
    }

    private boolean isProcessable(Item item) {
        // Condition 1: A standard video that is not a live stream or premiere.
        boolean isStandardVideo = item.getLiveBroadcastContent() == LiveBroadcastContent.NONE;

        // Condition 2: A past live stream or premiere that should now be available as a
        // video.
        boolean isLiveOrUpcoming = item.getLiveBroadcastContent() != LiveBroadcastContent.NONE;
        boolean scheduledTimeIsInThePast = item.getScheduledStartTime() != null
                && item.getScheduledStartTime().isBefore(OffsetDateTime.now());
        boolean isProcessableLiveStream = isLiveOrUpcoming && scheduledTimeIsInThePast;

        // An item is considered non-processed if it's a standard video OR a past live
        // stream that is now processable.
        return isStandardVideo || isProcessableLiveStream;
    }

    /**
     * Parses a YouTube video URL to extract the video ID from the 'v' query
     * parameter.
     *
     * @param url The YouTube video URL (e.g.,
     * "https://www.youtube.com/watch?v=videoId").
     * @return The video ID string, or null if it cannot be parsed.
     */
    private String parseVideoIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        // First, try to get the 'v' query parameter, which covers standard watch URLs.
        String videoId = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("v");
        if (videoId != null && !videoId.isBlank()) {
            return videoId;
        }

        // If 'v' param is not found, check for /shorts/ or /v/ or /embed/ paths.
        // Example: https://www.youtube.com/shorts/VIDEO_ID
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null && (host.equals("youtube.com") || host.endsWith(".youtube.com")
                    || host.equals("youtu.be") || host.endsWith(".youtu.be"))) {
                String path = uri.getPath();
                return Arrays.stream(path.split("/")).reduce((first, second) -> second).orElse(null);
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Map<String, Object> downloadItems(List<String> videoIds, String configName,
            String authorizationHeader) {
        logger.info("Received request to download {} items with config '{}'.", videoIds.size(), configName);

        List<Item> itemsToDownload = itemRepository.findAllByVideoIdIn(videoIds);
        if (itemsToDownload.size() != videoIds.size()) {
            logger.warn("Could not find all requested video IDs in the database. Requested: {}, Found: {}",
                    videoIds.size(), itemsToDownload.size());
        }

        List<DownloadItem> downloadItems = itemsToDownload.stream()
                .map(item -> {
                    DownloadItem downloadItem = new DownloadItem();
                    downloadItem.setVideoId(item.getVideoId());
                    downloadItem.setTitle(item.getTitle());
                    downloadItem.setThumbnailUrl(item.getThumbnailUrl());
                    downloadItem.setDescription(item.getDescription());
                    return downloadItem;
                }).toList();

        Map<String, Object> downloaderRequestPayload = new LinkedHashMap<>();
        if (configName != null) {
            downloaderRequestPayload.put("configName", configName);
        }
        downloaderRequestPayload.put("items", downloadItems);

        try {
            String downloadUrl = downloaderServiceUrl.endsWith("/") ? downloaderServiceUrl + "download"
                    : downloaderServiceUrl + "/download";
            URI downloaderUri = new URI(downloadUrl);
            try (HttpClient client = new HttpClient(Scheme.valueOf(downloaderUri.getScheme().toUpperCase()),
                    downloaderUri.getHost(), downloaderUri.getPort())) {
                String requestBody = OBJECT_MAPPER.writeValueAsString(downloaderRequestPayload);
                logger.info("Sending download request to {}: {}", downloadUrl, requestBody);
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                    headers.put("Authorization", authorizationHeader);
                }
                String responseBody = client.post("/download", null, requestBody, headers).body();
                logger.info("Received response from downloader: {}", responseBody);

                // The downloader returns an ApiResponse object, so we need to parse it and
                // extract the 'data' field.
                JsonNode rootNode = OBJECT_MAPPER.readTree(responseBody);
                JsonNode dataNode = rootNode.path("data");
                if (dataNode.isMissingNode() || !dataNode.isArray()) {
                    throw new YoutubeApiRequestException(
                            "Downloader response did not contain a valid 'data' array.");
                }

                Map<String, Item> itemMap = itemsToDownload.stream()
                        .collect(Collectors.toMap(Item::getVideoId, item -> item));
                List<DownloadInfo> newDownloadInfos = new ArrayList<>();

                for (JsonNode taskIdentifierNode : dataNode) {
                    String videoId = taskIdentifierNode.path("videoId").asText(null);
                    String taskId = taskIdentifierNode.path("taskId").asText(null);
                    Item item = itemMap.get(videoId);

                    if (item != null && taskId != null && !taskId.isBlank()) {
                        DownloadInfo downloadInfo = new DownloadInfo();
                        downloadInfo.setDownloadTaskId(taskId);
                        downloadInfo.setItem(item);
                        newDownloadInfos.add(downloadInfo);
                    }
                }

                downloadInfoRepository.saveAll(newDownloadInfos);
                logger.info("Successfully created {} DownloadInfo records.", newDownloadInfos.size());

                return Map.of("createdTasks", newDownloadInfos.size());
            }
        } catch (URISyntaxException | IOException e) {
            logger.error("Failed to call downloader service at {}. Reason: {}", downloaderServiceUrl, e.getMessage(),
                    e);
            throw new YoutubeApiRequestException("Failed to call downloader service", e);
        }
    }
}
