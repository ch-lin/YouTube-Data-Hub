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
import java.util.List;
import java.util.Map;

/**
 * A high-level service that orchestrates various YouTube-related operations,
 * such as processing channels, downloading videos, and managing application
 * data.
 */
public interface YoutubeHubService {

    /**
     * Performs cleanup of application data, such as removing stale job
     * information or unreferenced entities.
     */
    void cleanup();

    /**
     * Initiates and processes a job to fetch new YouTube videos from specified
     * channels.
     *
     * @param key The YouTube Data API key
     * @param configName the name of the configuration to use for resolving the
     * API key
     * @param delayInMilliseconds the delay between processing individual
     * channels
     * @param publishedAfter the timestamp to fetch videos published after this
     * date
     * @param forcePublishedAfter if true, forces the use of
     * {@code publishedAfter} even if a more recent processing time is available
     * @param channelIds a list of YouTube channel IDs to process
     * @return a map containing the results of the job processing, such as the
     * number of new videos found
     * @throws
     * ch.lin.youtube.hub.backend.common.exception.InvalidRequestException if a
     * required parameter like the API key is missing or invalid.
     * @throws
     * ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException
     * if the provided YouTube API key is invalid.
     * @throws
     * ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException
     * if an error occurs while communicating with the YouTube Data API.
     */
    Map<String, Object> processJob(String key, String configName, Long delayInMilliseconds, OffsetDateTime publishedAfter,
            boolean forcePublishedAfter, List<String> channelIds);

    /**
     * Marks all videos from the specified channels as manually downloaded.
     *
     * @param channelIds the list of channel IDs whose videos should be marked
     * @return the total number of videos updated
     */
    int markAllManuallyDownloaded(List<String> channelIds);

    /**
     * Verifies a list of video URLs to check if they are new or already exist
     * in the system.
     *
     * @param urls a list of YouTube video URLs to verify
     * @return a map categorizing the URLs, for example, into 'new' and
     * 'existing' lists
     */
    Map<String, List<String>> verifyNewItems(List<String> urls);

    /**
     * Downloads a list of specified YouTube videos using a given configuration.
     *
     * @param videoIds the list of YouTube video IDs to download
     * @param configName the name of the download configuration to use
     * @param authorizationHeader the authorization header required for the
     * download operation
     * @return a map containing the results of the download operation, such as
     * download status for each video
     * @throws
     * ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException
     * if an error occurs while communicating with the downloader service.
     */
    Map<String, Object> downloadItems(List<String> videoIds, String configName, String authorizationHeader);

}
