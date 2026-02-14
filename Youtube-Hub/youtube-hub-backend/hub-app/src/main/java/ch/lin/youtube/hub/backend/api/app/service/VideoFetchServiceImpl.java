package ch.lin.youtube.hub.backend.api.app.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ch.lin.platform.http.HttpClient;
import ch.lin.platform.http.exception.HttpException;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.LiveBroadcastContent;

/**
 * Service dedicated to interacting with the YouTube Data API for Video
 * resources.
 * <p>
 * This service handles the low-level details of constructing API requests,
 * parsing JSON responses, and mapping them to {@link Item} domain entities.
 */
@Service
public class VideoFetchServiceImpl implements VideoFetchService {

    private static final Logger logger = LoggerFactory.getLogger(VideoFetchServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final YoutubeApiUsageService youtubeApiUsageService;
    private final ItemRepository itemRepository;

    public VideoFetchServiceImpl(YoutubeApiUsageService youtubeApiUsageService, ItemRepository itemRepository) {
        this.youtubeApiUsageService = youtubeApiUsageService;
        this.itemRepository = itemRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Item> fetchAndCreateItemsFromVideoIds(HttpClient client, String apiKey,
            Map<String, JsonNode> newVideoSnippets, long delayInMilliseconds,
            long quotaLimit, long quotaThreshold) throws InterruptedException {

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
            checkQuota(quotaLimit, quotaThreshold);
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
                    continue;
                }

                Item newItem = new Item();
                JsonNode videoSnippet = videoItemNode.path("snippet");
                newItem.setVideoId(videoId);
                newItem.setTitle(videoSnippet.path("title").asText());
                newItem.setDescription(videoSnippet.path("description").asText(null));
                newItem.setKind(videoItemNode.path("kind").asText());
                newItem.setVideoPublishedAt(OffsetDateTime.parse(originalSnippet.path("publishedAt").asText()));

                String liveBroadcastContentStr = videoSnippet.path("liveBroadcastContent").asText("NONE").toUpperCase();
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
            throw new YoutubeApiRequestException("Failed to fetch video details from YouTube API for video IDs: " + videoIds, e);
        }
        return newItems;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int updateExistingItems(HttpClient client, String apiKey, List<Item> existingItemsToUpdate,
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
            checkQuota(quotaLimit, quotaThreshold);
            delayRequest(delayInMilliseconds);

            // Quota cost: 1 unit for videos.list operation
            youtubeApiUsageService.recordUsage(1L);
            logger.debug("    -> Checking for updates for video IDs {}.", videoIds);

            String responseBody = client.get("/youtube/v3/videos", params, null).body();
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode videoItemsNode = root.path("items");

            if (videoItemsNode.isMissingNode() || !videoItemsNode.isArray()) {
                logger.warn("    -> No detailed video items found for update check in API response for video IDs: {}", videoIds);
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
                    logger.info("    -> Updating title for video {}: '{}' -> '{}'", videoId, existingItem.getTitle(), newTitle);
                    existingItem.setTitle(newTitle);
                    updated = true;
                }

                String newDescription = videoSnippet.path("description").asText(null);
                if (!Objects.equals(existingItem.getDescription(), newDescription)) {
                    logger.info("    -> Updating description for video {}", videoId);
                    existingItem.setDescription(newDescription);
                    updated = true;
                }

                String liveBroadcastContentStr = videoSnippet.path("liveBroadcastContent").asText("NONE").toUpperCase();
                LiveBroadcastContent newLiveStatus = LiveBroadcastContent.valueOf(liveBroadcastContentStr);
                if (existingItem.getLiveBroadcastContent() != newLiveStatus) {
                    logger.info("    -> Updating live status for video {}: {} -> {}", videoId, existingItem.getLiveBroadcastContent(), newLiveStatus);
                    existingItem.setLiveBroadcastContent(newLiveStatus);
                    updated = true;
                }

                JsonNode liveStreamingDetails = videoItemNode.path("liveStreamingDetails");
                if (!liveStreamingDetails.isMissingNode() && liveStreamingDetails.has("scheduledStartTime")) {
                    String scheduledTimeStr = liveStreamingDetails.path("scheduledStartTime").asText(null);
                    if (scheduledTimeStr != null) {
                        OffsetDateTime newScheduledTime = OffsetDateTime.parse(scheduledTimeStr);
                        if (!newScheduledTime.equals(existingItem.getScheduledStartTime())) {
                            logger.info("    -> Updating scheduled time for video {}: {} -> {}", videoId, existingItem.getScheduledStartTime(), newScheduledTime);
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
            throw new YoutubeApiRequestException("Failed to fetch video details for update check from YouTube API for video IDs: " + videoIds, e);
        }
        return updatedCount;
    }

    /**
     * Checks if the daily quota limit has been reached.
     *
     * @param limit The daily quota limit.
     * @param threshold The safety threshold.
     * @throws QuotaExceededException if the daily quota limit is reached.
     */
    private void checkQuota(long limit, long threshold) {
        if (!youtubeApiUsageService.hasSufficientQuota(limit, threshold)) {
            throw new QuotaExceededException("Daily quota limit reached.");
        }
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
}
