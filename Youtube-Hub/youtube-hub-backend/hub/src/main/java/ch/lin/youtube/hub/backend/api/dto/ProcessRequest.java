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
package ch.lin.youtube.hub.backend.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the request body for triggering the main YouTube processing job.
 * <p>
 * This DTO is used by the
 * {@link ch.lin.youtube.hub.backend.api.controller.YoutubeHubController#processJob(ProcessRequest)}
 * endpoint to capture parameters for the job, such as an optional API key,
 * processing delay, and filters. Example in a cURL request body:
 *
 * <pre>
 * {@code
 * {
 *   "apiKey": "your-optional-api-key",
 *   "delayInMilliseconds": 100,
 *   "publishedAfter": "2023-10-27T10:00:00+02:00",
 *   "forcePublishedAfter": true,
 *   "channelIds": ["UC-lHJZR3Gqxm24_Vd_AJ5Yw", "UCuAXFkgsw1L7xaCfnd5JJOw"]
 * }
 * }
 * </pre>
 */
@Getter
@Setter
public class ProcessRequest {

    /**
     * The YouTube Data API key for making requests. If not provided, the system
     * will attempt to use a configured default key from the database or
     * application properties.
     */
    private String apiKey;

    /**
     * The name of the configuration to use for resolving the API key. If
     * provided, the system will attempt to find a configuration with this name.
     * If not provided, the default configuration will be used.
     */
    private String configName;

    /**
     * The delay in milliseconds between each YouTube Data API request to avoid
     * hitting rate limits. Defaults to 100 milliseconds.
     */
    @Min(value = 0, message = "Delay must be a non-negative number.")
    private Long delayInMilliseconds = 100L;

    /**
     * An optional datetime to fetch videos published after this point. If
     * provided, it will be used as a filter. By default, it only applies if
     * it's newer than the last processed time stored in the database. Use
     * {@link #forcePublishedAfter} to override this behavior and always use
     * this timestamp if it's provided.
     */
    private OffsetDateTime publishedAfter;

    /**
     * If true, forces the use of {@code publishedAfter} as the cutoff time,
     * ignoring any previously stored `processedAt` timestamp for a playlist.
     * This has no effect if {@code publishedAfter} is null. Defaults to false.
     */
    private Boolean forcePublishedAfter = false;

    /**
     * An optional list of channel IDs to process. If this list is provided and
     * not empty, only the channels with these IDs will be processed. If the
     * list is null or empty, all channels in the database will be processed.
     */
    private List<String> channelIds;
}
