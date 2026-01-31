package ch.lin.youtube.hub.backend.api.app.service;

import java.util.List;

import ch.lin.youtube.hub.backend.api.app.service.model.AddChannelsResult;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;

/**
 * Defines the service layer contract for managing YouTube channels.
 * <p>
 * This interface outlines the core business logic for operations such as
 * retrieving, adding, deleting, and fetching details for channels.
 */
public interface ChannelService {

    /**
     * Retrieves all channels stored in the local database.
     *
     * @return A list of all {@link Channel} entities.
     */
    List<Channel> getAllChannels();

    /**
     * Adds new channels by parsing a list of YouTube channel URLs.
     * <p>
     * For each URL, it extracts the channel ID, fetches channel details from
     * the YouTube API, and persists the new channel. It handles existing
     * channels gracefully and reports any URLs that could not be processed.
     *
     * @param apiKey The YouTube Data API key required for fetching channel
     * information.
     * @param configName The name of the configuration to use for resolving the
     * API key.
     * @param urls A list of YouTube channel URLs to add.
     * @return An {@link AddChannelsResult} object containing lists of
     * successfully added channels and failed URLs.
     */
    AddChannelsResult addChannelsByUrl(String apiKey, String configName, List<String> urls);

    /**
     * Fetches raw channel details as a JSON string from the YouTube Data API.
     *
     * @param channelId The unique ID of the YouTube channel.
     * @param apiKey The YouTube Data API key.
     * @param configName The name of the configuration to use for resolving the
     * API key.
     * @return A JSON string representing the channel details from the API
     * response.
     */
    String getChannelDetailsFromApi(String channelId, String apiKey, String configName);

    /**
     * Deletes a channel from the local database based on its YouTube channel
     * ID.
     *
     * @param channelId The unique ID of the channel to be deleted.
     */
    void deleteChannel(String channelId);

}
