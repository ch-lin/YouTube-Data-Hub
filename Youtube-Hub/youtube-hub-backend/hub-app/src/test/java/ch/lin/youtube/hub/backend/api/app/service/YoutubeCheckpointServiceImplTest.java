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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.PlaylistRepository;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.Playlist;

@ExtendWith(MockitoExtension.class)
class YoutubeCheckpointServiceImplTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private PlaylistRepository playlistRepository;

    private YoutubeCheckpointServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new YoutubeCheckpointServiceImpl(itemRepository, playlistRepository);
    }

    @Test
    void savePageProgress_ShouldSaveItemsAndPlaylist_WhenItemsPresent() {
        Playlist playlist = new Playlist();
        playlist.setPlaylistId("pl1");
        Item item = new Item();
        List<Item> newItems = List.of(item);
        String nextPageToken = "token123";

        service.savePageProgress(playlist, newItems, nextPageToken);

        verify(itemRepository).saveAll(Objects.requireNonNull(newItems));
        assertThat(playlist.getLastPageToken()).isEqualTo(nextPageToken);
        verify(playlistRepository).save(playlist);
    }

    @Test
    @SuppressWarnings("null")
    void savePageProgress_ShouldOnlySavePlaylist_WhenItemsEmpty() {
        Playlist playlist = new Playlist();
        playlist.setPlaylistId("pl1");
        List<Item> newItems = Collections.emptyList();
        String nextPageToken = "token123";

        service.savePageProgress(playlist, newItems, nextPageToken);

        verify(itemRepository, never()).saveAll(any());
        assertThat(playlist.getLastPageToken()).isEqualTo(nextPageToken);
        verify(playlistRepository).save(playlist);
    }

    @Test
    @SuppressWarnings("null")
    void savePageProgress_ShouldOnlySavePlaylist_WhenItemsNull() {
        Playlist playlist = new Playlist();
        playlist.setPlaylistId("pl1");
        String nextPageToken = "token123";

        service.savePageProgress(playlist, null, nextPageToken);

        verify(itemRepository, never()).saveAll(any());
        assertThat(playlist.getLastPageToken()).isEqualTo(nextPageToken);
        verify(playlistRepository).save(playlist);
    }
}
