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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.Playlist;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;

class ItemResponseTest {

    @Test
    void itemResponse_ShouldMapFromEntity() {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Test Channel");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("pl1");
        playlist.setChannel(channel);

        Item item = new Item();
        item.setVideoId("vid1");
        item.setTitle("Test Video");
        item.setKind("youtube#video");
        item.setVideoPublishedAt(OffsetDateTime.now());
        item.setScheduledStartTime(OffsetDateTime.now().plusDays(1));
        item.setThumbnailUrl("http://thumb.url");
        item.setStatus(ProcessingStatus.PENDING);
        item.setPlaylist(playlist);

        ItemResponse response = new ItemResponse(item);

        assertThat(response.getVideoId()).isEqualTo("vid1");
        assertThat(response.getTitle()).isEqualTo("Test Video");
        assertThat(response.getPlaylistId()).isEqualTo("pl1");
        assertThat(response.getChannelId()).isEqualTo("ch1");
        assertThat(response.getChannelTitle()).isEqualTo("Test Channel");
    }

    @Test
    void itemResponse_ShouldMapFromEntity_WhenPlaylistIsNull() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setPlaylist(null);

        ItemResponse response = new ItemResponse(item);

        assertThat(response.getVideoId()).isEqualTo("vid1");
        assertThat(response.getPlaylistId()).isNull();
        assertThat(response.getChannelId()).isNull();
        assertThat(response.getChannelTitle()).isNull();
    }

    @Test
    void itemResponse_ShouldMapFromEntity_WhenChannelIsNull() {
        Playlist playlist = new Playlist();
        playlist.setPlaylistId("pl1");
        playlist.setChannel(null);

        Item item = new Item();
        item.setVideoId("vid1");
        item.setPlaylist(playlist);

        ItemResponse response = new ItemResponse(item);

        assertThat(response.getVideoId()).isEqualTo("vid1");
        assertThat(response.getPlaylistId()).isEqualTo("pl1");
        assertThat(response.getChannelId()).isNull();
        assertThat(response.getChannelTitle()).isNull();
    }
}
