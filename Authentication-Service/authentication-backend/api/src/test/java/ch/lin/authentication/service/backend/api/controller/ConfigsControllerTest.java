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
package ch.lin.authentication.service.backend.api.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.lin.authentication.service.backend.api.app.service.ConfigsService;
import ch.lin.authentication.service.backend.api.app.service.command.CreateConfigCommand;
import ch.lin.authentication.service.backend.api.app.service.command.UpdateConfigCommand;
import ch.lin.authentication.service.backend.api.app.service.model.AllConfigsData;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;
import ch.lin.authentication.service.backend.api.dto.CreateConfigRequest;
import ch.lin.authentication.service.backend.api.dto.UpdateConfigRequest;

@ExtendWith(MockitoExtension.class)
class ConfigsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConfigsService configsService;

    @InjectMocks
    private ConfigsController configsController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(configsController).build();
    }

    @Test
    void getAllConfigs_ShouldReturnAllConfigs() throws Exception {
        // Given
        List<String> allNames = Arrays.asList("default", "custom");
        AllConfigsData configsData = new AllConfigsData("default", allNames);
        when(configsService.getAllConfigs()).thenReturn(configsData);

        // When & Then
        mockMvc.perform(get("/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value("default"))
                .andExpect(jsonPath("$.configs[0]").value("default"))
                .andExpect(jsonPath("$.configs[1]").value("custom"));
    }

    @Test
    void createConfig_ShouldCreateConfigAndReturnCreated() throws Exception {
        // Given
        CreateConfigRequest request = new CreateConfigRequest("new-config", false, 3600000L, 86400000L, "http://issuer.com");
        AuthenticationConfig createdConfig = new AuthenticationConfig();
        createdConfig.setName("new-config");
        createdConfig.setEnabled(false);
        createdConfig.setJwtExpiration(3600000L);
        createdConfig.setJwtRefreshExpiration(86400000L);
        createdConfig.setJwtIssuerUri("http://issuer.com");

        when(configsService.createConfig(any(CreateConfigCommand.class))).thenReturn(createdConfig);

        // When & Then
        mockMvc.perform(post("/configs")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/configs/new-config"))
                .andExpect(jsonPath("$.name").value("new-config"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void deleteAllConfigs_ShouldDeleteAllAndReturnNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/configs"))
                .andExpect(status().isNoContent());

        verify(configsService).deleteAllConfigs();
    }

    @Test
    void getConfig_ShouldReturnConfigByName() throws Exception {
        // Given
        String configName = "default";
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName(configName);
        when(configsService.getConfig(configName)).thenReturn(config);

        // When & Then
        mockMvc.perform(get("/configs/{name}", configName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(configName));
    }

    @Test
    void saveConfig_ShouldUpdateConfigAndReturnOk() throws Exception {
        // Given
        String configName = "my-config";
        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setEnabled(true);
        request.setJwtExpiration(1800000L);
        AuthenticationConfig savedConfig = new AuthenticationConfig();
        savedConfig.setName(configName);
        savedConfig.setEnabled(true);
        savedConfig.setJwtExpiration(1800000L);

        when(configsService.saveConfig(any(UpdateConfigCommand.class))).thenReturn(savedConfig);

        // When & Then
        mockMvc.perform(patch("/configs/{name}", configName)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(configName))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.jwtExpiration").value(1800000L));
    }

    @Test
    void deleteConfig_ShouldDeleteConfigAndReturnNoContent() throws Exception {
        // Given
        String configName = "my-jwt-config";

        // When & Then
        mockMvc.perform(delete("/configs/{name}", configName))
                .andExpect(status().isNoContent());

        verify(configsService).deleteConfig(configName);
    }
}
