package ch.lin.youtube.hub.backend.api.app.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.platform.http.HttpClient;
import ch.lin.platform.http.Scheme;
import ch.lin.platform.http.exception.HttpException;
import ch.lin.youtube.hub.backend.api.app.repository.ChannelRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.AddChannelsResult;
import ch.lin.youtube.hub.backend.api.app.service.model.AddChannelsResult.FailedUrl;
import ch.lin.youtube.hub.backend.api.common.exception.ChannelNotFoundException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;

/**
 * Service implementation for managing YouTube channels.
 * <p>
 * This class provides the concrete logic for operations defined in the
 * {@link ChannelService} interface. It handles interactions with the
 * {@link ChannelRepository} for database persistence and communicates with the
 * YouTube Data API via an {@link HttpClient} to fetch and update channel
 * information.
 */
@Service
public class ChannelServiceImpl implements ChannelService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChannelRepository channelRepository;
    private final ConfigsService configsService;

    public ChannelServiceImpl(ChannelRepository channelRepository,
            ConfigsService configsService) {
        this.channelRepository = channelRepository;
        this.configsService = configsService;
    }

    /**
     * Retrieves all channels stored in the database.
     *
     * @return A list of all {@link Channel} entities.
     */
    @Override
    @Transactional
    public List<Channel> getAllChannels() {
        logger.info("Fetching all channels from the database.");
        return channelRepository.findAll();
    }

    /**
     * Adds new channels by parsing their handles from a list of YouTube URLs,
     * fetching their details from the YouTube Data API, and persisting them. If
     * a channel with the same ID already exists, its title will be updated.
     * This method processes a list of URLs and returns a result that separates
     * successful additions from failures.
     *
     * @param apiKey An optional YouTube Data API key. If null or blank, the
     * service will attempt to use a configured default key.
     * @param configName The name of the configuration to use for resolving the
     * API key.
     * @param urls A list of YouTube channel URLs (e.g.,
     * "https://www.youtube.com/@handle").
     * @return An {@link AddChannelsResult} object containing the successfully
     * added channels and the URLs that failed to process.
     * @throws InvalidRequestException if no API key is provided and no default
     * key is configured.
     * @throws YoutubeApiAuthException if the provided YouTube API key is not
     * valid.
     * @throws YoutubeApiRequestException if a general I/O error occurs while
     * communicating with the YouTube API.
     */
    @Override
    @Transactional
    public AddChannelsResult addChannelsByUrl(String apiKey, String configName, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return new AddChannelsResult(Collections.emptyList(), Collections.emptyList());
        }
        String resolvedApiKey = apiKey;
        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            logger.info("No API key provided in request. Attempting to use a configured key.");
            HubConfig resolvedConfig;
            if (configName != null && !configName.isBlank()) {
                resolvedConfig = configsService.getResolvedConfig(configName);
                if (!configName.equalsIgnoreCase(resolvedConfig.getName())) {
                    throw new InvalidRequestException("Configuration with name '" + configName + "' not found.");
                }
            } else {
                resolvedConfig = configsService.getResolvedConfig(null);
            }
            logger.info("Using configuration: {}", resolvedConfig.getName());
            resolvedApiKey = resolvedConfig.getYoutubeApiKey();
        }

        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            throw new InvalidRequestException(
                    "A YouTube API key is required. None was provided in the request, and no default key is configured.");
        }

        List<Channel> savedChannels = new ArrayList<>();
        List<FailedUrl> failedUrls = new ArrayList<>();
        try (HttpClient client = new HttpClient(Scheme.HTTPS, "youtube.googleapis.com", 443)) {
            for (String url : urls) {
                String handle = parseChannelHandleFromUrl(url);
                if (handle == null) {
                    logger.warn("Could not parse a valid channel handle from URL: {}", url);
                    failedUrls.add(new FailedUrl(url, "Could not parse a valid channel handle from URL."));
                    continue;
                }

                Map<String, String> channelInfo = fetchChannelInfoByHandle(client, handle, resolvedApiKey);
                if (channelInfo.isEmpty()) {
                    logger.warn("Could not fetch channel info from YouTube API for handle: {}", handle);
                    failedUrls.add(
                            new FailedUrl(url, "Could not fetch channel info from YouTube API for handle: " + handle));
                    continue;
                }

                Channel channel = new Channel();
                channel.setChannelId(channelInfo.get("channelId"));
                channel.setTitle(channelInfo.get("title"));
                channel.setHandle(channelInfo.get("handle"));

                // The saveChannel method handles both creation of new channels and updates to
                // existing ones.
                Channel savedChannel = saveChannel(channel);
                savedChannels.add(savedChannel);
            }
        } catch (IOException e) {
            logger.error("An I/O error occurred while communicating with the YouTube API.", e);
            throw new YoutubeApiRequestException("An internal error occurred while contacting the YouTube API.", e);
        }
        return new AddChannelsResult(savedChannels, failedUrls);
    }

    /**
     * Parses a YouTube channel handle from a URL.
     *
     * @param url The full YouTube channel URL (e.g.,
     * "https://www.youtube.com/@handle").
     * @return The handle string (e.g., "handle") or null if not found.
     */
    private String parseChannelHandleFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        // Example URLs: https://www.youtube.com/@handle
        // or https://www.youtube.com/@handle/videos
        int atIndex = url.lastIndexOf('@');
        if (atIndex != -1 && atIndex < url.length() - 1) {
            String afterAt = url.substring(atIndex + 1);
            // Split by '/' to handle cases like /videos, /playlists etc. and take the
            // first part.
            String[] parts = afterAt.split("/");
            return (parts.length > 0 && !parts[0].isBlank()) ? parts[0] : null;
        }
        return null;
    }

    /**
     * Fetches channel details (ID and title) from the YouTube Data API using a
     * channel handle.
     *
     * @param client The reusable HttpClient for making the request.
     * @param handle The YouTube channel handle (e.g., "GoogleDevelopers").
     * @param apiKey The YouTube Data API key.
     * @return A map containing "channelId" and "title", or an empty map if the
     * fetch fails.
     */
    private Map<String, String> fetchChannelInfoByHandle(HttpClient client, String handle, String apiKey) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("part", "snippet");
            params.put("forHandle", handle);
            params.put("key", apiKey);

            String responseBody = client.get("/youtube/v3/channels", params, null).body();

            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode itemNode = root.path("items").path(0);

            if (!itemNode.isMissingNode()) {
                String channelId = itemNode.path("id").asText(null);
                String title = itemNode.path("snippet").path("title").asText(null);
                String customUrl = itemNode.path("snippet").path("customUrl").asText(null);

                if (channelId != null && title != null) {
                    Map<String, String> details = new HashMap<>();
                    details.put("channelId", channelId);
                    details.put("title", title);
                    details.put("handle", customUrl != null ? customUrl : "@" + handle);
                    return details;
                }
            }
            logger.warn("Could not parse 'id' or 'title' from YouTube API response for handle {}", handle);
        } catch (IOException | URISyntaxException e) {
            if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 400
                    && e.getMessage().contains("API key not valid")) {
                logger.error("The provided YouTube API key is not valid.", e);
                throw new YoutubeApiAuthException("The provided YouTube API key is not valid.", e);
            }
            logger.error("Failed to fetch channel details from YouTube API for handle {}: {}", handle, e.getMessage(),
                    e);
        }
        return Collections.emptyMap();
    }

    /**
     * Fetches comprehensive details for a specific channel directly from the
     * YouTube Data API.
     *
     * @param channelId The unique ID of the YouTube channel.
     * @param apiKey An optional YouTube Data API key. If null or blank, the
     * service will attempt to use a configured default key.
     * @param configName The name of the configuration to use for resolving the
     * API key.
     * @return A JSON string containing the channel's details.
     * @throws InvalidRequestException if the channel ID or API key is null or
     * blank, or if no key is available.
     * @throws YoutubeApiRequestException if an error occurs while communicating
     * with the YouTube API.
     */
    @Override
    public String getChannelDetailsFromApi(String channelId, String apiKey, String configName) {
        if (channelId == null || channelId.isBlank()) {
            throw new InvalidRequestException("Channel ID cannot be null or blank.");
        }
        String resolvedApiKey = apiKey;
        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            logger.info("No API key provided in request. Attempting to use a configured key.");
            HubConfig resolvedConfig;
            if (configName != null && !configName.isBlank()) {
                resolvedConfig = configsService.getResolvedConfig(configName);
                if (!configName.equalsIgnoreCase(resolvedConfig.getName())) {
                    throw new InvalidRequestException("Configuration with name '" + configName + "' not found.");
                }
            } else {
                resolvedConfig = configsService.getResolvedConfig(null);
            }
            logger.info("Using configuration: {}", resolvedConfig.getName());
            resolvedApiKey = resolvedConfig.getYoutubeApiKey();
        }

        if (resolvedApiKey == null || resolvedApiKey.isBlank()) {
            throw new InvalidRequestException(
                    "A YouTube API key is required. None was provided in the request, and no default key is configured.");
        }

        try (HttpClient client = new HttpClient(Scheme.HTTPS, "youtube.googleapis.com", 443)) {
            Map<String, String> params = new HashMap<>();
            // Fetching a comprehensive set of details for the channel.
            params.put("part", "snippet,contentDetails,statistics,brandingSettings");
            params.put("id", channelId);
            params.put("key", resolvedApiKey);

            logger.info("Fetching details for channel {} from YouTube API.", channelId);
            return client.get("/youtube/v3/channels", params, null).body();
        } catch (IOException | URISyntaxException e) {
            if (e instanceof HttpException && ((HttpException) e).getStatusCode() == 400
                    && e.getMessage().contains("API key not valid")) {
                logger.error("The provided YouTube API key is not valid.", e);
                throw new YoutubeApiAuthException("The provided YouTube API key is not valid.", e);
            }
            logger.error("An internal error occurred while contacting the YouTube API.", e);
            throw new YoutubeApiRequestException("An internal error occurred while contacting the YouTube API.", e);
        }
    }

    /**
     * Saves a channel to the database.
     * <p>
     * This method performs an "upsert" operation: if a channel with the given
     * ID already exists, its title is updated; otherwise, a new channel is
     * created.
     *
     * @param channel The {@link Channel} object with the data to save. Must not
     * be null and must have a valid ID and title.
     * @return The saved or updated {@link Channel} entity.
     * @throws InvalidRequestException if the provided channel data is invalid.
     */
    public Channel saveChannel(Channel channel) {
        if (channel == null || channel.getChannelId() == null || channel.getChannelId().isBlank()
                || channel.getTitle() == null || channel.getTitle().isBlank()
                || channel.getHandle() == null || channel.getHandle().isBlank()) {
            throw new InvalidRequestException("Channel data is invalid.");
        }

        Channel channelToSave = channelRepository.findByChannelId(channel.getChannelId())
                .map(existingChannel -> {
                    existingChannel.setTitle(channel.getTitle());
                    existingChannel.setHandle(channel.getHandle());
                    return existingChannel;
                })
                .orElse(channel);
        if (channelToSave != null) {
            return channelRepository.save(channelToSave);
        }
        throw new ChannelNotFoundException("Channel with id " + channel.getChannelId() + " not found.");
    }

    /**
     * Deletes a channel from the database based on its channel ID.
     *
     * @param channelId The unique ID of the channel to delete.
     * @throws InvalidRequestException if the channel ID is null or empty.
     * @throws ChannelNotFoundException if no channel with the given ID is
     * found.
     */
    @Override
    @Transactional
    public void deleteChannel(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            throw new InvalidRequestException("Channel ID cannot be null or empty.");
        }
        Channel channelToDelete = channelRepository.findByChannelId(channelId)
                .orElseThrow(() -> new ChannelNotFoundException("Channel with id " + channelId + " not found."));
        if (channelToDelete != null) {
            channelRepository.delete(channelToDelete);
        }
    }

}
