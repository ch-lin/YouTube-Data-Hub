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

import java.time.OffsetDateTime;

import ch.lin.platform.http.HttpClient;
import ch.lin.youtube.hub.backend.api.app.service.model.PlaylistProcessingResult;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.domain.model.Playlist;

/**
 * Service responsible for processing individual YouTube channels.
 */
public interface ChannelProcessingService {

    /**
     * Prepares the channel and its uploads playlist. This ensures the playlist
     * exists and is committed before processing items.
     *
     * @param channel the channel to prepare
     * @param client the HTTP client for making API requests
     * @param apiKey the YouTube Data API key
     * @param delayInMilliseconds the delay in milliseconds before making API
     * requests
     * @param quotaLimit the daily quota limit
     * @param quotaThreshold the safety threshold for quota
     * @return the prepared playlist
     * @throws QuotaExceededException if the daily quota limit is reached
     * @throws YoutubeApiAuthException if the API key is invalid
     * @throws YoutubeApiRequestException if the API call fails
     */
    Playlist prepareChannelAndPlaylist(Channel channel, HttpClient client, String apiKey,
            long delayInMilliseconds, long quotaLimit, long quotaThreshold);

    /**
     * Processes items for a given playlist.
     *
     * @param playlist the playlist to process
     * @param client the HTTP client for making API requests
     * @param apiKey the YouTube Data API key
     * @param publishedAfter optional datetime to filter videos
     * @param forcePublishedAfter if true, forces the use of
     * {@code publishedAfter}
     * @param delayInMilliseconds the delay in milliseconds before making API
     * requests
     * @param quotaLimit the daily quota limit
     * @param quotaThreshold the safety threshold for quota
     * @return the result of processing the playlist
     * @throws QuotaExceededException if the daily quota limit is reached
     * @throws YoutubeApiAuthException if the API key is invalid
     * @throws YoutubeApiRequestException if the API call fails
     */
    PlaylistProcessingResult processPlaylistItems(Playlist playlist, HttpClient client, String apiKey,
            OffsetDateTime publishedAfter, boolean forcePublishedAfter,
            long delayInMilliseconds, long quotaLimit, long quotaThreshold);

}
