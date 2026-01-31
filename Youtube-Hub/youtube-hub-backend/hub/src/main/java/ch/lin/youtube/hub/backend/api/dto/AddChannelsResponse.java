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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Represents the response for the add channels by URL operation.
 * <p>
 * This DTO contains lists of channels that were successfully added or updated,
 * and URLs that failed to process. It is returned by the
 * {@link ch.lin.hub.backend.api.controller.ChannelController#addChannelsByUrl(AddChannelsByUrlRequest)}
 * endpoint.
 * <p>
 * Example JSON response:
 * <pre>
 * {@code
 * {
 *   "addedChannels": [
 *     {
 *       "id": "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
 *       "title": "SpringSource",
 *       "handle": "@SpringSourceDev",
 *       "thumbnailUrl": "https://yt3.ggpht.com/..."
 *     }
 *   ],
 *   "failedUrls": [
 *     {
 *       "url": "https://www.youtube.com/invalid-url",
 *       "reason": "Could not extract a valid channel handle or ID from the URL."
 *     }
 *   ]
 * }
 * }
 * </pre>
 */
@Getter
@AllArgsConstructor
public class AddChannelsResponse {

    /**
     * A list of channels that were successfully added or updated.
     */
    private final List<ChannelResponse> addedChannels;
    /**
     * A list of URLs that could not be processed, along with the reason for
     * failure.
     */
    private final List<FailedUrlResponse> failedUrls;

    /**
     * A nested DTO representing a single failed URL and the reason for the
     * failure.
     */
    @Getter
    @AllArgsConstructor
    public static class FailedUrlResponse {

        /**
         * The URL that could not be processed.
         */
        private final String url;
        /**
         * A message explaining why the processing failed.
         */
        private final String reason;
    }
}
