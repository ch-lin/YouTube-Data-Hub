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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.PlaylistRepository;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.Playlist;

/**
 * Implementation of {@link YoutubeCheckpointService}.
 * <p>
 * This service handles the persistence of intermediate progress (checkpoints)
 * in a separate transaction to ensure data integrity even if the main
 * processing flow is interrupted (e.g., by a quota limit exception).
 */
@Service
public class YoutubeCheckpointServiceImpl implements YoutubeCheckpointService {

    private static final Logger logger = LoggerFactory.getLogger(YoutubeCheckpointServiceImpl.class);

    private final ItemRepository itemRepository;
    private final PlaylistRepository playlistRepository;

    public YoutubeCheckpointServiceImpl(ItemRepository itemRepository, PlaylistRepository playlistRepository) {
        this.itemRepository = itemRepository;
        this.playlistRepository = playlistRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePageProgress(Playlist playlist, List<Item> newItems, String nextPageToken) {
        if (newItems != null && !newItems.isEmpty()) {
            itemRepository.saveAll(newItems);
        }
        playlist.setLastPageToken(nextPageToken);
        playlistRepository.save(playlist);
        logger.debug("Checkpoint saved for playlist {}. Next token: {}", playlist.getPlaylistId(), nextPageToken);
    }
}