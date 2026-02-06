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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.platform.http.HttpClient;
import ch.lin.youtube.hub.backend.api.app.repository.ChannelRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.AddChannelsResult;
import ch.lin.youtube.hub.backend.api.common.exception.ChannelNotFoundException;
import ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;

@ExtendWith(MockitoExtension.class)
class ChannelServiceImplTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ConfigsService configsService;

    private ChannelServiceImpl channelService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        channelService = new ChannelServiceImpl(channelRepository, configsService);
    }

    @Test
    void getAllChannels_ShouldReturnAllChannels() {
        Channel channel = new Channel();
        channel.setChannelId("123");
        when(channelRepository.findAll()).thenReturn(List.of(channel));

        List<Channel> result = channelService.getAllChannels();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChannelId()).isEqualTo("123");
    }

    @Test
    void saveChannel_ShouldSave_WhenValid() {
        Channel channel = new Channel();
        channel.setChannelId("123");
        channel.setTitle("Test Channel");
        channel.setHandle("@test");

        when(channelRepository.findByChannelId("123")).thenReturn(Optional.empty());
        when(channelRepository.save(channel)).thenReturn(channel);

        Channel saved = channelService.saveChannel(channel);

        assertThat(saved).isEqualTo(channel);
        verify(channelRepository).save(channel);
    }

    @Test
    void saveChannel_ShouldUpdate_WhenExists() {
        Channel existing = new Channel();
        existing.setChannelId("123");
        existing.setTitle("Old Title");
        existing.setHandle("@old");

        Channel update = new Channel();
        update.setChannelId("123");
        update.setTitle("New Title");
        update.setHandle("@new");

        when(channelRepository.findByChannelId("123")).thenReturn(Optional.of(existing));
        when(channelRepository.save(existing)).thenReturn(existing);

        Channel saved = channelService.saveChannel(update);

        assertThat(saved.getTitle()).isEqualTo("New Title");
        assertThat(saved.getHandle()).isEqualTo("@new");
        verify(channelRepository).save(existing);
    }

    @Test
    void saveChannel_ShouldThrow_WhenValidationFails() {
        // 1. Channel is null
        assertThatThrownBy(() -> channelService.saveChannel(null))
                .isInstanceOf(InvalidRequestException.class);

        // 2. Channel ID is null
        Channel c1 = new Channel();
        c1.setTitle("Title");
        c1.setHandle("@handle");
        assertThatThrownBy(() -> channelService.saveChannel(c1))
                .isInstanceOf(InvalidRequestException.class);

        // 3. Channel ID is blank
        Channel c2 = new Channel();
        c2.setChannelId("   ");
        c2.setTitle("Title");
        c2.setHandle("@handle");
        assertThatThrownBy(() -> channelService.saveChannel(c2))
                .isInstanceOf(InvalidRequestException.class);

        // 4. Title is null
        Channel c3 = new Channel();
        c3.setChannelId("id");
        c3.setHandle("@handle");
        assertThatThrownBy(() -> channelService.saveChannel(c3))
                .isInstanceOf(InvalidRequestException.class);

        // 5. Title is blank
        Channel c4 = new Channel();
        c4.setChannelId("id");
        c4.setTitle("");
        c4.setHandle("@handle");
        assertThatThrownBy(() -> channelService.saveChannel(c4))
                .isInstanceOf(InvalidRequestException.class);

        // 6. Handle is null
        Channel c5 = new Channel();
        c5.setChannelId("id");
        c5.setTitle("Title");
        assertThatThrownBy(() -> channelService.saveChannel(c5))
                .isInstanceOf(InvalidRequestException.class);

        // 7. Handle is blank
        Channel c6 = new Channel();
        c6.setChannelId("id");
        c6.setTitle("Title");
        c6.setHandle("  ");
        assertThatThrownBy(() -> channelService.saveChannel(c6))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteChannel_ShouldDelete_WhenFound() {
        Channel channel = new Channel();
        channel.setChannelId("123");
        when(channelRepository.findByChannelId("123")).thenReturn(Optional.of(channel));

        channelService.deleteChannel("123");

        verify(channelRepository).delete(channel);
    }

    @Test
    void deleteChannel_ShouldThrow_WhenNotFound() {
        when(channelRepository.findByChannelId("123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.deleteChannel("123"))
                .isInstanceOf(ChannelNotFoundException.class);
    }

    @Test
    void deleteChannel_ShouldThrow_WhenIdInvalid() {
        assertThatThrownBy(() -> channelService.deleteChannel(null))
                .isInstanceOf(InvalidRequestException.class);

        assertThatThrownBy(() -> channelService.deleteChannel(""))
                .isInstanceOf(InvalidRequestException.class);

        assertThatThrownBy(() -> channelService.deleteChannel("   "))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void addChannelsByUrl_ShouldReturnEmpty_WhenUrlsNull() {
        AddChannelsResult result = channelService.addChannelsByUrl("key", "config", null);
        assertThat(result.getAddedChannels()).isEmpty();
        assertThat(result.getFailedUrls()).isEmpty();
    }

    @Test
    void addChannelsByUrl_ShouldReturnEmpty_WhenUrlsEmpty() {
        AddChannelsResult result = channelService.addChannelsByUrl("key", "config", Collections.emptyList());
        assertThat(result.getAddedChannels()).isEmpty();
        assertThat(result.getFailedUrls()).isEmpty();
    }

    @Test
    void addChannelsByUrl_ShouldThrow_WhenNoApiKey() {
        HubConfig config = new HubConfig();
        // No API key in config
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        List<String> urls = List.of("http://url");
        assertThatThrownBy(() -> channelService.addChannelsByUrl(null, null, urls))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("API key is required");
    }

    @SuppressWarnings("null")
    @Test
    void addChannelsByUrl_ShouldThrow_WhenConfigNameMismatch() {
        HubConfig resolvedConfig = new HubConfig();
        resolvedConfig.setName("default");
        when(configsService.getResolvedConfig("custom")).thenReturn(resolvedConfig);

        List<String> urls = List.of("http://url");
        assertThatThrownBy(() -> channelService.addChannelsByUrl(null, "custom", urls))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Configuration with name 'custom' not found");
    }

    @SuppressWarnings("null")
    @Test
    void addChannelsByUrl_ShouldProcess_WhenValid() {
        String url = "https://www.youtube.com/@test";
        String apiKey = "key";
        String responseBody = "{\"items\":[{\"id\":\"UC123\",\"snippet\":{\"title\":\"Test Channel\",\"customUrl\":\"@test\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            when(channelRepository.findByChannelId("UC123")).thenReturn(Optional.empty());
            when(channelRepository.save(any(Channel.class))).thenAnswer(i -> i.getArgument(0));

            AddChannelsResult result = channelService.addChannelsByUrl(apiKey, null, List.of(url));

            assertThat(result.getAddedChannels()).hasSize(1);
            Channel added = result.getAddedChannels().get(0);
            assertThat(added.getChannelId()).isEqualTo("UC123");
            assertThat(added.getTitle()).isEqualTo("Test Channel");
            assertThat(added.getHandle()).isEqualTo("@test");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldHandleMalformedUrl() {
        String url = "https://www.youtube.com/watch?v=123"; // No handle
        String apiKey = "key";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class)) {
            AddChannelsResult result = channelService.addChannelsByUrl(apiKey, null, List.of(url));

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(1);
            assertThat(result.getFailedUrls().get(0).getUrl()).isEqualTo(url);
            assertThat(result.getFailedUrls().get(0).getReason()).contains("Could not parse a valid channel handle");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldHandleApiFetchFailure() {
        String url = "https://www.youtube.com/@test";
        String apiKey = "key";
        String responseBody = "{}"; // Empty response

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            AddChannelsResult result = channelService.addChannelsByUrl(apiKey, null, List.of(url));

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(1);
            assertThat(result.getFailedUrls().get(0).getUrl()).isEqualTo(url);
            assertThat(result.getFailedUrls().get(0).getReason()).contains("Could not fetch channel info");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldThrow_WhenHttpClientCloseFails() {
        String url = "invalid-url"; // Use invalid url to skip get() call
        String apiKey = "key";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> doThrow(new IOException("Close failed")).when(mock).close())) {

            List<String> urls = List.of(url);
            assertThatThrownBy(() -> channelService.addChannelsByUrl(apiKey, null, urls))
                    .isInstanceOf(YoutubeApiRequestException.class)
                    .hasMessageContaining("An internal error occurred");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldReturnDetails() {
        String channelId = "UC123";
        String apiKey = "key";
        String responseBody = "{}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            String result = channelService.getChannelDetailsFromApi(channelId, apiKey, null);
            assertThat(result).isEqualTo(responseBody);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldThrow_WhenConfigNameMismatch() {
        HubConfig resolvedConfig = new HubConfig();
        resolvedConfig.setName("default");
        when(configsService.getResolvedConfig("custom")).thenReturn(resolvedConfig);

        assertThatThrownBy(() -> channelService.getChannelDetailsFromApi("id", null, "custom"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Configuration with name 'custom' not found");
    }

    @Test
    void getChannelDetailsFromApi_ShouldThrow_WhenNoApiKey() {
        HubConfig config = new HubConfig();
        // No API key
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        assertThatThrownBy(() -> channelService.getChannelDetailsFromApi("id", null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("API key is required");
    }

    @SuppressWarnings("null")
    @Test
    void addChannelsByUrl_ShouldUseConfig_WhenApiKeyBlank() {
        HubConfig config = new HubConfig();
        config.setName("default");
        config.setYoutubeApiKey("config-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        String url = "https://www.youtube.com/@test";
        String responseBody = "{\"items\":[{\"id\":\"UC123\",\"snippet\":{\"title\":\"Test Channel\",\"customUrl\":\"@test\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            when(channelRepository.findByChannelId("UC123")).thenReturn(Optional.empty());
            when(channelRepository.save(any(Channel.class))).thenAnswer(i -> i.getArgument(0));

            AddChannelsResult result = channelService.addChannelsByUrl("", null, List.of(url));

            assertThat(result.getAddedChannels()).hasSize(1);
            verify(configsService).getResolvedConfig(null);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldHandleUrlParsingEdgeCases() {
        String url1 = "https://www.youtube.com/@"; // Ends with @
        String url2 = "https://www.youtube.com/@/"; // Empty handle part
        String url3 = ""; // Empty url
        String url4 = "https://www.youtube.com/@//"; // Empty handle part with double slash
        String url5 = null; // Null url
        String url6 = "https://www.youtube.com/@/videos"; // Handle is empty but path continues

        List<String> urls = java.util.Arrays.asList(url1, url2, url3, url4, url5, url6);

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class)) {
            AddChannelsResult result = channelService.addChannelsByUrl("key", null, urls);

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(6);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @SuppressWarnings("null")
    @Test
    void addChannelsByUrl_ShouldHandleUrlWithTrailingSlashOrPath() {
        String url1 = "https://www.youtube.com/@test/";
        String url2 = "https://www.youtube.com/@test/videos";
        String responseBody = "{\"items\":[{\"id\":\"UC123\",\"snippet\":{\"title\":\"Test Channel\",\"customUrl\":\"@test\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            when(channelRepository.findByChannelId("UC123")).thenReturn(Optional.empty());
            when(channelRepository.save(any(Channel.class))).thenAnswer(i -> i.getArgument(0));

            AddChannelsResult result = channelService.addChannelsByUrl("key", null, List.of(url1, url2));

            assertThat(result.getAddedChannels()).hasSize(2);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldHandleMissingIdOrTitle() {
        String url = "https://www.youtube.com/@test";
        // Missing ID
        String responseBody = "{\"items\":[{\"snippet\":{\"title\":\"Test Channel\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            AddChannelsResult result = channelService.addChannelsByUrl("key", null, List.of(url));

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(1);
            assertThat(result.getFailedUrls().get(0).getReason()).contains("Could not fetch channel info");
            assertThat(mocked.constructed()).hasSize(1);
        }

        // Missing Title (title is null)
        String responseBody2 = "{\"items\":[{\"id\":\"UC123\",\"snippet\":{}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody2));
                })) {

            AddChannelsResult result = channelService.addChannelsByUrl("key", null, List.of(url));

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(1);
            assertThat(result.getFailedUrls().get(0).getReason()).contains("Could not fetch channel info");
            assertThat(mocked.constructed()).hasSize(1);
        }

        // Both Missing (id and title are null)
        String responseBody3 = "{\"items\":[{\"snippet\":{}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody3));
                })) {

            AddChannelsResult result = channelService.addChannelsByUrl("key", null, List.of(url));

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(1);
            assertThat(result.getFailedUrls().get(0).getReason()).contains("Could not fetch channel info");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @SuppressWarnings("null")
    @Test
    void addChannelsByUrl_ShouldFallbackToHandle_WhenCustomUrlMissing() {
        String url = "https://www.youtube.com/@test";
        // Missing customUrl
        String responseBody = "{\"items\":[{\"id\":\"UC123\",\"snippet\":{\"title\":\"Test Channel\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            when(channelRepository.findByChannelId("UC123")).thenReturn(Optional.empty());
            when(channelRepository.save(any(Channel.class))).thenAnswer(i -> i.getArgument(0));

            AddChannelsResult result = channelService.addChannelsByUrl("key", null, List.of(url));

            assertThat(result.getAddedChannels()).hasSize(1);
            assertThat(result.getAddedChannels().get(0).getHandle()).isEqualTo("@test");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldThrowAuthException_WhenApiKeyInvalid() {
        String url = "https://www.youtube.com/@test";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "API key not valid"));
                })) {

            List<String> urls = List.of(url);
            assertThatThrownBy(() -> channelService.addChannelsByUrl("key", null, urls))
                    .isInstanceOf(ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException.class);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldHandleGenericIoExceptionInFetch() {
        String url = "https://www.youtube.com/@test";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenThrow(new IOException("Network error"));
                })) {

            AddChannelsResult result = channelService.addChannelsByUrl("key", null, List.of(url));

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(1);
            assertThat(result.getFailedUrls().get(0).getReason()).contains("Could not fetch channel info");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldUseDefaultConfig_WhenConfigNameNull() {
        HubConfig config = new HubConfig();
        config.setName("default");
        config.setYoutubeApiKey("config-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        String channelId = "UC123";
        String responseBody = "{}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            channelService.getChannelDetailsFromApi(channelId, null, null);
            verify(configsService).getResolvedConfig(null);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldThrowAuthException_WhenApiKeyInvalid() {
        String channelId = "UC123";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "API key not valid"));
                })) {

            assertThatThrownBy(() -> channelService.getChannelDetailsFromApi(channelId, "key", null))
                    .isInstanceOf(ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiAuthException.class);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldHandleHttpException_WhenNotAuthError() {
        String url = "https://www.youtube.com/@test";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    // Status 500
                    when(mock.get(anyString(), anyMap(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 500, "Server Error"));
                })) {

            AddChannelsResult result = channelService.addChannelsByUrl("key", null, List.of(url));

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(1);
            assertThat(result.getFailedUrls().get(0).getReason()).contains("Could not fetch channel info");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldHandleHttpException_When400ButNotAuthError() {
        String url = "https://www.youtube.com/@test";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    // Status 400 but different message
                    when(mock.get(anyString(), anyMap(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "Bad Request"));
                })) {

            AddChannelsResult result = channelService.addChannelsByUrl("key", null, List.of(url));

            assertThat(result.getAddedChannels()).isEmpty();
            assertThat(result.getFailedUrls()).hasSize(1);
            assertThat(result.getFailedUrls().get(0).getReason()).contains("Could not fetch channel info");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @SuppressWarnings("null")
    @Test
    void addChannelsByUrl_ShouldUseDefaultConfig_WhenConfigNameIsEmpty() {
        HubConfig config = new HubConfig();
        config.setName("default");
        config.setYoutubeApiKey("config-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        String url = "https://www.youtube.com/@test";
        String responseBody = "{\"items\":[{\"id\":\"UC123\",\"snippet\":{\"title\":\"Test Channel\",\"customUrl\":\"@test\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            when(channelRepository.findByChannelId("UC123")).thenReturn(Optional.empty());
            when(channelRepository.save(any(Channel.class))).thenAnswer(i -> i.getArgument(0));

            AddChannelsResult result = channelService.addChannelsByUrl("", "", List.of(url));

            assertThat(result.getAddedChannels()).hasSize(1);
            verify(configsService).getResolvedConfig(null);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @SuppressWarnings("null")
    @Test
    void addChannelsByUrl_ShouldSucceed_WhenConfigNameMatches() {
        HubConfig config = new HubConfig();
        config.setName("custom");
        config.setYoutubeApiKey("config-key");
        when(configsService.getResolvedConfig("custom")).thenReturn(config);

        String url = "https://www.youtube.com/@test";
        String responseBody = "{\"items\":[{\"id\":\"UC123\",\"snippet\":{\"title\":\"Test Channel\",\"customUrl\":\"@test\"}}]}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            when(channelRepository.findByChannelId("UC123")).thenReturn(Optional.empty());
            when(channelRepository.save(any(Channel.class))).thenAnswer(i -> i.getArgument(0));

            AddChannelsResult result = channelService.addChannelsByUrl("", "custom", List.of(url));

            assertThat(result.getAddedChannels()).hasSize(1);
            verify(configsService).getResolvedConfig("custom");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void addChannelsByUrl_ShouldThrow_WhenApiKeyIsBlankInConfig() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("   ");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        List<String> urls = List.of("http://url");
        assertThatThrownBy(() -> channelService.addChannelsByUrl(null, null, urls))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("API key is required");
    }

    @Test
    void getChannelDetailsFromApi_ShouldUseDefaultConfig_WhenConfigNameIsEmpty() {
        HubConfig config = new HubConfig();
        config.setName("default");
        config.setYoutubeApiKey("config-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        String channelId = "UC123";
        String responseBody = "{}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            channelService.getChannelDetailsFromApi(channelId, null, "");
            verify(configsService).getResolvedConfig(null);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldSucceed_WhenConfigNameMatches() {
        HubConfig config = new HubConfig();
        config.setName("custom");
        config.setYoutubeApiKey("config-key");
        when(configsService.getResolvedConfig("custom")).thenReturn(config);

        String channelId = "UC123";
        String responseBody = "{}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            channelService.getChannelDetailsFromApi(channelId, null, "custom");
            verify(configsService).getResolvedConfig("custom");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldThrow_WhenApiKeyIsBlankInConfig() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("   ");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        assertThatThrownBy(() -> channelService.getChannelDetailsFromApi("id", null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("API key is required");
    }

    @Test
    void getChannelDetailsFromApi_ShouldThrowRequestException_WhenHttpExceptionNotAuthError() {
        String channelId = "UC123";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    // Status 500
                    when(mock.get(anyString(), anyMap(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 500, "Server Error"));
                })) {

            assertThatThrownBy(() -> channelService.getChannelDetailsFromApi(channelId, "key", null))
                    .isInstanceOf(ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException.class)
                    .hasMessageContaining("An internal error occurred");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldThrowRequestException_WhenHttpException400ButNotAuthError() {
        String channelId = "UC123";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    // Status 400 but different message
                    when(mock.get(anyString(), anyMap(), any()))
                            .thenThrow(new ch.lin.platform.http.exception.HttpException("GET", 400, "Bad Request"));
                })) {

            assertThatThrownBy(() -> channelService.getChannelDetailsFromApi(channelId, "key", null))
                    .isInstanceOf(ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException.class)
                    .hasMessageContaining("An internal error occurred");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldThrowRequestException_WhenGenericIoException() {
        String channelId = "UC123";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any()))
                            .thenThrow(new IOException("Network error"));
                })) {

            assertThatThrownBy(() -> channelService.getChannelDetailsFromApi(channelId, "key", null))
                    .isInstanceOf(ch.lin.youtube.hub.backend.api.common.exception.YoutubeApiRequestException.class)
                    .hasMessageContaining("An internal error occurred");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getChannelDetailsFromApi_ShouldThrow_WhenChannelIdInvalid() {
        assertThatThrownBy(() -> channelService.getChannelDetailsFromApi(null, "key", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Channel ID cannot be null or blank");

        assertThatThrownBy(() -> channelService.getChannelDetailsFromApi("", "key", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Channel ID cannot be null or blank");

        assertThatThrownBy(() -> channelService.getChannelDetailsFromApi("   ", "key", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Channel ID cannot be null or blank");
    }

    @Test
    void getChannelDetailsFromApi_ShouldUseConfig_WhenApiKeyBlank() {
        HubConfig config = new HubConfig();
        config.setName("default");
        config.setYoutubeApiKey("config-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        String channelId = "UC123";
        String responseBody = "{}";

        try (MockedConstruction<HttpClient> mocked = mockConstruction(HttpClient.class,
                (mock, context) -> {
                    when(mock.get(anyString(), anyMap(), any())).thenReturn(new HttpClient.Response(200, responseBody));
                })) {

            channelService.getChannelDetailsFromApi(channelId, "   ", null);
            verify(configsService).getResolvedConfig(null);
            assertThat(mocked.constructed()).hasSize(1);
        }
    }
}
