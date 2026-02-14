package ch.lin.youtube.hub.backend.api.app.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import ch.lin.platform.http.HttpClient;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Item;

/**
 * Service dedicated to interacting with the YouTube Data API for Video
 * resources.
 * <p>
 * This service handles the low-level details of constructing API requests,
 * parsing JSON responses, and mapping them to {@link Item} domain entities.
 */
public interface VideoFetchService {

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
    List<Item> fetchAndCreateItemsFromVideoIds(HttpClient client, String apiKey,
            Map<String, JsonNode> newVideoSnippets, long delayInMilliseconds,
            long quotaLimit, long quotaThreshold) throws InterruptedException;

    /**
     * Fetches detailed information for a batch of existing video IDs from the
     * YouTube Data API and updates the corresponding Item entities if there are
     * changes.
     *
     * @param client The reusable HttpClient for making the request.
     * @param apiKey the YouTube Data API key
     * @param existingItemsToUpdate a list of existing Item entities to check
     * for updates
     * @param delayInMilliseconds the delay in milliseconds before making the
     * API request
     * @param quotaLimit the daily quota limit
     * @param quotaThreshold the safety threshold for quota
     * @return the number of items that were updated
     * @throws InterruptedException if the thread is interrupted while sleeping.
     * @throws QuotaExceededException if the daily quota limit is reached.
     * @throws YoutubeApiAuthException if the API key is invalid.
     * @throws YoutubeApiRequestException if the API call fails.
     */
    int updateExistingItems(HttpClient client, String apiKey, List<Item> existingItemsToUpdate,
            long delayInMilliseconds, long quotaLimit, long quotaThreshold) throws InterruptedException;
}
