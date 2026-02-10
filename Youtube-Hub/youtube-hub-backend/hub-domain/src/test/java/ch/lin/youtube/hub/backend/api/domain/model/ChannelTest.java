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

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class ChannelTest {

    @Test
    void testGettersAndSetters() {
        Channel channel = new Channel();
        Long id = 1L;
        String channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw";
        String title = "Google for Developers";
        String handle = "@GoogleDevelopers";
        Set<Playlist> playlists = new HashSet<>();

        channel.setId(id);
        channel.setChannelId(channelId);
        channel.setTitle(title);
        channel.setHandle(handle);
        channel.setPlaylists(playlists);

        assertEquals(id, channel.getId());
        assertEquals(channelId, channel.getChannelId());
        assertEquals(title, channel.getTitle());
        assertEquals(handle, channel.getHandle());
        assertEquals(playlists, channel.getPlaylists());
    }

    @Test
    void testNoArgsConstructor() {
        Channel channel = new Channel();
        assertNotNull(channel);
        assertNull(channel.getId());
        assertNull(channel.getChannelId());
        assertNull(channel.getTitle());
        assertNull(channel.getHandle());
        assertNull(channel.getPlaylists());
    }

    @Test
    void testAllArgsConstructor() {
        String channelId = "UC_x5XG1OV2P6uZZ5FSM9Ttw";
        String title = "Google for Developers";
        String handle = "@GoogleDevelopers";
        Set<Playlist> playlists = new HashSet<>();

        Channel channel = new Channel(channelId, title, handle, playlists);

        assertNull(channel.getId()); // BaseEntity ID is not part of AllArgsConstructor
        assertEquals(channelId, channel.getChannelId());
        assertEquals(title, channel.getTitle());
        assertEquals(handle, channel.getHandle());
        assertEquals(playlists, channel.getPlaylists());
    }

    @Test
    void testEqualsAndHashCode() {
        String channelId1 = "UC_x5XG1OV2P6uZZ5FSM9Ttw";
        String title1 = "Google for Developers";
        String handle1 = "@GoogleDevelopers";
        Channel channel1 = new Channel(channelId1, title1, handle1, new HashSet<>());

        String channelId2 = "UC_x5XG1OV2P6uZZ5FSM9Ttw";
        String title2 = "Google for Developers";
        String handle2 = "@GoogleDevelopers";
        Channel channel2 = new Channel(channelId2, title2, handle2, new HashSet<>());

        String channelId3 = "UC-lHJZR3Gqxm24_Vd_AJ5Yw";
        String title3 = "Another Channel";
        String handle3 = "@AnotherChannel";
        Channel channel3 = new Channel(channelId3, title3, handle3, new HashSet<>());

        Channel channel4 = new Channel(channelId1, title3, handle1, new HashSet<>());

        // Test for equality
        assertEquals(channel1, channel2);
        // Same ID, different title -> Should be equal now
        assertEquals(channel1, channel4);

        assertNotEquals(channel1, channel3);
        assertNotEquals(channel2, channel3);

        // Test hash code contract
        assertEquals(channel1.hashCode(), channel2.hashCode());
        assertNotEquals(channel1.hashCode(), channel3.hashCode());
        assertEquals(channel1.hashCode(), channel4.hashCode());

        // Test against null and other types
        assertNotEquals(channel1, null);
        assertNotEquals(channel1, new Object());
    }

    @Test
    void testEqualsWithSameInstance() {
        Channel channel = new Channel("id", "title", "handle", null);
        assertEquals(channel, channel);
    }
}
