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
package ch.lin.youtube.hub.backend.api.domain.model;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemTest {

    private Item item1;
    private Item item2;

    private String videoId;
    private String title;
    private String description;
    private String kind;
    private OffsetDateTime videoPublishedAt;
    private LiveBroadcastContent liveBroadcastContent;
    private OffsetDateTime scheduledStartTime;
    private String thumbnailUrl;
    private ProcessingStatus status;
    private Playlist playlist;
    private Tag tag;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        videoId = "videoId123";
        title = "Test Video";
        description = "This is a test video description.";
        kind = "youtube#video";
        videoPublishedAt = OffsetDateTime.now();
        liveBroadcastContent = LiveBroadcastContent.NONE;
        scheduledStartTime = OffsetDateTime.now().plusDays(1);
        thumbnailUrl = "http://example.com/thumb.jpg";
        status = ProcessingStatus.DOWNLOADED;
        playlist = new Playlist(); // Using real objects for simplicity
        tag = new Tag();

        item1 = new Item(videoId, title, description, kind, videoPublishedAt, liveBroadcastContent, scheduledStartTime,
                thumbnailUrl, status, playlist, tag, null);
        item2 = new Item(videoId, title, description, kind, videoPublishedAt, liveBroadcastContent, scheduledStartTime,
                thumbnailUrl, status, playlist, tag, null);
    }

    @Test
    void testGettersAndSetters() {
        Item item = new Item();
        Long id = 1L;

        item.setId(id);
        item.setVideoId(videoId);
        item.setDescription(description);
        item.setTitle(title);
        item.setKind(kind);
        item.setVideoPublishedAt(videoPublishedAt);
        item.setLiveBroadcastContent(liveBroadcastContent);
        item.setScheduledStartTime(scheduledStartTime);
        item.setThumbnailUrl(thumbnailUrl);
        item.setStatus(status);
        item.setPlaylist(playlist);
        item.setTag(tag);

        assertAll("Verify all getters",
                () -> assertEquals(id, item.getId()),
                () -> assertEquals(videoId, item.getVideoId()),
                () -> assertEquals(description, item.getDescription()),
                () -> assertEquals(title, item.getTitle()),
                () -> assertEquals(kind, item.getKind()),
                () -> assertEquals(videoPublishedAt, item.getVideoPublishedAt()),
                () -> assertEquals(liveBroadcastContent, item.getLiveBroadcastContent()),
                () -> assertEquals(scheduledStartTime, item.getScheduledStartTime()),
                () -> assertEquals(thumbnailUrl, item.getThumbnailUrl()),
                () -> assertEquals(status, item.getStatus()),
                () -> assertEquals(playlist, item.getPlaylist()),
                () -> assertEquals(tag, item.getTag()));
    }

    @Test
    void testNoArgsConstructor() {
        Item item = new Item();
        assertNotNull(item);
        assertNull(item.getId());
        assertNull(item.getVideoId());
        assertNull(item.getTitle());
        assertEquals(ProcessingStatus.NEW, item.getStatus(), "Status should default to NEW");
    }

    @Test
    void testAllArgsConstructor() {
        assertNotNull(item1);
        assertNull(item1.getId()); // BaseEntity ID is not part of AllArgsConstructor
        assertEquals(videoId, item1.getVideoId());
        assertEquals(description, item1.getDescription());
        assertEquals(title, item1.getTitle());
        assertEquals(status, item1.getStatus());
        assertEquals(playlist, item1.getPlaylist());
        assertEquals(tag, item1.getTag());
    }

    @Test
    void testEqualsAndHashCodeContract() {
        // Equal objects
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

        // Same instance
        assertEquals(item1, item1);

        // Not equal to null or other types
        assertNotEquals(item1, null);
        assertNotEquals(item1, new Object());
    }

    @Test
    void testEqualsAndHashCode_WhenFieldChanges() {
        // Change a field NOT included in 'of' clause (title)
        item2.setTitle("A Different Title");
        assertEquals(item1, item2, "Changing title should not affect equality as it is not part of the business key");
        assertEquals(item1.hashCode(), item2.hashCode());

        // Change another field NOT included in 'of' clause (description)
        item2.setDescription("A different description");
        assertEquals(item1, item2, "Changing description should not affect equality");
        assertEquals(item1.hashCode(), item2.hashCode());

        // Change the field included in 'of' clause (videoId)
        item2.setVideoId("differentVideoId");
        assertNotEquals(item1, item2, "Changing videoId should affect equality");
        assertNotEquals(item1.hashCode(), item2.hashCode());
    }
}
