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
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ch.lin.platform.api.ApiResponse;
import ch.lin.youtube.hub.backend.api.app.service.ConfigsService;
import ch.lin.youtube.hub.backend.api.app.service.command.CreateConfigCommand;
import ch.lin.youtube.hub.backend.api.app.service.command.UpdateConfigCommand;
import ch.lin.youtube.hub.backend.api.app.service.model.AllConfigsData;
import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;
import ch.lin.youtube.hub.backend.api.dto.AllConfigsResponse;
import ch.lin.youtube.hub.backend.api.dto.CreateConfigRequest;
import ch.lin.youtube.hub.backend.api.dto.UpdateConfigRequest;

@ExtendWith(MockitoExtension.class)
class ConfigsControllerTest {

    @Mock
    private ConfigsService configsService;

    private ConfigsController configsController;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        configsController = new ConfigsController(configsService);
    }

    @Test
    void getAllConfigs_ShouldReturnConfigs() {
        AllConfigsData data = new AllConfigsData("default", List.of("default", "custom"));
        when(configsService.getAllConfigs()).thenReturn(data);

        ResponseEntity<AllConfigsResponse> response = configsController.getAllConfigs();
        AllConfigsResponse body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getEnabledConfigName()).isEqualTo("default");
        assertThat(body.getAllConfigNames()).containsExactly("default", "custom");
    }

    @Test
    void createConfig_ShouldCreateAndReturnConfig() {
        // Mock request context for ServletUriComponentsBuilder
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        CreateConfigRequest createRequest = new CreateConfigRequest();
        createRequest.setName("new-config");
        createRequest.setEnabled(true);
        createRequest.setYoutubeApiKey("key");

        HubConfig created = new HubConfig();
        created.setName("new-config");
        when(configsService.createConfig(any(CreateConfigCommand.class))).thenReturn(created);

        ResponseEntity<ApiResponse<HubConfig>> response = configsController.createConfig(createRequest);
        ApiResponse<HubConfig> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(body.getData().getName()).isEqualTo("new-config");

        ArgumentCaptor<CreateConfigCommand> captor = ArgumentCaptor.forClass(CreateConfigCommand.class);
        verify(configsService).createConfig(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("new-config");
        assertThat(captor.getValue().getEnabled()).isTrue();
    }

    @Test
    void deleteAllConfigs_ShouldCallService() {
        ResponseEntity<Void> response = configsController.deleteAllConfigs();

        verify(configsService).deleteAllConfigs();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getConfig_ShouldReturnConfig() {
        HubConfig config = new HubConfig();
        config.setName("test");
        when(configsService.getConfig("test")).thenReturn(config);

        ResponseEntity<HubConfig> response = configsController.getConfig("test");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(config);
    }

    @Test
    void saveConfig_ShouldUpdateAndReturnConfig() {
        UpdateConfigRequest updateRequest = new UpdateConfigRequest();
        updateRequest.setEnabled(true);

        HubConfig saved = new HubConfig();
        saved.setName("test");
        saved.setEnabled(true);
        when(configsService.saveConfig(any(UpdateConfigCommand.class))).thenReturn(saved);

        ResponseEntity<ApiResponse<HubConfig>> response = configsController.saveConfig("test", updateRequest);
        ApiResponse<HubConfig> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getData().getEnabled()).isTrue();

        ArgumentCaptor<UpdateConfigCommand> captor = ArgumentCaptor.forClass(UpdateConfigCommand.class);
        verify(configsService).saveConfig(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("test");
        assertThat(captor.getValue().getEnabled()).isPresent().contains(true);
    }

    @Test
    void deleteConfig_ShouldCallService() {
        ResponseEntity<Void> response = configsController.deleteConfig("test");

        verify(configsService).deleteConfig("test");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
