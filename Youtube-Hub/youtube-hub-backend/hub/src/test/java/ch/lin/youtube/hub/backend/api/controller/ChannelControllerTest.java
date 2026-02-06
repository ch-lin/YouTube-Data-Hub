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
package ch.lin.youtube.hub.backend.api.controller;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.lin.youtube.hub.backend.api.app.service.ChannelService;
import ch.lin.youtube.hub.backend.api.app.service.model.AddChannelsResult;
import ch.lin.youtube.hub.backend.api.app.service.model.AddChannelsResult.FailedUrl;
import ch.lin.youtube.hub.backend.api.domain.model.Channel;
import ch.lin.youtube.hub.backend.api.dto.AddChannelsByUrlRequest;
import ch.lin.youtube.hub.backend.api.dto.AddChannelsResponse;
import ch.lin.youtube.hub.backend.api.dto.ChannelResponse;

@ExtendWith(MockitoExtension.class)
class ChannelControllerTest {

    @Mock
    private ChannelService channelService;

    private ChannelController channelController;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        channelController = new ChannelController(channelService);
    }

    @Test
    void getAllChannels_ShouldReturnChannels() {
        Channel channel = new Channel();
        channel.setChannelId("ch1");
        when(channelService.getAllChannels()).thenReturn(List.of(channel));

        ResponseEntity<List<ChannelResponse>> response = channelController.getAllChannels();
        List<ChannelResponse> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).hasSize(1);
        assertThat(body.get(0).getChannelId()).isEqualTo("ch1");
    }

    @Test
    void addChannelsByUrl_ShouldReturnResult() {
        AddChannelsByUrlRequest request = new AddChannelsByUrlRequest();
        request.setApiKey("key");
        request.setUrls(List.of("url1"));
        request.setConfigName("config");

        Channel channel = new Channel();
        channel.setChannelId("ch1");
        FailedUrl failed = new FailedUrl("url2", "reason");

        AddChannelsResult result = new AddChannelsResult(List.of(channel), List.of(failed));
        when(channelService.addChannelsByUrl("key", "config", List.of("url1"))).thenReturn(result);

        ResponseEntity<AddChannelsResponse> response = channelController.addChannelsByUrl(request);
        AddChannelsResponse body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getAddedChannels()).hasSize(1);
        assertThat(body.getFailedUrls()).hasSize(1);
    }

    @Test
    void getChannelDetails_ShouldReturnJson() {
        String json = "{\"id\": \"ch1\"}";
        when(channelService.getChannelDetailsFromApi("ch1", "key", "config")).thenReturn(json);

        ResponseEntity<String> response = channelController.getChannelDetails("ch1", "key", "config");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(json);
    }

    @Test
    void deleteChannel_ShouldCallService() {
        ResponseEntity<Void> response = channelController.deleteChannel("ch1");

        verify(channelService).deleteChannel("ch1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
