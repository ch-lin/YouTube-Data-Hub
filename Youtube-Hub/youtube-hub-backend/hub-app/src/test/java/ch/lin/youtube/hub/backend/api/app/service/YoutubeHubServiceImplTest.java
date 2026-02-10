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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.mockito.MockedConstruction;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.platform.http.HttpClient;
import ch.lin.youtube.hub.backend.api.app.repository.ChannelRepository;
import ch.lin.youtube.hub.backend.api.app.repository.DownloadInfoRepository;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.PlaylistRepository;
import ch.lin.youtube.hub.backend.api.app.repository.TagRepository;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.domain.model.DownloadInfo;
import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.LiveBroadcastContent;
import ch.lin.youtube.hub.backend.api.domain.model.Playlist;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class YoutubeHubServiceImplTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private PlaylistRepository playlistRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private DownloadInfoRepository downloadInfoRepository;
    @Mock
    private ConfigsService configsService;
    @Mock
    private YoutubeApiUsageService youtubeApiUsageService;

    private YoutubeHubServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new YoutubeHubServiceImpl(channelRepository, itemRepository, playlistRepository, tagRepository,
                downloadInfoRepository, configsService, youtubeApiUsageService);
        ReflectionTestUtils.setField(service, "downloaderServiceUrl", "http://localhost:8081");

        // Default to having sufficient quota for all tests to prevent QuotaExceededException
        lenient().when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong())).thenReturn(true);
    }

    @Test
    void cleanup_ShouldCallRepositories() {
        service.cleanup();

        verify(downloadInfoRepository).cleanTable();
        verify(itemRepository).cleanTable();
        verify(playlistRepository).cleanTable();
        verify(channelRepository).cleanTable();
        verify(tagRepository).cleanTable();

        verify(downloadInfoRepository).resetSequence();
        verify(itemRepository).resetSequence();
        verify(playlistRepository).resetSequence();
        verify(channelRepository).resetSequence();
        verify(tagRepository).resetSequence();
    }

    @Test
    void verifyNewItems_ShouldCategorizeUrls() {
        String newUrl = "https://www.youtube.com/watch?v=new123";
        String existingNewStandardUrl = "https://www.youtube.com/watch?v=existNewStd";
        String existingNewFutureLiveUrl = "https://www.youtube.com/watch?v=existNewFuture";
        String existingDownloadedUrl = "https://www.youtube.com/watch?v=existDown";
        String invalidUrl = "https://google.com";

        Item itemNewStd = new Item();
        itemNewStd.setVideoId("existNewStd");
        itemNewStd.setStatus(ProcessingStatus.NEW);
        itemNewStd.setLiveBroadcastContent(LiveBroadcastContent.NONE);

        Item itemNewFuture = new Item();
        itemNewFuture.setVideoId("existNewFuture");
        itemNewFuture.setStatus(ProcessingStatus.NEW);
        itemNewFuture.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        itemNewFuture.setScheduledStartTime(OffsetDateTime.now().plusDays(1));

        Item itemDownloaded = new Item();
        itemDownloaded.setVideoId("existDown");
        itemDownloaded.setStatus(ProcessingStatus.DOWNLOADED);

        when(itemRepository.findByVideoId("new123")).thenReturn(Optional.empty());
        when(itemRepository.findByVideoId("existNewStd")).thenReturn(Optional.of(itemNewStd));
        when(itemRepository.findByVideoId("existNewFuture")).thenReturn(Optional.of(itemNewFuture));
        when(itemRepository.findByVideoId("existDown")).thenReturn(Optional.of(itemDownloaded));

        Map<String, List<String>> result = service.verifyNewItems(List.of(newUrl, existingNewStandardUrl, existingNewFutureLiveUrl, existingDownloadedUrl, invalidUrl));

        assertThat(result.get("new")).containsExactlyInAnyOrder(newUrl, existingNewStandardUrl);
        assertThat(result.get("undownloaded")).containsExactlyInAnyOrder(newUrl, existingNewStandardUrl, existingNewFutureLiveUrl);
    }

    @Test
    @SuppressWarnings({"null", "unchecked"})
    void markAllManuallyDownloaded_ShouldUpdateItems() {
        Item item1 = new Item();
        item1.setStatus(ProcessingStatus.NEW);
        Item item2 = new Item();
        item2.setStatus(ProcessingStatus.NEW);

        when(itemRepository.findAll(any(Specification.class))).thenReturn(List.of(item1, item2));

        int count = service.markAllManuallyDownloaded(List.of("ch1"));

        assertThat(count).isEqualTo(2);
        assertThat(item1.getStatus()).isEqualTo(ProcessingStatus.MANUALLY_DOWNLOADED);
        assertThat(item2.getStatus()).isEqualTo(ProcessingStatus.MANUALLY_DOWNLOADED);
        verify(itemRepository).saveAll(anyList());
    }

    @Test
    void processJob_ShouldThrow_WhenNoApiKey() {
        when(configsService.getResolvedConfig(null)).thenReturn(new HubConfig());

        assertThatThrownBy(() -> service.processJob(null, null, null, null, false, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("A YouTube API key is required");
    }

    @Test
    void processJob_ShouldProcessChannels_AndHandleFailures() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    HttpClient.Response response = new HttpClient.Response(200, "{\"items\": []}");
                    when(mock.get(eq("/youtube/v3/channels"), any(), any())).thenReturn(response);
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("processedChannels")).isEqualTo(0);
            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("channelId")).isEqualTo("ch1");
            assertThat(mocked.constructed()).hasSize(1);
            verify(youtubeApiUsageService).recordUsage(1L);
        }
    }

    @Test
    void processJob_ShouldStopEarly_WhenQuotaExceeded() throws Exception {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        // Simulate quota exceeded
        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong())).thenReturn(false);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class)) {
            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // Should stop immediately, so 0 processed
            assertThat(result.get("processedChannels")).isEqualTo(0);
            // Verify that we checked quota
            verify(youtubeApiUsageService).hasSufficientQuota(anyLong(), anyLong());
            // Verify no API calls were made
            assertThat(mocked.constructed()).hasSize(1);
            verify(mocked.constructed().get(0), never()).get(anyString(), anyMap(), any());
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldStop_WhenQuotaExceeded_BeforePlaylistItemsFetch() throws Exception {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        // 1. fetchChannelDetailsFromApi -> true
        // 2. fetchAndProcessPlaylistItems -> false
        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong()))
                .thenReturn(true)
                .thenReturn(false);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("processedChannels")).isEqualTo(0);
            verify(youtubeApiUsageService, times(2)).hasSufficientQuota(anyLong(), anyLong());

            HttpClient client = mocked.constructed().get(0);
            verify(client).get(eq("/youtube/v3/channels"), any(), any());
            verify(client, never()).get(eq("/youtube/v3/playlistItems"), any(), any());
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldStop_WhenQuotaExceeded_BeforeVideoDetailsFetch() throws Exception {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty()); // New item

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        // 1. fetchChannelDetailsFromApi -> true
        // 2. fetchAndProcessPlaylistItems (playlistItems) -> true
        // 3. fetchAndCreateItemsFromVideoIds (videos) -> false
        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("processedChannels")).isEqualTo(0);
            verify(youtubeApiUsageService, times(3)).hasSufficientQuota(anyLong(), anyLong());

            HttpClient client = mocked.constructed().get(0);
            verify(client).get(eq("/youtube/v3/channels"), any(), any());
            verify(client).get(eq("/youtube/v3/playlistItems"), any(), any());
            verify(client, never()).get(eq("/youtube/v3/videos"), any(), any());
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldStop_WhenQuotaExceeded_BeforeVideoUpdateCheck() throws Exception {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem)); // Existing item

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        // 1. fetchChannelDetailsFromApi -> true
        // 2. fetchAndProcessPlaylistItems (playlistItems) -> true
        // 3. updateExistingItems (videos) -> false
        // Note: fetchAndCreateItemsFromVideoIds is skipped because no new items
        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("processedChannels")).isEqualTo(0);
            verify(youtubeApiUsageService, times(3)).hasSufficientQuota(anyLong(), anyLong());

            HttpClient client = mocked.constructed().get(0);
            verify(client).get(eq("/youtube/v3/channels"), any(), any());
            verify(client).get(eq("/youtube/v3/playlistItems"), any(), any());
            verify(client, never()).get(eq("/youtube/v3/videos"), any(), any());
        }
    }

    @Test
    @SuppressWarnings("null")
    void downloadItems_ShouldCallDownloader() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setTitle("Title");
        when(itemRepository.findAllByVideoIdIn(List.of("vid1"))).thenReturn(List.of(item));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    HttpClient.Response response = new HttpClient.Response(200, "{\"data\": [{\"videoId\": \"vid1\", \"taskId\": \"task1\"}]}");
                    when(mock.post(eq("/download"), any(), anyString(), any())).thenReturn(response);
                })) {

            Map<String, Object> result = service.downloadItems(List.of("vid1"), "default", null);

            assertThat(result.get("createdTasks")).isEqualTo(1);
            verify(downloadInfoRepository).saveAll(anyList());
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldProcessItems_WithDifferentLiveStatuses() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByVideoId(anyString())).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}, {\"snippet\": {\"resourceId\": {\"videoId\": \"v2\"}, \"publishedAt\": \"2023-01-01T11:00:00Z\"}}, {\"snippet\": {\"resourceId\": {\"videoId\": \"v3\"}, \"publishedAt\": \"2023-01-01T12:00:00Z\"}}]}";
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 1\", \"liveBroadcastContent\": \"none\"}}, {\"id\": \"v2\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 2\", \"liveBroadcastContent\": \"upcoming\"}, \"liveStreamingDetails\": {\"scheduledStartTime\": \"2023-01-02T10:00:00Z\"}}, {\"id\": \"v3\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 3\", \"liveBroadcastContent\": \"live\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("processedChannels")).isEqualTo(1);
            assertThat(result.get("newItems")).isEqualTo(3);
            assertThat(result.get("standardVideoCount")).isEqualTo(1);
            assertThat(result.get("upcomingVideoCount")).isEqualTo(1);
            assertThat(result.get("liveVideoCount")).isEqualTo(1);
            assertThat(mocked.constructed()).hasSize(1);
            verify(youtubeApiUsageService, times(3)).recordUsage(1L);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldUpdateExistingItems() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Old Title");
        existingItem.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"New Title\", \"liveBroadcastContent\": \"live\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("updatedItemsCount")).isEqualTo(1);
            assertThat(existingItem.getTitle()).isEqualTo("New Title");
            assertThat(existingItem.getLiveBroadcastContent()).isEqualTo(LiveBroadcastContent.LIVE);
            assertThat(mocked.constructed()).hasSize(1);
            verify(youtubeApiUsageService, times(3)).recordUsage(1L);
        }
    }

    @Test
    void processJob_ShouldUseProvidedApiKey_AndResolveConfigForQuota() {
        HubConfig config = new HubConfig();
        config.setQuota(10000L);
        config.setQuotaSafetyThreshold(500L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any())).thenReturn(new HttpClient.Response(200, "{\"items\": []}"));
                })) {
            service.processJob("provided-key", null, 100L, null, false, null);
            assertThat(mocked.constructed()).hasSize(1);
        }

        verify(configsService).getResolvedConfig(null);
    }

    @Test
    void processJob_ShouldThrow_WhenConfigNameMismatch() {
        HubConfig config = new HubConfig();
        config.setName("default");
        when(configsService.getResolvedConfig("custom")).thenReturn(config);

        assertThatThrownBy(() -> service.processJob(null, "custom", null, null, false, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Configuration with name 'custom' not found");
    }

    @Test
    void processJob_ShouldFilterByChannelIds() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        List<String> channelIds = List.of("ch1");
        when(channelRepository.findAllByChannelIdIn(channelIds)).thenReturn(List.of());

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any())).thenReturn(new HttpClient.Response(200, "{\"items\": []}"));
                })) {
            service.processJob(null, null, 100L, null, false, channelIds);
            assertThat(mocked.constructed()).hasSize(1);
        }

        verify(channelRepository).findAllByChannelIdIn(channelIds);
        verify(channelRepository, never()).findAll();
    }

    @Test
    void processJob_ShouldHandleNegativeDelay() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any())).thenReturn(new HttpClient.Response(200, "{\"items\": []}"));
                })) {
            service.processJob(null, null, -1L, null, false, null);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldResolveConfig_WhenApiKeyIsBlank() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any())).thenReturn(new HttpClient.Response(200, "{\"items\": []}"));
                })) {
            service.processJob("   ", null, 100L, null, false, null);
            assertThat(mocked.constructed()).hasSize(1);
        }

        verify(configsService).getResolvedConfig(null);
    }

    @Test
    void processJob_ShouldResolveDefault_WhenConfigNameIsBlank() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any())).thenReturn(new HttpClient.Response(200, "{\"items\": []}"));
                })) {
            service.processJob(null, "   ", 100L, null, false, null);
            assertThat(mocked.constructed()).hasSize(1);
        }

        verify(configsService).getResolvedConfig(null);
    }

    @Test
    void processJob_ShouldSucceed_WhenConfigNameMatches() {
        HubConfig config = new HubConfig();
        config.setName("custom");
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig("custom")).thenReturn(config);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any())).thenReturn(new HttpClient.Response(200, "{\"items\": []}"));
                })) {
            service.processJob(null, "custom", 100L, null, false, null);
            assertThat(mocked.constructed()).hasSize(1);
        }

        verify(configsService).getResolvedConfig("custom");
    }

    @Test
    void processJob_ShouldThrow_WhenResolvedApiKeyIsBlank() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("   ");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        assertThatThrownBy(() -> service.processJob(null, null, null, null, false, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("A YouTube API key is required");
    }

    @Test
    void processJob_ShouldFetchAllChannels_WhenChannelIdsIsEmpty() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        List<String> channelIds = Collections.emptyList();

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any())).thenReturn(new HttpClient.Response(200, "{\"items\": []}"));
                })) {
            service.processJob(null, null, 100L, null, false, channelIds);
            assertThat(mocked.constructed()).hasSize(1);
        }

        verify(channelRepository).findAll();
        verify(channelRepository, never()).findAllByChannelIdIn(any());
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldUpdateChannelTitle_WhenChanged() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Old Title");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"New Title\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                })) {
            service.processJob(null, null, null, null, false, null);
            assertThat(channel.getTitle()).isEqualTo("New Title");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldNotUpdateChannelTitle_WhenSame() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Same Title");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Same Title\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                })) {
            service.processJob(null, null, null, null, false, null);
            assertThat(channel.getTitle()).isEqualTo("Same Title");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldNotUpdateChannelTitle_WhenNewTitleIsBlank() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Old Title");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"\"}}]}";
        String playlistItemsResponse = "{\"items\": []}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                })) {
            service.processJob(null, null, null, null, false, null);
            assertThat(channel.getTitle()).isEqualTo("Old Title");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldThrow_WhenHttpClientCloseFails() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        try (@SuppressWarnings("unused") MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    doThrow(new IOException("Close failed")).when(mock).close();
                })) {
            assertThatThrownBy(() -> service.processJob(null, null, 100L, null, false, null))
                    .isInstanceOf(YoutubeApiRequestException.class)
                    .hasMessageContaining("An I/O error occurred with the YouTube API client");
        }
    }

    @Test
    void processJob_ShouldFail_WhenUploadsIdMissing() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        // Response with missing uploads ID
        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Could not parse 'uploads' or 'title'");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldFail_WhenTitleMissing() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        // Response with missing title
        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"UU123\"}}, \"snippet\": {}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Could not parse 'uploads' or 'title'");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldHandleInterruptedException() {
        // Clear any existing interrupt status to avoid interfering with Mockito initialization
        Thread.interrupted();

        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");

        // Interrupt the thread when fetching channels to trigger InterruptedException in delayRequest
        when(channelRepository.findAll()).thenAnswer(inv -> {
            Thread.currentThread().interrupt();
            return List.of(channel);
        });

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class)) {

            try {
                // Use a positive delay to ensure Thread.sleep is called
                Map<String, Object> result = service.processJob(null, null, 10L, null, false, null);

                @SuppressWarnings("unchecked")
                List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
                assertThat(failures).hasSize(1);
                assertThat(failures.get(0).get("reason")).contains("Failed to fetch channel details");

                // Verify thread interrupt status was restored
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
            } finally {
                // Clear interrupt status for subsequent tests
                Thread.interrupted();
            }
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldThrowAuthException_WhenApiKeyInvalid() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "API key not valid"));
                })) {

            assertThatThrownBy(() -> service.processJob(null, null, 100L, null, false, null))
                    .isInstanceOf(YoutubeApiAuthException.class)
                    .hasMessageContaining("The provided YouTube API key is not valid");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldHandleHttpException_When400ButNotAuthError() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "Bad Request"));
                })) {

            Map<String, Object> result = service.processJob(null, null, 100L, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch channel details");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldHandleGenericHttpException() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 500, "Server Error"));
                })) {

            Map<String, Object> result = service.processJob(null, null, 100L, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch channel details");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldCoverPublishedAfterBranches() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        OffsetDateTime t1 = OffsetDateTime.parse("2023-01-01T11:00:00Z");
        OffsetDateTime t2 = t1.plusHours(1);
        OffsetDateTime t3 = t1.plusHours(2);

        Playlist p1 = new Playlist();
        p1.setPlaylistId("uploads1");
        p1.setProcessedAt(t3);
        Playlist p2 = new Playlist();
        p2.setPlaylistId("uploads1");
        p2.setProcessedAt(null);
        Playlist p3 = new Playlist();
        p3.setPlaylistId("uploads1");
        p3.setProcessedAt(t1);
        Playlist p4 = new Playlist();
        p4.setPlaylistId("uploads1");
        p4.setProcessedAt(t3);

        when(playlistRepository.findByPlaylistId("uploads1"))
                .thenReturn(Optional.of(p1))
                .thenReturn(Optional.of(p2))
                .thenReturn(Optional.of(p3))
                .thenReturn(Optional.of(p4));
        //when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByVideoId(anyString())).thenReturn(Optional.empty());

        String chResp = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String vidT2 = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"vT2\"}, \"publishedAt\": \"" + t2 + "\"}}]}";
        String vidT3 = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"vT3\"}, \"publishedAt\": \"" + t3 + "\"}}]}";
        String vidDetailsT2 = "{\"items\": [{\"id\": \"vT2\", \"snippet\": {\"title\": \"V\", \"liveBroadcastContent\": \"none\"}}]}";
        String vidDetailsT3 = "{\"items\": [{\"id\": \"vT3\", \"snippet\": {\"title\": \"V\", \"liveBroadcastContent\": \"none\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any())).thenReturn(new HttpClient.Response(200, chResp));
                    if (context.getCount() == 3) {
                        when(mock.get(eq("/youtube/v3/videos"), any(), any())).thenReturn(new HttpClient.Response(200, vidDetailsT3));
                    } else {
                        when(mock.get(eq("/youtube/v3/videos"), any(), any())).thenReturn(new HttpClient.Response(200, vidDetailsT2));
                    }

                    switch (context.getCount()) {
                        case 1, 2, 4 ->
                            when(mock.get(eq("/youtube/v3/playlistItems"), any(), any())).thenReturn(new HttpClient.Response(200, vidT2));
                        case 3 ->
                            when(mock.get(eq("/youtube/v3/playlistItems"), any(), any())).thenReturn(new HttpClient.Response(200, vidT3));
                    }
                })) {

            // 1. Force=true. Playlist=T3. Request=T1. Video=T2. Effective=T1. T2 > T1 -> Process.
            Map<String, Object> r1 = service.processJob(null, null, 0L, t1, true, null);
            assertThat(r1.get("newItems")).isEqualTo(1);
            // 2. Playlist=null. Request=T1. Video=T2. Effective=T1. T2 > T1 -> Process.
            Map<String, Object> r2 = service.processJob(null, null, 0L, t1, false, null);
            assertThat(r2.get("newItems")).isEqualTo(1);
            // 3. Request > Playlist. Request=T2. Playlist=T1. Video=T3. Effective=T2. T3 > T2 -> Process.
            Map<String, Object> r3 = service.processJob(null, null, 0L, t2, false, null);
            assertThat(r3.get("newItems")).isEqualTo(1);
            // 4. Request < Playlist. Not Forced. Request=T1. Playlist=T3. Video=T2. Effective=T3. T2 < T3 -> Stop.
            Map<String, Object> r4 = service.processJob(null, null, 0L, t1, false, null);
            assertThat(r4.get("newItems")).isEqualTo(0);
            assertThat(mocked.constructed()).hasSize(4);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandlePagination() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        // Page 1: Has next page token
        String page1Response = "{\"nextPageToken\": \"token123\", \"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Page 2: No next page token
        String page2Response = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v2\"}, \"publishedAt\": \"2023-01-01T09:00:00Z\"}}]}";

        String videosResponse1 = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 1\", \"liveBroadcastContent\": \"none\"}}]}";
        String videosResponse2 = "{\"items\": [{\"id\": \"v2\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 2\", \"liveBroadcastContent\": \"none\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));

                    // Mocking consecutive calls for playlistItems
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, page1Response))
                            .thenReturn(new HttpClient.Response(200, page2Response));

                    // Mocking consecutive calls for videos
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse1))
                            .thenReturn(new HttpClient.Response(200, videosResponse2));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("newItems")).isEqualTo(2);
            assertThat(mocked.constructed()).hasSize(1);
            verify(youtubeApiUsageService, times(5)).recordUsage(1L);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleMissingItemsInPlaylistResponse() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String emptyItemsResponse = "{}"; // No items array

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, emptyItemsResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);
            assertThat(result.get("newItems")).isEqualTo(0);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldSkipItemsWithBlankVideoId() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // One item with valid ID, one with blank ID
        String playlistResponse = "{\"items\": ["
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}},"
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}"
                + "]}";

        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 1\", \"liveBroadcastContent\": \"none\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);
            assertThat(result.get("newItems")).isEqualTo(1);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleNonArrayItemsInPlaylistResponse() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String invalidItemsResponse = "{\"items\": \"not-an-array\"}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, invalidItemsResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);
            assertThat(result.get("newItems")).isEqualTo(0);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleException_WhenSaveAllFails() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Mock saveAll to throw RuntimeException
        when(itemRepository.saveAll(anyList())).thenThrow(new RuntimeException("DB Error"));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 1\", \"liveBroadcastContent\": \"none\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // The exception is caught and logged, processing continues.
            // newItems count is incremented before saveAll.
            assertThat(result.get("newItems")).isEqualTo(1);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleInterruptedException_InPlaylistProcessing() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenAnswer(inv -> {
                                throw new InterruptedException("Interrupted");
                            });
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch and process playlist items");
            assertThat(Thread.interrupted()).isTrue();
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleURISyntaxException_InPlaylistProcessing() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenThrow(new URISyntaxException("input", "reason"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch and process playlist items");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldThrowAuthException_InPlaylistProcessing() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "API key not valid"));
                })) {

            assertThatThrownBy(() -> service.processJob(null, null, null, null, false, null))
                    .isInstanceOf(YoutubeApiAuthException.class);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleHttpException_InPlaylistProcessing_NotAuth() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "Bad Request"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch and process playlist items");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldThrowAuthException_InVideoDetailsFetching() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "API key not valid"));
                })) {

            assertThatThrownBy(() -> service.processJob(null, null, null, null, false, null))
                    .isInstanceOf(YoutubeApiAuthException.class);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleHttpException_InVideoDetailsFetching_NotAuth() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "Bad Request"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch video details");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldThrowAuthException_InVideoUpdateChecking() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "API key not valid"));
                })) {

            assertThatThrownBy(() -> service.processJob(null, null, null, null, false, null))
                    .isInstanceOf(YoutubeApiAuthException.class);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleHttpException_InPlaylistProcessing_StatusNot400() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 500, "Server Error"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch and process playlist items");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleHttpException_InVideoDetailsFetching_StatusNot400() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 500, "Server Error"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch video details");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleHttpException_InVideoUpdateChecking_StatusNot400() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 500, "Server Error"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch video details for update check");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleHttpException_InVideoUpdateChecking_NotAuth() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "Bad Request"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch video details for update check");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void processJob_ShouldHandleIOException_InChannelDetailsFetching() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenThrow(new IOException("Network Error"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch channel details");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleIOException_InVideoDetailsFetching() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenThrow(new IOException("Network Error"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch video details");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleIOException_InVideoUpdateChecking() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenThrow(new IOException("Network Error"));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("reason")).contains("Failed to fetch video details for update check");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleMissingItemsInVideosResponse() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // Ensure item is not found so it's treated as new
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Empty items in videos response
        String videosResponse = "{}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // newItems should be 0 because video details fetch failed (returned empty list)
            assertThat(result.get("newItems")).isEqualTo(0);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldSkipUnrequestedVideos_WhenApiReturnsUnexpectedId() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // v1 is new
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // Playlist items has v1
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Videos response has v2 (unexpected)
        String videosResponse = "{\"items\": [{\"id\": \"v2\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 2\", \"liveBroadcastContent\": \"none\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // v2 is skipped because it wasn't in the playlist items map (newVideoSnippets)
            // v1 is not processed because details weren't returned
            assertThat(result.get("newItems")).isEqualTo(0);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleMissingLiveStreamingDetails() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // v1 is new
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Videos response missing liveStreamingDetails
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 1\", \"liveBroadcastContent\": \"none\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // Should process successfully even without liveStreamingDetails
            assertThat(result.get("newItems")).isEqualTo(1);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleLiveStreamingDetailsWithoutScheduledStartTime() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // v1 is new
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Videos response has liveStreamingDetails but no scheduledStartTime
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 1\", \"liveBroadcastContent\": \"live\"}, \"liveStreamingDetails\": {\"actualStartTime\": \"2023-01-02T10:00:00Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // Should process successfully
            assertThat(result.get("newItems")).isEqualTo(1);

            // Verify the created item has no scheduled start time
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Item>> captor = ArgumentCaptor.forClass(List.class);
            verify(itemRepository).saveAll(captor.capture());
            List<Item> savedItems = captor.getValue();
            assertThat(savedItems).hasSize(1);
            assertThat(savedItems.get(0).getScheduledStartTime()).isNull();
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleNonArrayItemsInVideosResponse_WhenUpdating() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Existing item
        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Old Title");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Videos response has items node but it is not an array
        String videosResponse = "{\"items\": \"not-an-array\"}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // Should not update anything, and not throw exception
            assertThat(result.get("updatedItemsCount")).isEqualTo(0);
            assertThat(existingItem.getTitle()).isEqualTo("Old Title");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleNonArrayItemsInVideosResponse_WhenCreating() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // Item not found -> New item path
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Videos response has items node but it is not an array
        String videosResponse = "{\"items\": \"not-an-array\"}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // Should handle gracefully and return 0 new items
            assertThat(result.get("newItems")).isEqualTo(0);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleMissingItemsInVideosResponse_WhenUpdating() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Existing item
        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Old Title");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Videos response missing items node
        String videosResponse = "{}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // Should not update anything, and not throw exception
            assertThat(result.get("updatedItemsCount")).isEqualTo(0);
            assertThat(existingItem.getTitle()).isEqualTo("Old Title");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldSkipUnrequestedVideos_WhenUpdating() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Existing item with videoId "v1"
        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Old Title");
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        // Playlist items has v1
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Videos response has v2 (unexpected) instead of v1
        String videosResponse = "{\"items\": [{\"id\": \"v2\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Video 2\", \"liveBroadcastContent\": \"none\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // v2 is skipped because it wasn't in the existingItemsToUpdate map.
            // v1 is not updated because its details weren't returned.
            assertThat(result.get("updatedItemsCount")).isEqualTo(0);
            // Verify the item was not changed
            assertThat(existingItem.getTitle()).isEqualTo("Old Title");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldUpdateDescription_WhenChanged() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Title");
        existingItem.setDescription("Old Description");
        existingItem.setLiveBroadcastContent(LiveBroadcastContent.NONE);
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Video response with new description
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Title\", \"description\": \"New Description\", \"liveBroadcastContent\": \"none\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("updatedItemsCount")).isEqualTo(1);
            assertThat(existingItem.getDescription()).isEqualTo("New Description");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldUpdateScheduledStartTime_WhenChanged() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Title");
        existingItem.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        existingItem.setScheduledStartTime(OffsetDateTime.parse("2023-01-02T10:00:00Z"));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Video response with new scheduledStartTime
        String newTime = "2023-01-02T12:00:00Z";
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Title\", \"liveBroadcastContent\": \"upcoming\"}, \"liveStreamingDetails\": {\"scheduledStartTime\": \"" + newTime + "\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("updatedItemsCount")).isEqualTo(1);
            assertThat(existingItem.getScheduledStartTime()).isEqualTo(OffsetDateTime.parse(newTime));
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldNotUpdateScheduledStartTime_WhenSame() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String sameTime = "2023-01-02T10:00:00Z";
        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Title");
        existingItem.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        existingItem.setScheduledStartTime(OffsetDateTime.parse(sameTime));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Video response with same scheduledStartTime
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Title\", \"liveBroadcastContent\": \"upcoming\"}, \"liveStreamingDetails\": {\"scheduledStartTime\": \"" + sameTime + "\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("updatedItemsCount")).isEqualTo(0);
            assertThat(existingItem.getScheduledStartTime()).isEqualTo(OffsetDateTime.parse(sameTime));
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleLiveStreamingDetailsWithoutScheduledStartTime_WhenUpdating() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Title");
        existingItem.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        existingItem.setScheduledStartTime(OffsetDateTime.parse("2023-01-02T10:00:00Z"));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Video response has liveStreamingDetails but no scheduledStartTime
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Title\", \"liveBroadcastContent\": \"live\"}, \"liveStreamingDetails\": {\"actualStartTime\": \"2023-01-02T10:00:05Z\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("updatedItemsCount")).isEqualTo(1);
            assertThat(existingItem.getLiveBroadcastContent()).isEqualTo(LiveBroadcastContent.LIVE);
            assertThat(existingItem.getScheduledStartTime()).isEqualTo(OffsetDateTime.parse("2023-01-02T10:00:00Z"));
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldHandleExplicitNullScheduledStartTime_WhenUpdating() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Item existingItem = new Item();
        existingItem.setVideoId("v1");
        existingItem.setTitle("Title");
        existingItem.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        existingItem.setScheduledStartTime(OffsetDateTime.parse("2023-01-02T10:00:00Z"));
        when(itemRepository.findByVideoId("v1")).thenReturn(Optional.of(existingItem));

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";
        String playlistItemsResponse = "{\"items\": [{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}}]}";
        // Video response has liveStreamingDetails with explicit null scheduledStartTime
        String videosResponse = "{\"items\": [{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"Title\", \"liveBroadcastContent\": \"live\"}, \"liveStreamingDetails\": {\"scheduledStartTime\": null}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("updatedItemsCount")).isEqualTo(1);
            assertThat(existingItem.getScheduledStartTime()).isEqualTo(OffsetDateTime.parse("2023-01-02T10:00:00Z"));
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings("null")
    void processJob_ShouldSelectBestThumbnailUrl() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        channel.setTitle("Channel 1");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        when(playlistRepository.findByPlaylistId("uploads1")).thenReturn(Optional.empty());
        when(playlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(itemRepository.findByVideoId(anyString())).thenReturn(Optional.empty());

        String channelResponse = "{\"items\": [{\"contentDetails\": {\"relatedPlaylists\": {\"uploads\": \"uploads1\"}}, \"snippet\": {\"title\": \"Channel 1\"}}]}";

        String playlistItemsResponse = "{\"items\": ["
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v1\"}, \"publishedAt\": \"2023-01-01T10:00:00Z\"}},"
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v2\"}, \"publishedAt\": \"2023-01-01T11:00:00Z\"}},"
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v3\"}, \"publishedAt\": \"2023-01-01T12:00:00Z\"}},"
                + "{\"snippet\": {\"resourceId\": {\"videoId\": \"v4\"}, \"publishedAt\": \"2023-01-01T13:00:00Z\"}}"
                + "]}";

        String videosResponse = "{\"items\": ["
                + // v1: maxres available
                "{\"id\": \"v1\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"V1\", \"liveBroadcastContent\": \"none\", \"thumbnails\": {\"maxres\": {\"url\": \"http://maxres\"}, \"default\": {\"url\": \"http://default\"}}}},"
                + // v2: only medium available
                "{\"id\": \"v2\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"V2\", \"liveBroadcastContent\": \"none\", \"thumbnails\": {\"medium\": {\"url\": \"http://medium\"}}}},"
                + // v3: empty thumbnails object
                "{\"id\": \"v3\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"V3\", \"liveBroadcastContent\": \"none\", \"thumbnails\": {}}},"
                + // v4: thumbnails missing
                "{\"id\": \"v4\", \"kind\": \"youtube#video\", \"snippet\": {\"title\": \"V4\", \"liveBroadcastContent\": \"none\"}}"
                + "]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(eq("/youtube/v3/channels"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, channelResponse));
                    when(mock.get(eq("/youtube/v3/playlistItems"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, playlistItemsResponse));
                    when(mock.get(eq("/youtube/v3/videos"), any(), any()))
                            .thenReturn(new HttpClient.Response(200, videosResponse));
                })) {

            service.processJob(null, null, null, null, false, null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Item>> captor = ArgumentCaptor.forClass(List.class);
            verify(itemRepository).saveAll(captor.capture());

            List<Item> savedItems = captor.getValue();
            assertThat(savedItems.stream().filter(i -> i.getVideoId().equals("v1")).findFirst().orElseThrow().getThumbnailUrl()).isEqualTo("http://maxres");
            assertThat(savedItems.stream().filter(i -> i.getVideoId().equals("v2")).findFirst().orElseThrow().getThumbnailUrl()).isEqualTo("http://medium");
            assertThat(savedItems.stream().filter(i -> i.getVideoId().equals("v3")).findFirst().orElseThrow().getThumbnailUrl()).isNull();
            assertThat(savedItems.stream().filter(i -> i.getVideoId().equals("v4")).findFirst().orElseThrow().getThumbnailUrl()).isNull();
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getBestAvailableThumbnailUrl_ShouldReturnNull_WhenNodeIsNull() {
        Objects.requireNonNull(service);
        String result = ReflectionTestUtils.invokeMethod(service, "getBestAvailableThumbnailUrl", (JsonNode) null);
        assertThat(result).isNull();
    }

    @Test
    @SuppressWarnings({"unchecked", "null"})
    void markAllManuallyDownloaded_ShouldReturnZero_WhenNoItemsFound_AndLogAllChannels() {
        // Capture specification to test the lambda logic when channelIds is null
        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        when(itemRepository.findAll(captor.capture())).thenReturn(Collections.emptyList());

        int count = service.markAllManuallyDownloaded(null);

        assertThat(count).isEqualTo(0);
        verify(itemRepository, never()).saveAll(anyList());

        // Verify Specification logic for null channelIds (should not filter by channel)
        Specification<Item> spec = captor.getValue();
        Root<Item> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        Path<Object> path = mock(Path.class);

        // Use lenient() to avoid strict stubbing errors due to overload ambiguity
        lenient().when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.notEqual(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.and(any(), any())).thenReturn(predicate);
        lenient().when(cb.and(any(), any(), any())).thenReturn(predicate);
        lenient().when(cb.or(any(), any())).thenReturn(predicate);
        lenient().when(cb.lessThan(any(Expression.class), any(OffsetDateTime.class))).thenReturn(predicate);
        when(root.get(anyString())).thenReturn(path);
        when(path.isNotNull()).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        // Should not try to access playlist->channel->channelId because channelIds is null
        verify(root, never()).get("playlist");
    }

    @Test
    @SuppressWarnings("unchecked")
    void markAllManuallyDownloaded_ShouldExecuteSpecificationLogic() {
        // Mocks for Criteria API
        Root<Item> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        Path<Object> path = mock(Path.class);
        Fetch<Object, Object> fetch = mock(Fetch.class);

        // Use lenient() to avoid strict stubbing errors due to overload ambiguity
        lenient().when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.notEqual(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.and(any(), any())).thenReturn(predicate);
        lenient().when(cb.and(any(), any(), any())).thenReturn(predicate);
        lenient().when(cb.or(any(), any())).thenReturn(predicate);
        lenient().when(cb.lessThan(any(Expression.class), any(OffsetDateTime.class))).thenReturn(predicate);

        when(root.get(anyString())).thenReturn(path);
        when(root.fetch(anyString(), any(JoinType.class))).thenReturn(fetch);
        when(path.get(anyString())).thenReturn(path); // For playlist.channel.channelId traversal
        when(path.in(any(java.util.Collection.class))).thenReturn(predicate);
        when(path.isNotNull()).thenReturn(predicate);

        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        when(itemRepository.findAll(captor.capture())).thenReturn(Collections.emptyList());

        service.markAllManuallyDownloaded(List.of("ch1"));
        Specification<Item> spec = captor.getValue();

        // 1. Test fetch logic with Item.class result type (Should fetch)
        doReturn(Item.class).when(query).getResultType();
        spec.toPredicate(root, query, cb);
        verify(root).fetch("playlist", JoinType.LEFT);

        // 2. Test fetch logic with Long.class result type (Should NOT fetch)
        doReturn(Long.class).when(query).getResultType();
        org.mockito.Mockito.clearInvocations(root);
        spec.toPredicate(root, query, cb);
        verify(root, never()).fetch("playlist", JoinType.LEFT);

        // 3. Test fetch logic with primitive long.class result type (Should NOT fetch)
        doReturn(long.class).when(query).getResultType();
        org.mockito.Mockito.clearInvocations(root);
        spec.toPredicate(root, query, cb);
        verify(root, never()).fetch("playlist", JoinType.LEFT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void markAllManuallyDownloaded_ShouldHandleEmptyChannelList_AndNullQuery() {
        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        when(itemRepository.findAll(captor.capture())).thenReturn(Collections.emptyList());

        // 1. Test empty list behavior for logging and specification creation
        int count = service.markAllManuallyDownloaded(Collections.emptyList());
        assertThat(count).isEqualTo(0);

        Specification<Item> spec = captor.getValue();
        Root<Item> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        Path<Object> path = mock(Path.class);

        lenient().when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.notEqual(any(Expression.class), any(Object.class))).thenReturn(predicate);
        lenient().when(cb.and(any(), any())).thenReturn(predicate);
        lenient().when(cb.and(any(), any(), any())).thenReturn(predicate);
        lenient().when(cb.or(any(), any())).thenReturn(predicate);
        lenient().when(cb.lessThan(any(Expression.class), any(OffsetDateTime.class))).thenReturn(predicate);
        when(root.get(anyString())).thenReturn(path);
        when(path.isNotNull()).thenReturn(predicate);

        // 2. Test null query behavior in Specification
        spec.toPredicate(root, null, cb);

        verify(root, never()).fetch(anyString(), any(JoinType.class));
        // channelIds is empty, so it should not filter by playlist/channel
        verify(root, never()).get("playlist");
    }

    @Test
    void verifyNewItems_ShouldReturnEmpty_WhenInputIsNullOrEmpty() {
        Map<String, List<String>> resultNull = service.verifyNewItems(null);
        assertThat(resultNull.get("new")).isEmpty();
        assertThat(resultNull.get("undownloaded")).isEmpty();

        Map<String, List<String>> resultEmpty = service.verifyNewItems(Collections.emptyList());
        assertThat(resultEmpty.get("new")).isEmpty();
        assertThat(resultEmpty.get("undownloaded")).isEmpty();
    }

    @Test
    void verifyNewItems_ShouldHandleManuallyDownloadedAndOtherStatuses() {
        String manualUrl = "https://www.youtube.com/watch?v=manual";
        String failedUrl = "https://www.youtube.com/watch?v=failed";
        String pendingUrl = "https://www.youtube.com/watch?v=pending";

        Item itemManual = new Item();
        itemManual.setVideoId("manual");
        itemManual.setStatus(ProcessingStatus.MANUALLY_DOWNLOADED);

        Item itemFailed = new Item();
        itemFailed.setVideoId("failed");
        itemFailed.setStatus(ProcessingStatus.FAILED);

        Item itemPending = new Item();
        itemPending.setVideoId("pending");
        itemPending.setStatus(ProcessingStatus.PENDING);

        when(itemRepository.findByVideoId("manual")).thenReturn(Optional.of(itemManual));
        when(itemRepository.findByVideoId("failed")).thenReturn(Optional.of(itemFailed));
        when(itemRepository.findByVideoId("pending")).thenReturn(Optional.of(itemPending));

        Map<String, List<String>> result = service.verifyNewItems(List.of(manualUrl, failedUrl, pendingUrl));

        assertThat(result.get("new")).isEmpty();
        assertThat(result.get("undownloaded")).containsExactlyInAnyOrder(failedUrl, pendingUrl);
        assertThat(result.get("undownloaded")).doesNotContain(manualUrl);
    }

    @Test
    void verifyNewItems_ShouldHandleInvalidAndBlankUrls() {
        List<String> urls = new ArrayList<>();
        urls.add(null);
        urls.add("");
        urls.add("   ");
        urls.add("https://not-youtube.com/foo");

        Map<String, List<String>> result = service.verifyNewItems(urls);

        assertThat(result.get("new")).isEmpty();
        assertThat(result.get("undownloaded")).isEmpty();
    }

    @Test
    void verifyNewItems_ShouldHandleUrlResultingInEmptyVideoId() {
        // https://youtu.be results in empty path, which results in empty string from parseVideoIdFromUrl
        String url = "https://youtu.be";
        Map<String, List<String>> result = service.verifyNewItems(List.of(url));

        assertThat(result.get("new")).isEmpty();
        assertThat(result.get("undownloaded")).isEmpty();
    }

    @Test
    void verifyNewItems_ShouldHandleLiveStreamLogic() {
        String urlNullStart = "https://www.youtube.com/watch?v=nullStart";
        String urlFuture = "https://www.youtube.com/watch?v=future";
        String urlPast = "https://www.youtube.com/watch?v=past";

        Item itemNullStart = new Item();
        itemNullStart.setVideoId("nullStart");
        itemNullStart.setStatus(ProcessingStatus.NEW);
        itemNullStart.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        itemNullStart.setScheduledStartTime(null);

        Item itemFuture = new Item();
        itemFuture.setVideoId("future");
        itemFuture.setStatus(ProcessingStatus.NEW);
        itemFuture.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        itemFuture.setScheduledStartTime(OffsetDateTime.now().plusHours(1));

        Item itemPast = new Item();
        itemPast.setVideoId("past");
        itemPast.setStatus(ProcessingStatus.NEW);
        itemPast.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        itemPast.setScheduledStartTime(OffsetDateTime.now().minusHours(1));

        when(itemRepository.findByVideoId("nullStart")).thenReturn(Optional.of(itemNullStart));
        when(itemRepository.findByVideoId("future")).thenReturn(Optional.of(itemFuture));
        when(itemRepository.findByVideoId("past")).thenReturn(Optional.of(itemPast));

        Map<String, List<String>> result = service.verifyNewItems(List.of(urlNullStart, urlFuture, urlPast));

        assertThat(result.get("new")).containsExactly(urlPast);
    }

    @Test
    void verifyNewItems_ShouldHandleUrlParsingEdgeCases_Extended() {
        // Valid cases via fallback parsing
        String urlExactYoutube = "https://youtube.com/embed/vid0";
        String urlSubdomain = "https://m.youtube.com/v/vid1";
        String urlYoutuBe = "https://youtu.be/vid2";
        String urlYoutuBeSub = "https://www.youtu.be/vid3";

        // Invalid cases
        String urlOther = "https://other.com/v/vid4";
        String urlEmptyPath = "https://youtu.be/"; // Path is "/", split returns empty, hits orElse(null)
        String urlException = "http://youtube.com/path with spaces"; // Triggers URISyntaxException
        String urlNoHost = "file:///path/to/file"; // Host is null (or empty/different scheme handling)

        when(itemRepository.findByVideoId("vid0")).thenReturn(Optional.empty());
        when(itemRepository.findByVideoId("vid1")).thenReturn(Optional.empty());
        when(itemRepository.findByVideoId("vid2")).thenReturn(Optional.empty());
        when(itemRepository.findByVideoId("vid3")).thenReturn(Optional.empty());

        Map<String, List<String>> result = service.verifyNewItems(List.of(
                urlExactYoutube, urlSubdomain, urlYoutuBe, urlYoutuBeSub,
                urlOther, urlEmptyPath, urlException, urlNoHost
        ));

        assertThat(result.get("new")).containsExactlyInAnyOrder(
                urlExactYoutube, urlSubdomain, urlYoutuBe, urlYoutuBeSub);
    }

    @Test
    @SuppressWarnings({"null"})
    void downloadItems_ShouldWarn_WhenSomeItemsNotFound() {
        Item item1 = new Item();
        item1.setVideoId("v1");
        item1.setTitle("V1");

        // Request v1 and v2, but only v1 exists
        when(itemRepository.findAllByVideoIdIn(List.of("v1", "v2"))).thenReturn(List.of(item1));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    HttpClient.Response response = new HttpClient.Response(200, "{\"data\": [{\"videoId\": \"v1\", \"taskId\": \"task1\"}]}");
                    when(mock.post(eq("/download"), any(), anyString(), any())).thenReturn(response);
                })) {

            Map<String, Object> result = service.downloadItems(List.of("v1", "v2"), "default", null);

            assertThat(result.get("createdTasks")).isEqualTo(1);
            verify(downloadInfoRepository).saveAll(anyList());
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void downloadItems_ShouldHandleNullConfigName() {
        Item item = new Item();
        item.setVideoId("v1");
        when(itemRepository.findAllByVideoIdIn(List.of("v1"))).thenReturn(List.of(item));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    HttpClient.Response response = new HttpClient.Response(200, "{\"data\": []}");
                    when(mock.post(eq("/download"), any(), anyString(), any())).thenReturn(response);
                })) {

            service.downloadItems(List.of("v1"), null, null);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void verifyNewItems_ShouldHandleUrlWithEmptyVParameter() {
        // This targets the check inside parseVideoIdFromUrl: if (videoId != null && !videoId.isBlank())
        // when 'v' parameter is present but empty.
        String url = "https://www.youtube.com/watch?v=";

        // When v is empty, it falls back to path parsing.
        // For https://www.youtube.com/watch?v=, path is /watch.
        // split("/") -> ["", "watch"]. reduce -> "watch".
        when(itemRepository.findByVideoId("watch")).thenReturn(Optional.empty());

        Map<String, List<String>> result = service.verifyNewItems(List.of(url));

        // It will be identified as "new" with videoId="watch" because "watch" is not in DB
        assertThat(result.get("new")).contains(url);
    }

    @Test
    @SuppressWarnings({"null"})
    void downloadItems_ShouldHandleTrailingSlashInUrl() {
        ReflectionTestUtils.setField(service, "downloaderServiceUrl", "http://localhost:8081/");
        Item item = new Item();
        item.setVideoId("v1");
        when(itemRepository.findAllByVideoIdIn(List.of("v1"))).thenReturn(List.of(item));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    HttpClient.Response response = new HttpClient.Response(200, "{\"data\": [{\"videoId\": \"v1\", \"taskId\": \"task1\"}]}");
                    when(mock.post(eq("/download"), any(), anyString(), any())).thenReturn(response);
                })) {

            service.downloadItems(List.of("v1"), "default", null);
            verify(downloadInfoRepository).saveAll(anyList());
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void downloadItems_ShouldIncludeAuthHeader() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        when(itemRepository.findAllByVideoIdIn(List.of("v1"))).thenReturn(List.of(item));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    HttpClient.Response response = new HttpClient.Response(200, "{\"data\": []}");
                    when(mock.post(eq("/download"), any(), anyString(), anyMap())).thenReturn(response);
                })) {

            service.downloadItems(List.of("v1"), "default", "Bearer token");

            HttpClient client = mocked.constructed().get(0);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
            verify(client).post(eq("/download"), any(), anyString(), headersCaptor.capture());
            assertThat(headersCaptor.getValue()).containsEntry("Authorization", "Bearer token");
        }
    }

    @Test
    void downloadItems_ShouldThrow_WhenResponseDataInvalid() {
        Item item = new Item();
        item.setVideoId("v1");
        when(itemRepository.findAllByVideoIdIn(List.of("v1"))).thenReturn(List.of(item));

        // Case 1: Missing data node
        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.post(eq("/download"), any(), anyString(), any())).thenReturn(new HttpClient.Response(200, "{}"));
                })) {
            assertThatThrownBy(() -> service.downloadItems(List.of("v1"), "default", null))
                    .isInstanceOf(YoutubeApiRequestException.class)
                    .hasMessageContaining("Downloader response did not contain a valid 'data' array");
            assertThat(mocked.constructed()).hasSize(1);
        }

        // Case 2: Data node is not an array
        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.post(eq("/download"), any(), anyString(), any())).thenReturn(new HttpClient.Response(200, "{\"data\": \"string\"}"));
                })) {
            assertThatThrownBy(() -> service.downloadItems(List.of("v1"), "default", null))
                    .isInstanceOf(YoutubeApiRequestException.class)
                    .hasMessageContaining("Downloader response did not contain a valid 'data' array");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings({"null", "unchecked"})
    void downloadItems_ShouldSkipInvalidTasks() {
        Item item = new Item();
        item.setVideoId("v1");
        when(itemRepository.findAllByVideoIdIn(List.of("v1"))).thenReturn(List.of(item));

        String responseBody = "{\"data\": ["
                + "{\"videoId\": \"unknown\", \"taskId\": \"t1\"},"
                + // Item null (not in map)
                "{\"videoId\": \"v1\", \"taskId\": null},"
                + // TaskId null
                "{\"videoId\": \"v1\", \"taskId\": \"\"},"
                + // TaskId blank
                "{\"videoId\": \"v1\", \"taskId\": \"valid\"}"
                + // Valid
                "]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.post(eq("/download"), any(), anyString(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            service.downloadItems(List.of("v1"), "default", null);

            ArgumentCaptor<List<DownloadInfo>> captor = ArgumentCaptor.forClass(List.class);
            verify(downloadInfoRepository).saveAll(captor.capture());

            List<DownloadInfo> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getDownloadTaskId()).isEqualTo("valid");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void downloadItems_ShouldHandleExceptions() {
        Item item = new Item();
        item.setVideoId("v1");
        when(itemRepository.findAllByVideoIdIn(List.of("v1"))).thenReturn(List.of(item));

        // Case 1: IOException
        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.post(eq("/download"), any(), anyString(), any())).thenThrow(new IOException("Network error"));
                })) {
            assertThatThrownBy(() -> service.downloadItems(List.of("v1"), "default", null))
                    .isInstanceOf(YoutubeApiRequestException.class)
                    .hasMessageContaining("Failed to call downloader service");
            assertThat(mocked.constructed()).hasSize(1);
        }

        // Case 2: URISyntaxException
        Objects.requireNonNull(service);
        ReflectionTestUtils.setField(service, "downloaderServiceUrl", "http://invalid^host");

        assertThatThrownBy(() -> service.downloadItems(List.of("v1"), "default", null))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to call downloader service");
    }

    @Test
    void downloadItems_ShouldNotIncludeAuthHeader_WhenBlank() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        when(itemRepository.findAllByVideoIdIn(List.of("v1"))).thenReturn(List.of(item));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    HttpClient.Response response = new HttpClient.Response(200, "{\"data\": []}");
                    when(mock.post(eq("/download"), any(), anyString(), anyMap())).thenReturn(response);
                })) {

            service.downloadItems(List.of("v1"), "default", "   ");

            HttpClient client = mocked.constructed().get(0);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> headersCaptor = ArgumentCaptor.forClass(Map.class);
            verify(client).post(eq("/download"), any(), anyString(), headersCaptor.capture());
            assertThat(headersCaptor.getValue()).doesNotContainKey("Authorization");
        }
    }
}
