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

import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the data transfer object for a YouTube channel in API responses.
 * <p>
 * This DTO provides a client-friendly representation of a {@link Channel}
 * entity, exposing key information like the channel's ID, title, handle, and
 * thumbnail URL.
 * <p>
 * Example JSON structure:
 * <pre>
 * {@code
 * {
 *   "channelId": "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
 *   "title": "SpringSource",
 *   "handle": "@SpringSourceDev",
 *   "thumbnailUrl": "https://yt3.ggpht.com/..."
 * }
 * }
 * </pre>
 */
@Getter
@Setter
public class ChannelResponse {

    /**
     * The unique identifier of the YouTube channel (e.g.,
     * "UC-lHJZR3Gqxm24_Vd_AJ5Yw").
     */
    private String channelId;

    /**
     * The title of the YouTube channel.
     */
    private String title;

    /**
     * The handle of the YouTube channel.
     */
    private String handle;

    /**
     * Constructs a new ChannelResponse from a Channel domain entity.
     *
     * @param channel The {@link Channel} entity to map to a response DTO.
     */
    public ChannelResponse(Channel channel) {
        this.channelId = channel.getChannelId();
        this.title = channel.getTitle();
        this.handle = channel.getHandle();
    }
}
