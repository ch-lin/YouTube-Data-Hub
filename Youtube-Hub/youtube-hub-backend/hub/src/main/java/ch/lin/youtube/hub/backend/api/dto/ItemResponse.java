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

import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the data transfer object for a YouTube video item in API
 * responses.
 * <p>
 * This DTO provides a client-friendly representation of an {@link Item} entity,
 * including key details about the video and its associations like playlist and
 * channel. It is used in endpoints like
 * {@link ch.lin.youtube.hub.backend.api.controller.ItemController#getAllItems(Boolean, Boolean, String, Boolean, Boolean, List)}.
 * <p>
 * Example JSON structure:
 * <pre>
 * {@code
 * {
 *   "videoId": "dQw4w9WgXcQ",
 *   "title": "Official Music Video",
 *   "kind": "youtube#video",
 *   "videoPublishedAt": "2009-10-25T06:54:33Z",
 *   "scheduledStartTime": null,
 *   "playlistId": "PLj00-8v2w_3sWJ-B3FDL4cI4t-gJ2_f-p",
 *   "channelId": "UC-lHJZR3Gqxm24_Vd_AJ5Yw",
 *   "channelTitle": "Official Channel",
 *   "thumbnailUrl": "https://i.ytimg.com/vi/dQw4w9WgXcQ/default.jpg",
 *   "status": "PENDING"
 * }
 * }
 * </pre>
 */
@Getter
@NoArgsConstructor
public class ItemResponse {

    /**
     * The unique identifier of the YouTube video (e.g., "dQw4w9WgXcQ").
     */
    private String videoId;

    /**
     * The title of the YouTube video.
     */
    private String title;

    /**
     * The kind of the YouTube resource (e.g., "youtube#video").
     */
    private String kind;

    /**
     * The date and time when the video was published.
     */
    private OffsetDateTime videoPublishedAt;

    /**
     * The scheduled start time for a live broadcast. This will be null for
     * regular videos that are not premieres or live streams.
     */
    public OffsetDateTime scheduledStartTime;

    /**
     * The ID of the playlist this video belongs to. Can be null if the item is
     * not associated with a playlist.
     */
    private String playlistId;

    /**
     * The ID of the channel that owns the playlist. Can be null if the item is
     * not associated with a channel via a playlist.
     */
    private String channelId;

    /**
     * The title of the channel that owns the playlist. Can be null if the item
     * is not associated with a channel.
     */
    private String channelTitle;

    /**
     * The URL of the default thumbnail for the video.
     */
    private String thumbnailUrl;

    /**
     * The processing status of the item (e.g., PENDING, DOWNLOADED).
     *
     * @see ProcessingStatus
     */
    private ProcessingStatus status;

    /**
     * Constructs a new ItemResponse from an Item domain entity.
     *
     * @param item The {@link Item} entity to map to a response DTO.
     */
    public ItemResponse(Item item) {
        this.videoId = item.getVideoId();
        this.title = item.getTitle();
        this.kind = item.getKind();
        this.videoPublishedAt = item.getVideoPublishedAt();
        this.scheduledStartTime = item.getScheduledStartTime();
        this.thumbnailUrl = item.getThumbnailUrl();
        this.status = item.getStatus();
        if (item.getPlaylist() != null) {
            this.playlistId = item.getPlaylist().getPlaylistId();
            if (item.getPlaylist().getChannel() != null) {
                this.channelId = item.getPlaylist().getChannel().getChannelId();
                this.channelTitle = item.getPlaylist().getChannel().getTitle();
            }
        }
    }

}
