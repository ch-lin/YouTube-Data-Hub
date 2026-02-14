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
import ch.lin.youtube.hub.backend.api.app.repository.ChannelRepository;
import ch.lin.youtube.hub.backend.api.app.repository.DownloadInfoRepository;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.PlaylistRepository;
import ch.lin.youtube.hub.backend.api.app.repository.TagRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.DownloadItem;
import ch.lin.youtube.hub.backend.api.app.service.model.PlaylistProcessingResult;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
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
    private final ChannelProcessingService channelProcessingService;
    @Value("${youtube.hub.downloader.url}")
    private String downloaderServiceUrl;

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
            ChannelProcessingService channelProcessingService) {
        this.channelRepository = channelRepository;
        this.itemRepository = itemRepository;
        this.playlistRepository = playlistRepository;
        this.tagRepository = tagRepository;
        this.downloadInfoRepository = downloadInfoRepository;
        this.configsService = configsService;
        this.channelProcessingService = channelProcessingService;
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
                    Playlist playlist = channelProcessingService.prepareChannelAndPlaylist(
                            channel, client, apiKey, delayInMilliseconds, quotaLimit, quotaThreshold);

                    PlaylistProcessingResult channelResult = channelProcessingService.processPlaylistItems(
                            playlist, client, apiKey, publishedAfter, forcePublishedAfter, delayInMilliseconds, quotaLimit, quotaThreshold);

                    newItemsCount += channelResult.getNewItemsCount();
                    standardVideoCount += channelResult.getStandardVideoCount();
                    upcomingVideoCount += channelResult.getUpcomingVideoCount();
                    liveVideoCount += channelResult.getLiveVideoCount();
                    updatedItemsCount += channelResult.getUpdatedItemsCount();
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
