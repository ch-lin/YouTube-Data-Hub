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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.lin.platform.http.HttpClient;
import ch.lin.platform.http.exception.HttpException;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.common.exception.QuotaExceededException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.LiveBroadcastContent;

@ExtendWith(MockitoExtension.class)
class VideoFetchServiceImplTest {

    @Mock
    private YoutubeApiUsageService youtubeApiUsageService;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private HttpClient httpClient;

    private VideoFetchServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new VideoFetchServiceImpl(youtubeApiUsageService, itemRepository);
        // Default lenient behavior for quota check
        lenient().when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong())).thenReturn(true);
    }

    @Test
    void fetchAndCreateItems_ShouldReturnEmpty_WhenSnippetsEmpty() throws Exception {
        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", Collections.emptyMap(), 0, 100, 10);
        assertThat(result).isEmpty();
        verify(httpClient, never()).get(any(), any(), any());
    }

    @Test
    void fetchAndCreateItems_ShouldThrow_WhenQuotaExceeded() {
        when(youtubeApiUsageService.hasSufficientQuota(anyLong(), anyLong())).thenReturn(false);
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());

        assertThatThrownBy(() -> service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10))
                .isInstanceOf(QuotaExceededException.class);
    }

    @Test
    void fetchAndCreateItems_ShouldCreateItems_WhenResponseIsValid() throws Exception {
        String videoId = "v1";
        String publishedAt = "2023-01-01T12:00:00Z";
        ObjectNode snippetNode = objectMapper.createObjectNode();
        snippetNode.put("publishedAt", publishedAt);
        Map<String, JsonNode> snippets = Map.of(videoId, snippetNode);

        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "kind": "youtube#video",
                        "snippet": {
                            "title": "Test Video",
                            "description": "Description",
                            "liveBroadcastContent": "none",
                            "thumbnails": {
                                "high": { "url": "http://thumb.jpg" }
                            }
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).hasSize(1);
        Item item = result.get(0);
        assertThat(item.getVideoId()).isEqualTo("v1");
        assertThat(item.getTitle()).isEqualTo("Test Video");
        assertThat(item.getDescription()).isEqualTo("Description");
        assertThat(item.getKind()).isEqualTo("youtube#video");
        assertThat(item.getVideoPublishedAt()).isEqualTo(OffsetDateTime.parse(publishedAt));
        assertThat(item.getLiveBroadcastContent()).isEqualTo(LiveBroadcastContent.NONE);
        assertThat(item.getThumbnailUrl()).isEqualTo("http://thumb.jpg");
        verify(youtubeApiUsageService).recordUsage(1L);
    }

    @Test
    void fetchAndCreateItems_ShouldHandleLiveStreamingDetails() throws Exception {
        String videoId = "v1";
        ObjectNode snippetNode = objectMapper.createObjectNode();
        snippetNode.put("publishedAt", "2023-01-01T12:00:00Z");
        Map<String, JsonNode> snippets = Map.of(videoId, snippetNode);

        String scheduledTime = "2023-01-02T10:00:00Z";
        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "snippet": {
                            "title": "Live Video",
                            "liveBroadcastContent": "upcoming"
                        },
                        "liveStreamingDetails": {
                            "scheduledStartTime": "%s"
                        }
                    }
                ]
            }
            """.formatted(scheduledTime);
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduledStartTime()).isEqualTo(OffsetDateTime.parse(scheduledTime));
        assertThat(result.get(0).getLiveBroadcastContent()).isEqualTo(LiveBroadcastContent.UPCOMING);
    }

    @Test
    void fetchAndCreateItems_ShouldIgnoreItems_WhenSnippetMissingInInputMap() throws Exception {
        // Input map has v1, but API returns v1 and v2. v2 should be ignored because we don't have its original snippet (publishedAt).
        String videoId = "v1";
        ObjectNode snippetNode = objectMapper.createObjectNode();
        snippetNode.put("publishedAt", "2023-01-01T12:00:00Z");
        Map<String, JsonNode> snippets = Map.of(videoId, snippetNode);

        String responseBody = """
            {
                "items": [
                    { "id": "v1", "snippet": { "title": "V1", "liveBroadcastContent": "none" } },
                    { "id": "v2", "snippet": { "title": "V2", "liveBroadcastContent": "none" } }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVideoId()).isEqualTo("v1");
    }

    @Test
    void fetchAndCreateItems_ShouldReturnEmpty_WhenApiResponseHasNoItems() throws Exception {
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());
        String responseBody = "{}";
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchAndCreateItems_ShouldThrowAuthException_WhenApiKeyInvalid() throws Exception {
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new HttpException("GET", 400, "API key not valid"));

        assertThatThrownBy(() -> service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10))
                .isInstanceOf(YoutubeApiAuthException.class);
    }

    @Test
    void fetchAndCreateItems_ShouldThrowRequestException_WhenIoException() throws Exception {
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new IOException("Network error"));

        assertThatThrownBy(() -> service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10))
                .isInstanceOf(YoutubeApiRequestException.class);
    }

    @Test
    void updateExistingItems_ShouldReturnZero_WhenListEmpty() throws Exception {
        int count = service.updateExistingItems(httpClient, "key", Collections.emptyList(), 0, 100, 10);
        assertThat(count).isEqualTo(0);
        verify(httpClient, never()).get(any(), any(), any());
    }

    @Test
    void updateExistingItems_ShouldUpdate_WhenFieldsChanged() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        item.setTitle("Old Title");
        item.setDescription("Old Desc");
        item.setLiveBroadcastContent(LiveBroadcastContent.NONE);

        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "snippet": {
                            "title": "New Title",
                            "description": "New Desc",
                            "liveBroadcastContent": "live"
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(1);
        assertThat(item.getTitle()).isEqualTo("New Title");
        assertThat(item.getDescription()).isEqualTo("New Desc");
        assertThat(item.getLiveBroadcastContent()).isEqualTo(LiveBroadcastContent.LIVE);
        verify(itemRepository).save(item);
    }

    @Test
    void updateExistingItems_ShouldUpdateScheduledTime_WhenChanged() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        item.setTitle("Title");
        item.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        item.setScheduledStartTime(OffsetDateTime.parse("2023-01-01T10:00:00Z"));

        String newTime = "2023-01-01T12:00:00Z";
        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "upcoming"
                        },
                        "liveStreamingDetails": {
                            "scheduledStartTime": "%s"
                        }
                    }
                ]
            }
            """.formatted(newTime);
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(1);
        assertThat(item.getScheduledStartTime()).isEqualTo(OffsetDateTime.parse(newTime));
        verify(itemRepository).save(item);
    }

    @Test
    void updateExistingItems_ShouldNotUpdate_WhenFieldsSame() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        item.setTitle("Title");
        item.setLiveBroadcastContent(LiveBroadcastContent.NONE);

        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "none"
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(0);
        verify(itemRepository, never()).save(item);
    }

    @Test
    void updateExistingItems_ShouldHandleMissingItemsInResponse() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");

        String responseBody = "{}";
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void fetchAndCreateItems_ShouldHandleLiveStreamingDetails_WithoutScheduledStartTime() throws Exception {
        String videoId = "v1";
        ObjectNode snippetNode = objectMapper.createObjectNode();
        snippetNode.put("publishedAt", "2023-01-01T12:00:00Z");
        Map<String, JsonNode> snippets = Map.of(videoId, snippetNode);

        // liveStreamingDetails exists but lacks scheduledStartTime
        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "kind": "youtube#video",
                        "snippet": {
                            "title": "Live Video",
                            "liveBroadcastContent": "live"
                        },
                        "liveStreamingDetails": {
                            "actualStartTime": "2023-01-02T10:00:00Z"
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduledStartTime()).isNull();
    }

    @Test
    void fetchAndCreateItems_ShouldReturnEmpty_WhenItemsIsNotArray() throws Exception {
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());
        String responseBody = "{\"items\": \"not-an-array\"}";
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchAndCreateItems_ShouldThrowRequestException_WhenURISyntaxException() throws Exception {
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new URISyntaxException("input", "reason"));

        assertThatThrownBy(() -> service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10))
                .isInstanceOf(YoutubeApiRequestException.class);
    }

    @Test
    void fetchAndCreateItems_ShouldThrowRequestException_WhenHttpException400ButNotAuth() throws Exception {
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new HttpException("GET", 400, "Bad Request"));

        assertThatThrownBy(() -> service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch video details");
    }

    @Test
    void fetchAndCreateItems_ShouldThrowRequestException_WhenHttpException500() throws Exception {
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new HttpException("GET", 500, "Server Error"));

        assertThatThrownBy(() -> service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch video details");
    }

    @Test
    void updateExistingItems_ShouldThrowAuthException_WhenApiKeyInvalid() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new HttpException("GET", 400, "API key not valid"));

        assertThatThrownBy(() -> service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10))
                .isInstanceOf(YoutubeApiAuthException.class);
    }

    @Test
    void updateExistingItems_ShouldThrowRequestException_WhenIoException() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new IOException("Network error"));

        assertThatThrownBy(() -> service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10))
                .isInstanceOf(YoutubeApiRequestException.class);
    }

    @Test
    void updateExistingItems_ShouldThrowRequestException_WhenHttpException400ButNotAuth() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new HttpException("GET", 400, "Bad Request"));

        assertThatThrownBy(() -> service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch video details");
    }

    @Test
    void updateExistingItems_ShouldThrowRequestException_WhenHttpException500() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new HttpException("GET", 500, "Server Error"));

        assertThatThrownBy(() -> service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10))
                .isInstanceOf(YoutubeApiRequestException.class)
                .hasMessageContaining("Failed to fetch video details");
    }

    @Test
    void updateExistingItems_ShouldThrowRequestException_WhenURISyntaxException() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        when(httpClient.get(any(), any(), any()))
                .thenThrow(new URISyntaxException("input", "reason"));

        assertThatThrownBy(() -> service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10))
                .isInstanceOf(YoutubeApiRequestException.class);
    }

    @Test
    void fetchAndCreateItems_ShouldWait_WhenDelayIsPositive() throws Exception {
        Map<String, JsonNode> snippets = Map.of("v1", objectMapper.createObjectNode());
        String responseBody = "{\"items\": []}";
        when(httpClient.get(any(), any(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        long start = System.currentTimeMillis();
        // Set a small delay (50ms) to verify the sleep happens without slowing down tests too much
        service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 50L, 100, 10);
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isGreaterThanOrEqualTo(50L);
    }

    @Test
    void fetchAndCreateItems_ShouldHandleMissingThumbnails() throws Exception {
        String videoId = "v1";
        ObjectNode snippetNode = objectMapper.createObjectNode();
        snippetNode.put("publishedAt", "2023-01-01T12:00:00Z");
        Map<String, JsonNode> snippets = Map.of(videoId, snippetNode);

        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "kind": "youtube#video",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "none"
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getThumbnailUrl()).isNull();
    }

    @Test
    void fetchAndCreateItems_ShouldHandleThumbnailsWithNoValidUrl() throws Exception {
        String videoId = "v1";
        ObjectNode snippetNode = objectMapper.createObjectNode();
        snippetNode.put("publishedAt", "2023-01-01T12:00:00Z");
        Map<String, JsonNode> snippets = Map.of(videoId, snippetNode);

        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "kind": "youtube#video",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "none",
                            "thumbnails": {
                                "high": { "width": 100 }
                            }
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getThumbnailUrl()).isNull();
    }

    @Test
    void fetchAndCreateItems_ShouldSelectHighestResolutionThumbnail() throws Exception {
        String videoId = "v1";
        ObjectNode snippetNode = objectMapper.createObjectNode();
        snippetNode.put("publishedAt", "2023-01-01T12:00:00Z");
        Map<String, JsonNode> snippets = Map.of(videoId, snippetNode);

        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "kind": "youtube#video",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "none",
                            "thumbnails": {
                                "default": { "url": "http://default.jpg" },
                                "maxres": { "url": "http://maxres.jpg" },
                                "high": { "url": "http://high.jpg" }
                            }
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).hasSize(1);
        // "maxres" is first in the preference list
        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("http://maxres.jpg");
    }

    @Test
    void updateExistingItems_ShouldReturnZero_WhenItemsIsNotArray() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");

        String responseBody = "{\"items\": \"not-an-array\"}";
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void updateExistingItems_ShouldSkipItem_WhenIdNotInList() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");

        // Response contains v2, but we requested update for v1
        String responseBody = """
            {
                "items": [
                    {
                        "id": "v2",
                        "snippet": { "title": "Title" }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void updateExistingItems_ShouldHandleLiveStreamingDetails_WithoutScheduledStartTime() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        item.setTitle("Title");
        item.setLiveBroadcastContent(LiveBroadcastContent.LIVE);

        // liveStreamingDetails exists but no scheduledStartTime
        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "live"
                        },
                        "liveStreamingDetails": {
                            "actualStartTime": "2023-01-01T12:00:00Z"
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void updateExistingItems_ShouldHandleNullScheduledStartTime() throws Exception {
        Item item = new Item();
        item.setVideoId("v1");
        item.setTitle("Title");
        item.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);

        // scheduledStartTime is explicit null
        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "upcoming"
                        },
                        "liveStreamingDetails": {
                            "scheduledStartTime": null
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void updateExistingItems_ShouldNotUpdate_WhenScheduledTimeSame() throws Exception {
        String timeStr = "2023-01-01T12:00:00Z";
        Item item = new Item();
        item.setVideoId("v1");
        item.setTitle("Title");
        item.setLiveBroadcastContent(LiveBroadcastContent.UPCOMING);
        item.setScheduledStartTime(OffsetDateTime.parse(timeStr));

        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "upcoming"
                        },
                        "liveStreamingDetails": {
                            "scheduledStartTime": "%s"
                        }
                    }
                ]
            }
            """.formatted(timeStr);
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        int count = service.updateExistingItems(httpClient, "key", List.of(item), 0, 100, 10);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void fetchAndCreateItems_ShouldHandleExplicitNullThumbnails() throws Exception {
        String videoId = "v1";
        ObjectNode snippetNode = objectMapper.createObjectNode();
        snippetNode.put("publishedAt", "2023-01-01T12:00:00Z");
        Map<String, JsonNode> snippets = Map.of(videoId, snippetNode);

        String responseBody = """
            {
                "items": [
                    {
                        "id": "v1",
                        "kind": "youtube#video",
                        "snippet": {
                            "title": "Title",
                            "liveBroadcastContent": "none",
                            "thumbnails": null
                        }
                    }
                ]
            }
            """;
        when(httpClient.get(eq("/youtube/v3/videos"), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));

        List<Item> result = service.fetchAndCreateItemsFromVideoIds(httpClient, "key", snippets, 0, 100, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getThumbnailUrl()).isNull();
    }

    @Test
    void getBestAvailableThumbnailUrl_ShouldReturnNull_WhenNodeIsNull() throws Exception {
        // Use reflection to invoke private method with null to cover the null check branch
        // which is unreachable via public API using Jackson's path() method
        java.lang.reflect.Method method = VideoFetchServiceImpl.class.getDeclaredMethod("getBestAvailableThumbnailUrl", JsonNode.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, (JsonNode) null);
        assertThat(result).isNull();
    }
}
