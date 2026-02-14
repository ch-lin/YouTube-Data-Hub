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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.platform.http.HttpClient;
import ch.lin.youtube.hub.backend.api.app.repository.ChannelRepository;
import ch.lin.youtube.hub.backend.api.app.repository.DownloadInfoRepository;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.PlaylistRepository;
import ch.lin.youtube.hub.backend.api.app.repository.TagRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.PlaylistProcessingResult;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
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
    private ChannelProcessingService channelProcessingService;

    private YoutubeHubServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new YoutubeHubServiceImpl(channelRepository, itemRepository, playlistRepository, tagRepository,
                downloadInfoRepository, configsService, channelProcessingService);
        ReflectionTestUtils.setField(service, "downloaderServiceUrl", "http://localhost:8081");
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
    void verifyNewItems_ShouldReturnEmptyMaps_WhenUrlsIsNull() {
        Map<String, List<String>> result = service.verifyNewItems(null);
        assertThat(result.get("new")).isEmpty();
        assertThat(result.get("undownloaded")).isEmpty();
    }

    @Test
    void verifyNewItems_ShouldReturnEmptyMaps_WhenUrlsIsEmpty() {
        Map<String, List<String>> result = service.verifyNewItems(Collections.emptyList());
        assertThat(result.get("new")).isEmpty();
        assertThat(result.get("undownloaded")).isEmpty();
    }

    @Test
    void verifyNewItems_ShouldSkipInvalidUrls() {
        // 測試無法解析 videoId 的 URL (非 YouTube 域名)
        String invalidUrl = "https://google.com";

        Map<String, List<String>> result = service.verifyNewItems(List.of(invalidUrl));

        assertThat(result.get("new")).isEmpty();
        assertThat(result.get("undownloaded")).isEmpty();
    }

    @Test
    void verifyNewItems_ShouldExcludeManuallyDownloadedItems() {
        // 測試狀態為 MANUALLY_DOWNLOADED 的項目不應包含在 undownloaded 列表中
        String url = "https://www.youtube.com/watch?v=manual";
        Item item = new Item();
        item.setVideoId("manual");
        item.setStatus(ProcessingStatus.MANUALLY_DOWNLOADED);

        when(itemRepository.findByVideoId("manual")).thenReturn(Optional.of(item));

        Map<String, List<String>> result = service.verifyNewItems(List.of(url));

        assertThat(result.get("new")).isEmpty();
        assertThat(result.get("undownloaded")).isEmpty();
    }

    @Test
    void verifyNewItems_ShouldHandleBlankVideoIdFromUrl() {
        // 測試解析出空白 videoId 的情況 (例如 URL 路徑解碼後為空白)
        // https://youtu.be/%20 -> path is "/ ", split gives ["", " "], reduce gives " "
        String blankIdUrl = "https://youtu.be/%20";

        Map<String, List<String>> result = service.verifyNewItems(List.of(blankIdUrl));

        assertThat(result.get("new")).isEmpty();
        assertThat(result.get("undownloaded")).isEmpty();
    }

    @Test
    void verifyNewItems_ShouldHandleVariousLiveStreamScenarios() {
        String pastLiveUrl = "https://www.youtube.com/watch?v=pastLive";
        String nullTimeLiveUrl = "https://www.youtube.com/watch?v=nullTimeLive";

        // Case 1: Past Live Stream (Should be processable)
        // Covers branch: scheduledStartTime != null AND isBefore(now) is true
        Item itemPastLive = new Item();
        itemPastLive.setVideoId("pastLive");
        itemPastLive.setStatus(ProcessingStatus.NEW);
        itemPastLive.setLiveBroadcastContent(LiveBroadcastContent.LIVE);
        itemPastLive.setScheduledStartTime(OffsetDateTime.now().minusHours(1));

        // Case 2: Live Stream with Null Start Time (Should NOT be processable)
        // Covers branch: scheduledStartTime == null
        Item itemNullTimeLive = new Item();
        itemNullTimeLive.setVideoId("nullTimeLive");
        itemNullTimeLive.setStatus(ProcessingStatus.NEW);
        itemNullTimeLive.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        itemNullTimeLive.setScheduledStartTime(null);

        when(itemRepository.findByVideoId("pastLive")).thenReturn(Optional.of(itemPastLive));
        when(itemRepository.findByVideoId("nullTimeLive")).thenReturn(Optional.of(itemNullTimeLive));

        Map<String, List<String>> result = service.verifyNewItems(List.of(pastLiveUrl, nullTimeLiveUrl));

        assertThat(result.get("new")).contains(pastLiveUrl);
        assertThat(result.get("new")).doesNotContain(nullTimeLiveUrl);
        assertThat(result.get("undownloaded")).contains(pastLiveUrl, nullTimeLiveUrl);
    }

    @Test
    void verifyNewItems_ShouldHandleParsingEdgeCases() {
        // 1. Null/Blank URLs
        List<String> urls = new java.util.ArrayList<>();
        urls.add(null);
        urls.add("");
        urls.add("   ");

        // 2. Host logic variations
        // Host == null (opaque URI)
        urls.add("mailto:user@example.com");

        // Allowed hosts variations (without 'v' param to force host check)
        String validShorts = "https://www.youtube.com/shorts/s1";
        String validEmbed = "https://youtube.com/embed/e1";
        String validYoutuBe = "https://youtu.be/y1";
        String validSubdomainYoutuBe = "https://www.youtu.be/sy1";

        urls.add(validShorts);
        urls.add(validEmbed);
        urls.add(validYoutuBe);
        urls.add(validSubdomainYoutuBe);

        // 3. URISyntaxException trigger
        // Space in URL causes URISyntaxException in new URI(String) constructor
        String uriSyntaxExUrl = "https://www.youtube.com/shorts/id with space";
        urls.add(uriSyntaxExUrl);

        // Mock repository responses for valid IDs
        when(itemRepository.findByVideoId("s1")).thenReturn(Optional.empty());
        when(itemRepository.findByVideoId("e1")).thenReturn(Optional.empty());
        when(itemRepository.findByVideoId("y1")).thenReturn(Optional.empty());
        when(itemRepository.findByVideoId("sy1")).thenReturn(Optional.empty());

        Map<String, List<String>> result = service.verifyNewItems(urls);

        assertThat(result.get("new")).containsExactlyInAnyOrder(
                validShorts, validEmbed, validYoutuBe, validSubdomainYoutuBe
        );
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
    @SuppressWarnings({"null", "unchecked"})
    void markAllManuallyDownloaded_ShouldReturnZero_WhenNoItemsFound() {
        // 測試情境：資料庫中沒有符合條件的項目
        when(itemRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        int count = service.markAllManuallyDownloaded(List.of("ch1"));

        assertThat(count).isEqualTo(0);
        // 驗證不應呼叫 saveAll，因為沒有項目需要更新
        verify(itemRepository, never()).saveAll(anyList());
    }

    @Test
    @SuppressWarnings({"null", "unchecked"})
    void markAllManuallyDownloaded_ShouldHandleNullChannelIds() {
        // 測試情境：channelIds 為 null (表示針對所有頻道)
        Item item = new Item();
        item.setStatus(ProcessingStatus.NEW);
        when(itemRepository.findAll(any(Specification.class))).thenReturn(List.of(item));

        int count = service.markAllManuallyDownloaded(null);

        assertThat(count).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo(ProcessingStatus.MANUALLY_DOWNLOADED);
        verify(itemRepository).saveAll(anyList());
    }

    @Test
    @SuppressWarnings({"null", "unchecked"})
    void markAllManuallyDownloaded_ShouldHandleEmptyChannelIds() {
        // 測試情境：channelIds 為空列表 (表示針對所有頻道)
        Item item = new Item();
        item.setStatus(ProcessingStatus.NEW);
        when(itemRepository.findAll(any(Specification.class))).thenReturn(List.of(item));

        int count = service.markAllManuallyDownloaded(Collections.emptyList());

        assertThat(count).isEqualTo(1);
        assertThat(item.getStatus()).isEqualTo(ProcessingStatus.MANUALLY_DOWNLOADED);
        verify(itemRepository).saveAll(anyList());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void markAllManuallyDownloaded_ShouldConstructCorrectSpecification() {
        // Arrange
        List<String> channelIds = List.of("ch1");
        when(itemRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        // Act
        service.markAllManuallyDownloaded(channelIds);

        // Assert
        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(itemRepository).findAll(captor.capture());
        Specification<Item> spec = captor.getValue();

        // Mock Criteria API
        Root<Item> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        Path path = org.mockito.Mockito.mock(Path.class);
        Fetch<Object, Object> fetch = org.mockito.Mockito.mock(Fetch.class);

        // Setup mocks
        when(cb.equal(any(), any(Object.class))).thenReturn(predicate);
        when(cb.notEqual(any(), any(Object.class))).thenReturn(predicate);
        when(cb.and(any(), any())).thenReturn(predicate);
        when(cb.and(any(), any(), any())).thenReturn(predicate);
        when(cb.or(any(), any())).thenReturn(predicate);
        when(cb.lessThan(any(), any(OffsetDateTime.class))).thenReturn(predicate);

        when(root.get(anyString())).thenReturn(path);
        when(root.fetch(anyString(), any(JoinType.class))).thenReturn(fetch);

        // Handle nested path for channelIds: root.get("playlist").get("channel").get("channelId")
        when(path.get(anyString())).thenReturn(path);
        when(path.in(any(java.util.Collection.class))).thenReturn(predicate);
        when(path.isNotNull()).thenReturn(predicate);

        // Handle query result type for fetch check
        doReturn(Item.class).when(query).getResultType();

        // Execute Specification logic
        spec.toPredicate(root, query, cb);

        // Verify interactions
        // 1. Fetch playlist
        verify(root).fetch("playlist", JoinType.LEFT);

        // 2. Status = NEW
        verify(root).get("status");
        verify(cb).equal(path, ProcessingStatus.NEW);

        // 3. LiveBroadcastContent checks
        verify(root, org.mockito.Mockito.atLeastOnce()).get("liveBroadcastContent");
        verify(cb).equal(path, LiveBroadcastContent.NONE); // isStandardVideo
        verify(cb).notEqual(path, LiveBroadcastContent.NONE); // isProcessableLiveStream part 1

        // 4. ScheduledStartTime checks
        verify(root, org.mockito.Mockito.atLeastOnce()).get("scheduledStartTime");
        verify(path).isNotNull();
        verify(cb).lessThan(eq(path), any(OffsetDateTime.class));

        // 5. Channel IDs check
        verify(root).get("playlist");
        verify(path).in(channelIds);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void markAllManuallyDownloaded_Specification_ShouldSkipFetch_WhenQueryIsCount() {
        // Arrange
        when(itemRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        service.markAllManuallyDownloaded(null);

        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(itemRepository).findAll(captor.capture());
        Specification<Item> spec = captor.getValue();

        Root<Item> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        Path path = org.mockito.Mockito.mock(Path.class);

        when(cb.equal(any(), any(Object.class))).thenReturn(predicate);
        when(root.get(anyString())).thenReturn(path);

        // Count query
        doReturn(Long.class).when(query).getResultType();

        spec.toPredicate(root, query, cb);

        // Verify fetch is NOT called
        verify(root, never()).fetch(anyString(), any(JoinType.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void markAllManuallyDownloaded_Specification_ShouldSkipFetch_WhenQueryIsNull() {
        // Arrange
        when(itemRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        service.markAllManuallyDownloaded(List.of("ch1"));

        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(itemRepository).findAll(captor.capture());
        Specification<Item> spec = captor.getValue();

        Root<Item> root = org.mockito.Mockito.mock(Root.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        Path path = org.mockito.Mockito.mock(Path.class);

        // Setup mocks for predicates and paths to avoid NPEs during Specification execution
        when(cb.equal(any(), any(Object.class))).thenReturn(predicate);
        when(cb.notEqual(any(), any(Object.class))).thenReturn(predicate);
        when(cb.and(any(), any())).thenReturn(predicate);
        when(cb.and(any(), any(), any())).thenReturn(predicate);
        when(cb.or(any(), any())).thenReturn(predicate);
        when(cb.lessThan(any(), any(OffsetDateTime.class))).thenReturn(predicate);

        when(root.get(anyString())).thenReturn(path);
        when(path.get(anyString())).thenReturn(path); // Allow chaining .get().get()
        when(path.isNotNull()).thenReturn(predicate);
        when(path.in(any(java.util.Collection.class))).thenReturn(predicate);

        // Act: query is null
        spec.toPredicate(root, null, cb);

        // Assert: fetch is NOT called
        verify(root, never()).fetch(anyString(), any(JoinType.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void markAllManuallyDownloaded_Specification_ShouldSkipFetch_WhenQueryIsPrimitiveLong() {
        // Arrange
        when(itemRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        service.markAllManuallyDownloaded(List.of("ch1"));

        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(itemRepository).findAll(captor.capture());
        Specification<Item> spec = captor.getValue();

        Root<Item> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        Path path = org.mockito.Mockito.mock(Path.class);

        // Setup mocks
        when(cb.equal(any(), any(Object.class))).thenReturn(predicate);
        when(cb.notEqual(any(), any(Object.class))).thenReturn(predicate);
        when(cb.and(any(), any())).thenReturn(predicate);
        when(cb.and(any(), any(), any())).thenReturn(predicate);
        when(cb.or(any(), any())).thenReturn(predicate);
        when(cb.lessThan(any(), any(OffsetDateTime.class))).thenReturn(predicate);

        when(root.get(anyString())).thenReturn(path);
        when(path.get(anyString())).thenReturn(path); // Allow chaining
        when(path.isNotNull()).thenReturn(predicate);
        when(path.in(any(java.util.Collection.class))).thenReturn(predicate);

        // Act: query result type is long.class
        doReturn(long.class).when(query).getResultType();

        spec.toPredicate(root, query, cb);

        // Assert: fetch is NOT called
        verify(root, never()).fetch(anyString(), any(JoinType.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void markAllManuallyDownloaded_Specification_ShouldSkipChannelFilter_WhenChannelIdsEmpty() {
        // Arrange
        when(itemRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());
        // Pass empty list
        service.markAllManuallyDownloaded(Collections.emptyList());

        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(itemRepository).findAll(captor.capture());
        Specification<Item> spec = captor.getValue();

        Root<Item> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        Path path = org.mockito.Mockito.mock(Path.class);

        when(cb.equal(any(), any(Object.class))).thenReturn(predicate);
        when(root.get(anyString())).thenReturn(path);
        doReturn(Item.class).when(query).getResultType();

        // Act
        spec.toPredicate(root, query, cb);

        // Assert
        // Verify channel filter is NOT constructed.
        verify(root, never()).get("playlist");
    }

    @Test
    void processJob_ShouldThrow_WhenNoApiKey() {
        when(configsService.getResolvedConfig(null)).thenReturn(new HubConfig());

        assertThatThrownBy(() -> service.processJob(null, null, null, null, false, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("A YouTube API key is required");
    }

    @Test
    void processJob_ShouldProcessChannels_AndAggregateResults() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel1 = new Channel();
        channel1.setChannelId("ch1");
        Channel channel2 = new Channel();
        channel2.setChannelId("ch2");
        when(channelRepository.findAll()).thenReturn(List.of(channel1, channel2));

        Playlist playlist1 = new Playlist();
        playlist1.setPlaylistId("pl1");
        Playlist playlist2 = new Playlist();
        playlist2.setPlaylistId("pl2");

        PlaylistProcessingResult result1 = new PlaylistProcessingResult();
        result1.setNewItemsCount(2);
        result1.setStandardVideoCount(1);
        result1.setUpcomingVideoCount(1);

        PlaylistProcessingResult result2 = new PlaylistProcessingResult();
        result2.setNewItemsCount(3);
        result2.setLiveVideoCount(3);
        result2.setUpdatedItemsCount(1);

        when(channelProcessingService.prepareChannelAndPlaylist(eq(channel1), any(), anyString(), anyLong(), anyLong(), anyLong())).thenReturn(playlist1);
        when(channelProcessingService.prepareChannelAndPlaylist(eq(channel2), any(), anyString(), anyLong(), anyLong(), anyLong())).thenReturn(playlist2);

        when(channelProcessingService.processPlaylistItems(eq(playlist1), any(), anyString(), any(), eq(false), anyLong(), anyLong(), anyLong())).thenReturn(result1);
        when(channelProcessingService.processPlaylistItems(eq(playlist2), any(), anyString(), any(), eq(false), anyLong(), anyLong(), anyLong())).thenReturn(result2);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class)) {
            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("processedChannels")).isEqualTo(2);
            assertThat(result.get("newItems")).isEqualTo(5);
            assertThat(result.get("standardVideoCount")).isEqualTo(1);
            assertThat(result.get("upcomingVideoCount")).isEqualTo(1);
            assertThat(result.get("liveVideoCount")).isEqualTo(3);
            assertThat(result.get("updatedItemsCount")).isEqualTo(1);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    @SuppressWarnings({"unused"})
    void processJob_ShouldStopEarly_WhenQuotaExceeded() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel1 = new Channel();
        channel1.setChannelId("ch1");
        Channel channel2 = new Channel();
        channel2.setChannelId("ch2");
        when(channelRepository.findAll()).thenReturn(List.of(channel1, channel2));

        when(channelProcessingService.prepareChannelAndPlaylist(eq(channel1), any(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenThrow(new QuotaExceededException("Quota limit reached"));

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class)) {
            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            // Should stop after first channel, so 0 processed successfully
            assertThat(result.get("processedChannels")).isEqualTo(0);
            // Verify second channel was NOT processed
            verify(channelProcessingService, never()).prepareChannelAndPlaylist(eq(channel2), any(), anyString(), anyLong(), anyLong(), anyLong());
        }
    }

    @Test
    @SuppressWarnings({"unused"})
    void processJob_ShouldHandleChannelFailure_AndContinue() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("test-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        Channel channel1 = new Channel();
        channel1.setChannelId("ch1");
        channel1.setTitle("Channel 1");
        Channel channel2 = new Channel();
        channel2.setChannelId("ch2");
        when(channelRepository.findAll()).thenReturn(List.of(channel1, channel2));

        Playlist playlist2 = new Playlist();
        playlist2.setPlaylistId("pl2");

        when(channelProcessingService.prepareChannelAndPlaylist(eq(channel1), any(), anyString(), anyLong(), anyLong(), anyLong()))
                .thenThrow(new YoutubeApiRequestException("API Error"));
        when(channelProcessingService.prepareChannelAndPlaylist(eq(channel2), any(), anyString(), anyLong(), anyLong(), anyLong())).thenReturn(playlist2);
        when(channelProcessingService.processPlaylistItems(eq(playlist2), any(), anyString(), any(), eq(false), anyLong(), anyLong(), anyLong()))
                .thenReturn(new PlaylistProcessingResult());

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class)) {
            Map<String, Object> result = service.processJob(null, null, null, null, false, null);

            assertThat(result.get("processedChannels")).isEqualTo(1);
            @SuppressWarnings("unchecked")
            List<Map<String, String>> failures = (List<Map<String, String>>) result.get("failures");
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0).get("channelId")).isEqualTo("ch1");
            assertThat(failures.get(0).get("reason")).contains("API Error");
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
        // ReflectionTestUtils.setField(service, "downloaderServiceUrl", "http://invalid^host");
        // Note: URI constructor throws URISyntaxException, but it's hard to trigger with just setField if the string is valid until used.
        // We can simulate it by mocking URI construction or just rely on IOException coverage.
        // Actually, "http://invalid^host" will throw URISyntaxException in URI constructor.
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
