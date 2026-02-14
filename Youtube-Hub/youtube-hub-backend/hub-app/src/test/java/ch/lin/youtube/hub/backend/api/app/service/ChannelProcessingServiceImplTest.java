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

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.lin.platform.http.HttpClient;
import ch.lin.platform.http.exception.HttpException;
import ch.lin.youtube.hub.backend.api.app.repository.ChannelRepository;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.PlaylistRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.PlaylistProcessingResult;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.LiveBroadcastContent;
import ch.lin.youtube.hub.backend.api.domain.model.Playlist;

@ExtendWith(MockitoExtension.class)
class ChannelProcessingServiceImplTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private PlaylistRepository playlistRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private YoutubeApiUsageService youtubeApiUsageService;
    @Mock
    private VideoFetchService videoFetchService;
    @Mock
    private YoutubeCheckpointService youtubeCheckpointService;
    @Mock
    private HttpClient httpClient;

    private ChannelProcessingServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new ChannelProcessingServiceImpl(channelRepository, playlistRepository, itemRepository,
                youtubeApiUsageService, videoFetchService, youtubeCheckpointService);

        // Default lenient behavior for quota check
        lenient().when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong())).thenReturn(true);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldUpdateChannelTitle_WhenChanged() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Old Title");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"New Title\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(channel.getTitle()).isEqualTo("New Title");
        verify(channelRepository).save(channel);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldNotUpdateChannelTitle_WhenSame() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Same Title");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Same Title\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(channel.getTitle()).isEqualTo("Same Title");
        verify(channelRepository, never()).save(channel);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldNotUpdateChannelTitle_WhenNewTitleIsBlank() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Old Title");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(channel.getTitle()).isEqualTo("Old Title");
        verify(channelRepository, never()).save(channel);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldThrow_WhenQuotaExceeded_BeforePlaylistItemsFetch() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        // 1. fetchChannelDetailsFromApi -> true
        // 2. fetchAndProcessPlaylistItems -> false
        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong()))
                .thenReturn(true)
                .thenReturn(false);

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(QuotaExceededException.class);

        verify(httpClient, never()).get(eq("/youtube/v3/playlistItems"), anyMap(), any());
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldHandlePagination() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // Page 1: Has next page token
        String page1Response = "{\"nextPageToken\": \"token123\", \"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Page 2: No next page token
        String page2Response = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v2\"}, \"publishedAt\": \"2023-01-01T09:00:00Z\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any()))
                .thenReturn(new HttpClient.Response(200, page1Response))
                .thenReturn(new HttpClient.Response(200, page2Response));

        // Mock VideoFetchService
        Item item1 = new Item();
        item1.setVideoId("v1");
        item1.setLiveBroadcastContent(LiveBroadcastContent.NONE);
        Item item2 = new Item();
        item2.setVideoId("v2");
        item2.setLiveBroadcastContent(LiveBroadcastContent.NONE);

        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(item1))
                .thenReturn(List.of(item2));

        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(result.getNewItemsCount()).isEqualTo(2);
        verify(youtubeApiUsageService, times(3)).recordUsage(1L); // 1 channel + 2 playlistItems
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldStopFetching_WhenOldVideoFound() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        OffsetDateTime t1 = OffsetDateTime.parse("2023-01-01T10:00:00Z"); // Old
        OffsetDateTime t2 = OffsetDateTime.parse("2023-01-01T11:00:00Z"); // Last processed
        OffsetDateTime t3 = OffsetDateTime.parse("2023-01-01T12:00:00Z"); // New

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setProcessedAt(t2);
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // Response has v3 (new), v1 (old)
        String playlistItemsResponse = "{"
                + "\"nextPageToken\": \"tokenPage2\","
                + "\"items\": ["
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v3\"}, \"publishedAt\": \"" + t3 + "\"}},"
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"" + t1 + "\"}}"
                + "]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        Item item3 = new Item();
        item3.setVideoId("v3");
        item3.setLiveBroadcastContent(LiveBroadcastContent.NONE);
        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(item3));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(playlist.getProcessedAt()).isEqualTo(t3);
        assertThat(playlist.getLastPageToken()).isNull(); // Should be cleared because we stopped fetching
        verify(playlistRepository).save(playlist);
    }

    @Test
    void processSingleChannel_ShouldSaveProgress_WhenQuotaExceeded_DuringProcessing() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setLastPageToken("prevToken");
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        Item item1 = new Item();
        item1.setVideoId("v1");
        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(item1));

        // Simulate QuotaExceededException during update check
        when(videoFetchService.updateExistingItems(any(), anyString(), anyList(), anyLong(), anyLong(), anyLong()))
                .thenThrow(new QuotaExceededException("Quota exceeded"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(QuotaExceededException.class);

        // Verify that savePageProgress was called with v1 and the previous token (not advancing)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Item>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(youtubeCheckpointService).savePageProgress(eq(playlist), itemsCaptor.capture(), eq("prevToken"));

        List<Item> savedItems = itemsCaptor.getValue();
        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getVideoId()).isEqualTo("v1");
    }

    @Test
    void processSingleChannel_ShouldThrowAuthException_WhenApiKeyInvalid() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any()))
                .thenThrow(new HttpException("GET", 400, "API key not valid"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiAuthException.class)
                .hasMessageContaining("The provided YouTube API key is not valid");
    }

    @Test
    void processSingleChannel_ShouldHandleGenericHttpException() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any()))
                .thenThrow(new HttpException("GET", 500, "Server Error"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch channel details");
    }

    @Test
    void processSingleChannel_ShouldHandleIOException() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any()))
                .thenThrow(new IOException("Network Error"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch channel details");
    }

    @Test
    void processSingleChannel_ShouldHandleInterruptedException() {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong())).thenAnswer(inv -> {
            Thread.currentThread().interrupt();
            return true;
        });

        // Use a positive delay to ensure Thread.sleep is called
        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 10L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class);

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        // Clear interrupt status for other tests
        Thread.interrupted();
    }

    @Test
    void processSingleChannel_ShouldHandleURISyntaxException() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any()))
                .thenThrow(new URISyntaxException("input", "reason"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldResumeFromCheckpoint_WhenTokenExists() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setLastPageToken("checkpointToken");
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(httpClient).get(eq("/youtube/v3/playlistItems"), paramsCaptor.capture(), any());

        assertThat(paramsCaptor.getValue()).containsEntry("pageToken", "checkpointToken");
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldHandleMissingItemsInPlaylistResponse() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String emptyItemsResponse = "{}"; // No items array

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, emptyItemsResponse));

        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);
        assertThat(result.getNewItemsCount()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldSkipItemsWithBlankVideoId() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // One item with valid ID, one with blank ID
        String playlistResponse = "{\"items\": ["
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}},"
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}"
                + "]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistResponse));

        Item item1 = new Item();
        item1.setVideoId("v1");
        item1.setLiveBroadcastContent(LiveBroadcastContent.NONE);
        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(item1));

        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);
        assertThat(result.getNewItemsCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldThrow_WhenSavePageProgressFails() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        Item item1 = new Item();
        item1.setVideoId("v1");
        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(item1));

        doThrow(new RuntimeException("DB Error")).when(youtubeCheckpointService).savePageProgress(any(), anyList(), any());

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldHandleException_InItemLoop() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        // Mock Item to throw exception on setPlaylist
        Item mockItem = mock(Item.class);
        doThrow(new RuntimeException("Simulated Error")).when(mockItem).setPlaylist(any());

        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(mockItem));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        // Should still try to save progress (even if item loop failed for one item, though here it fails for the only item)
        // Actually, if fetchAndCreateItemsFromVideoIds returns items, and loop fails, we might still save them?
        // The current implementation catches generic Exception in the loop? No, the loop is:
        // for (Item newItem : createdItems) { ... }
        // If setPlaylist throws, the loop breaks (uncaught exception).
        // But processSingleChannel catches IOException | URISyntaxException | InterruptedException.
        // RuntimeException will propagate up.
        // Wait, the original test expected it to handle exception?
        // In original YoutubeHubServiceImpl:
        /*
        try {
            for (Item newItem : createdItems) { ... }
        } catch (Exception e) {
            logger.error("Failed to save a batch of items...", e);
        }
         */
        // Yes, there is a try-catch block around the loop in the implementation!
        verify(youtubeCheckpointService).savePageProgress(any(), anyList(), any());
    }

    @Test
    void processSingleChannel_ShouldThrow_WhenQuotaExceeded_Immediately() throws IOException, URISyntaxException {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        // Override default lenient stubbing to return false immediately
        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessage("Daily quota limit reached.");

        verify(httpClient, never()).get(anyString(), anyMap(), any());
    }

    @Test
    void processSingleChannel_ShouldThrow_WhenChannelResponseHasNoItems() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        String channelResponse = "{\"items\": []}";
        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Could not parse 'uploads' or 'title'");
    }

    @Test
    void processSingleChannel_ShouldThrow_WhenChannelResponseMissingUploadsId() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {}}, \"snippet\": {\"title\": \"Title\"}}]}";
        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Could not parse 'uploads' or 'title'");
    }

    @Test
    void processSingleChannel_ShouldThrow_WhenChannelResponseMissingTitle() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"UU123\"}}, \"snippet\": {}}]}";
        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Could not parse 'uploads' or 'title'");
    }

    @Test
    void processSingleChannel_ShouldThrowRequestException_WhenHttp400ButNotAuth() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any()))
                .thenThrow(new HttpException("GET", 400, "Bad Request"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch channel details");
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldOverridePublishedAfter_WhenForced() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        OffsetDateTime t1 = OffsetDateTime.parse("2023-01-01T10:00:00Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2023-01-01T11:00:00Z");
        OffsetDateTime t3 = OffsetDateTime.parse("2023-01-01T12:00:00Z");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setProcessedAt(t3); // Last processed is T3
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // API returns video at T2. T2 < T3 but T2 > T1.
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v2\"}, \"publishedAt\": \"" + t2 + "\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        Item item2 = new Item();
        item2.setVideoId("v2");
        item2.setLiveBroadcastContent(LiveBroadcastContent.NONE);
        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(item2));

        // Force T1. Effective should be T1. Video T2 > T1. Should process.
        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, t1, true);

        assertThat(result.getNewItemsCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldUseRequestPublishedAfter_WhenLastProcessedIsNull() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        OffsetDateTime t1 = OffsetDateTime.parse("2023-01-01T10:00:00Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2023-01-01T11:00:00Z");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setProcessedAt(null); // Last processed is null
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // API returns video at T1. T1 < T2.
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"" + t1 + "\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        // Request T2. Effective should be T2. Video T1 < T2. Should stop (0 items).
        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, t2, false);

        assertThat(result.getNewItemsCount()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldOverridePublishedAfter_WhenRequestIsNewer() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        OffsetDateTime t1 = OffsetDateTime.parse("2023-01-01T10:00:00Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2023-01-01T11:00:00Z");
        OffsetDateTime t3 = OffsetDateTime.parse("2023-01-01T12:00:00Z");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setProcessedAt(t1); // Last processed is T1
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // API returns video at T2. T2 > T1 but T2 < T3.
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v2\"}, \"publishedAt\": \"" + t2 + "\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        // Request T3. Effective should be T3. Video T2 < T3. Should stop (0 items).
        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, t3, false);

        assertThat(result.getNewItemsCount()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldNotOverridePublishedAfter_WhenRequestIsOlderAndNotForced() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        OffsetDateTime t1 = OffsetDateTime.parse("2023-01-01T10:00:00Z");
        OffsetDateTime t2 = OffsetDateTime.parse("2023-01-01T11:00:00Z");
        OffsetDateTime t3 = OffsetDateTime.parse("2023-01-01T12:00:00Z");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setProcessedAt(t3); // Last processed is T3
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // API returns video at T2. T2 < T3.
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v2\"}, \"publishedAt\": \"" + t2 + "\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        // Request T1. Not forced. Effective should remain T3. Video T2 < T3. Should stop (0 items).
        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, t1, false);

        assertThat(result.getNewItemsCount()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldHandleNonArrayItemsInPlaylistResponse() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String invalidItemsResponse = "{\"items\": \"not-an-array\"}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, invalidItemsResponse));

        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);
        assertThat(result.getNewItemsCount()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldQueueExistingItemsForUpdate() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        when(videoFetchService.updateExistingItems(any(), anyString(), anyList(), anyLong(), anyLong(), anyLong()))
                .thenReturn(1);

        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(result.getUpdatedItemsCount()).isEqualTo(1);
        verify(videoFetchService).updateExistingItems(eq(httpClient), eq("key"), anyList(), eq(0L), eq(1000L), eq(100L));
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldCountLiveAndUpcomingItems() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}, {\"snippet\": {\"resourceId\": {\"videoId\": \"v2\"}, \"publishedAt\": \"2023-01-01T11:00:00Z\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        Item item1 = new Item();
        item1.setVideoId("v1");
        item1.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);

        Item item2 = new Item();
        item2.setVideoId("v2");
        item2.setLiveBroadcastContent(LiveBroadcastContent.LIVE);

        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(item1, item2));

        PlaylistProcessingResult result = service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(result.getNewItemsCount()).isEqualTo(2);
        assertThat(result.getUpcomingVideoCount()).isEqualTo(1);
        assertThat(result.getLiveVideoCount()).isEqualTo(1);
    }

    @Test
    void processSingleChannel_ShouldNotSaveProgress_WhenQuotaExceeded_AndNoNewItems() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        when(videoFetchService.updateExistingItems(any(), anyString(), anyList(), anyLong(), anyLong(), anyLong()))
                .thenThrow(new QuotaExceededException("Quota exceeded"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(QuotaExceededException.class);

        verify(youtubeCheckpointService, never()).savePageProgress(any(), any(), any());
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldNotUpdateProcessedAt_WhenFinished_AndNoNewItems() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        OffsetDateTime oldProcessedAt = OffsetDateTime.parse("2020-01-01T00:00:00Z");
        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setProcessedAt(oldProcessedAt);
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(playlist.getProcessedAt()).isEqualTo(oldProcessedAt);
        verify(playlistRepository).save(playlist);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldClearToken_WhenFinished() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setLastPageToken("someToken");
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(playlist.getLastPageToken()).isNull();
        verify(playlistRepository).save(playlist);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldHandleInterruptedException_InPlaylistItems() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));

        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong()))
                .thenReturn(true)
                .thenAnswer(inv -> {
                    Thread.currentThread().interrupt();
                    return true;
                });

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 10L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch and process playlist items");

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldThrowAuthException_InPlaylistItems() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));

        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any()))
                .thenThrow(new HttpException("GET", 400, "API key not valid"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiAuthException.class);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldNotUpdateProcessedAt_WhenItemsMissingButTokenPresent() throws IOException, URISyntaxException {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setProcessedAt(OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"nextPageToken\": \"token123\"}"; // Missing items

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(playlist.getProcessedAt()).isEqualTo(OffsetDateTime.parse("2020-01-01T00:00:00Z"));
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldStopFetching_WhenOldVideoFound_WithNextPageToken() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        OffsetDateTime t1 = OffsetDateTime.parse("2023-01-01T10:00:00Z"); // Old
        OffsetDateTime t2 = OffsetDateTime.parse("2023-01-01T11:00:00Z"); // Last processed
        OffsetDateTime t3 = OffsetDateTime.parse("2023-01-01T12:00:00Z"); // New

        Playlist playlist = new Playlist();
        playlist.setPlaylistId("uploads1");
        playlist.setProcessedAt(t2);
        playlist.setChannel(channel);

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Mock item repo to return empty (new items)
        when(itemRepository.findByVideoId(anyString())).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        // Response has nextPageToken, v3 (new), v1 (old)
        String playlistItemsResponse = "{"
                + "\"nextPageToken\": \"tokenPage2\","
                + "\"items\": ["
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v3\"}, \"publishedAt\": \"" + t3 + "\"}},"
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"" + t1 + "\"}}"
                + "]}";

        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));
        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any())).thenReturn(new HttpClient.Response(200, playlistItemsResponse));

        // Mock VideoFetchService
        Item item3 = new Item();
        item3.setVideoId("v3");
        item3.setLiveBroadcastContent(LiveBroadcastContent.NONE);
        when(videoFetchService.fetchAndCreateItemsFromVideoIds(any(), anyString(), anyMap(), anyLong(), anyLong(), anyLong()))
                .thenReturn(List.of(item3));

        service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false);

        assertThat(playlist.getProcessedAt()).isEqualTo(t3);
        assertThat(playlist.getLastPageToken()).isNull();
        verify(playlistRepository).save(playlist);
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldThrowRequestException_WhenPlaylistItemsHttp400ButNotAuth() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));

        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any()))
                .thenThrow(new HttpException("GET", 400, "Bad Request"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch and process playlist items");
    }

    @Test
    @SuppressWarnings("null")
    void processSingleChannel_ShouldThrowRequestException_WhenPlaylistItemsHttp500() throws Exception {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        when(httpClient.get(eq("/youtube/v3/channels"), anyMap(), any())).thenReturn(new HttpClient.Response(200, channelResponse));

        when(httpClient.get(eq("/youtube/v3/playlistItems"), anyMap(), any()))
                .thenThrow(new HttpException("GET", 500, "Server Error"));

        assertThatThrownBy(() -> service.processSingleChannel(channel, httpClient, "key", 0L, 1000L, 100L, null, false))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch and process playlist items");
    }
}
