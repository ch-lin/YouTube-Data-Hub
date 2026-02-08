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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.lin.platform.exception.ConfigCreationException;
import ch.lin.platform.exception.ConfigNotFoundException;
import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.youtube.hub.backend.api.app.config.DefaultConfigFactory;
import ch.lin.youtube.hub.backend.api.app.config.HubDefaultProperties;
import ch.lin.youtube.hub.backend.api.app.repository.HubConfigRepository;
import ch.lin.youtube.hub.backend.api.app.service.command.CreateConfigCommand;
import ch.lin.youtube.hub.backend.api.app.service.command.UpdateConfigCommand;
import ch.lin.youtube.hub.backend.api.app.service.model.AllConfigsData;
import ch.lin.youtube.hub.backend.api.app.service.model.TimeZoneOption;
import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;
import ch.lin.youtube.hub.backend.api.domain.model.SchedulerType;

@ExtendWith(MockitoExtension.class)
class ConfigsServiceImplTest {

    @Mock
    private HubConfigRepository hubConfigRepository;
    @Mock
    private HubDefaultProperties defaultProperties;
    @Mock
    private DefaultConfigFactory defaultConfigFactory;

    private ConfigsServiceImpl configsService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        configsService = new ConfigsServiceImpl(hubConfigRepository, defaultProperties, defaultConfigFactory);
    }

    @Test
    @SuppressWarnings("null")
    void getAllConfigs_ShouldCreateDefault_WhenDbIsEmpty() {
        when(hubConfigRepository.count()).thenReturn(0L);
        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setName("default");

        // Mocking findOrCreateDefaultConfig internals
        when(hubConfigRepository.findByName("default")).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);
        when(hubConfigRepository.save(any())).thenReturn(defaultConfig);

        when(hubConfigRepository.findAll()).thenReturn(List.of(defaultConfig));
        when(hubConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.empty());

        AllConfigsData result = configsService.getAllConfigs();

        assertThat(result.getAllConfigNames()).containsExactly("default");
        assertThat(result.getEnabledConfigName()).isEqualTo("default");
        ArgumentCaptor<HubConfig> captor = ArgumentCaptor.forClass(HubConfig.class);
        verify(hubConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("default");
    }

    @Test
    void getAllConfigs_ShouldNotCreateDefault_WhenDbIsNotEmpty() {
        when(hubConfigRepository.count()).thenReturn(1L);
        HubConfig existingConfig = new HubConfig();
        existingConfig.setName("existing");
        existingConfig.setEnabled(true);

        when(hubConfigRepository.findAll()).thenReturn(List.of(existingConfig));
        when(hubConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.of(existingConfig));

        AllConfigsData result = configsService.getAllConfigs();

        assertThat(result.getAllConfigNames()).containsExactly("existing");
        assertThat(result.getEnabledConfigName()).isEqualTo("existing");
        verify(hubConfigRepository, never()).findByName("default");
    }

    @Test
    void createConfig_ShouldThrowException_WhenTimeZoneIsInvalid() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("new-config");
        when(command.getCronTimeZone()).thenReturn("Invalid/TimeZone");

        assertThatThrownBy(() -> configsService.createConfig(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid cron time zone");
    }

    @Test
    void createConfig_ShouldThrowException_WhenCronExpressionIsInvalid() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("new-config");
        when(command.getCronExpression()).thenReturn("invalid-cron");

        assertThatThrownBy(() -> configsService.createConfig(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid cron expression");
    }

    @Test
    void saveConfig_ShouldThrowException_WhenTimeZoneIsInvalid() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("existing");
        when(command.getEnabled()).thenReturn(Optional.empty());
        when(command.getCronTimeZone()).thenReturn(Optional.of("Invalid/TimeZone"));

        when(hubConfigRepository.findByName("existing")).thenReturn(Optional.of(new HubConfig()));

        assertThatThrownBy(() -> configsService.saveConfig(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid cron time zone");
    }

    @Test
    void saveConfig_ShouldThrowException_WhenCronExpressionIsInvalid() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("existing");
        when(command.getEnabled()).thenReturn(Optional.empty());
        when(command.getCronExpression()).thenReturn(Optional.of("invalid-cron"));

        when(hubConfigRepository.findByName("existing")).thenReturn(Optional.of(new HubConfig()));

        assertThatThrownBy(() -> configsService.saveConfig(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid cron expression");
    }

    @Test
    @SuppressWarnings("null")
    void createConfig_ShouldSucceed_WhenCronAndTimeZoneAreValid() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("valid-config");
        when(command.getCronExpression()).thenReturn("0 0 9 * * *");
        when(command.getCronTimeZone()).thenReturn("Asia/Taipei");

        when(hubConfigRepository.findByName("valid-config")).thenReturn(Optional.empty());
        when(hubConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HubConfig created = configsService.createConfig(command);

        assertThat(created.getCronExpression()).isEqualTo("0 0 9 * * *");
        assertThat(created.getCronTimeZone()).isEqualTo("Asia/Taipei");
    }

    @Test
    @SuppressWarnings("null")
    void saveConfig_ShouldSucceed_WhenUpdatingWithValidCronAndTimeZone() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("existing");
        when(command.getEnabled()).thenReturn(Optional.empty());
        when(command.getCronExpression()).thenReturn(Optional.of("0 0 12 * * *"));
        when(command.getCronTimeZone()).thenReturn(Optional.of("UTC"));

        when(command.getYoutubeApiKey()).thenReturn(Optional.empty());
        when(command.getClientId()).thenReturn(Optional.empty());
        when(command.getClientSecret()).thenReturn(Optional.empty());
        when(command.getAutoStartFetchScheduler()).thenReturn(Optional.empty());
        when(command.getSchedulerType()).thenReturn(Optional.empty());
        when(command.getFixedRate()).thenReturn(Optional.empty());

        HubConfig existing = new HubConfig();
        existing.setName("existing");
        when(hubConfigRepository.findByName("existing")).thenReturn(Optional.of(existing));
        when(hubConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // Return non-empty list to avoid default config logic
        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(new HubConfig()));

        HubConfig updated = configsService.saveConfig(command);

        assertThat(updated.getCronExpression()).isEqualTo("0 0 12 * * *");
        assertThat(updated.getCronTimeZone()).isEqualTo("UTC");
    }

    @SuppressWarnings("null")
    @Test
    void createConfig_ShouldThrowException_WhenNameIsDefault() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("default");

        assertThatThrownBy(() -> configsService.createConfig(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("system-reserved");
    }

    @Test
    void createConfig_ShouldThrowException_WhenConfigExists() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("existing");
        when(hubConfigRepository.findByName("existing")).thenReturn(Optional.of(new HubConfig()));

        assertThatThrownBy(() -> configsService.createConfig(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("already exists");
    }

    @SuppressWarnings("null")
    @Test
    void createConfig_ShouldDisableOthers_WhenEnabledIsTrue() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("new-config");
        when(command.getEnabled()).thenReturn(true);
        when(command.getAutoStartFetchScheduler()).thenReturn(true);
        when(command.getSchedulerType()).thenReturn(SchedulerType.FIXED_RATE);

        HubConfig existingEnabled = new HubConfig();
        existingEnabled.setName("old-config");
        existingEnabled.setEnabled(true);

        when(hubConfigRepository.findByName("new-config")).thenReturn(Optional.empty());
        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(existingEnabled));
        when(hubConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HubConfig created = configsService.createConfig(command);

        assertThat(created.getName()).isEqualTo("new-config");
        assertThat(created.getEnabled()).isTrue();
        assertThat(created.getAutoStartFetchScheduler()).isTrue();
        assertThat(created.getSchedulerType()).isEqualTo(SchedulerType.FIXED_RATE);
        assertThat(existingEnabled.getEnabled()).isFalse();
        verify(hubConfigRepository).saveAll(anyList());
    }

    @SuppressWarnings("null")
    @Test
    void createConfig_ShouldNotDisableOthers_WhenEnabledIsFalse() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("new-config");
        when(command.getEnabled()).thenReturn(false);

        when(hubConfigRepository.findByName("new-config")).thenReturn(Optional.empty());
        when(hubConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HubConfig created = configsService.createConfig(command);

        assertThat(created.getName()).isEqualTo("new-config");
        assertThat(created.getEnabled()).isFalse();
        verify(hubConfigRepository, never()).findAllByEnabledTrue();
    }

    @Test
    void deleteAllConfigs_ShouldCleanTable() {
        configsService.deleteAllConfigs();
        verify(hubConfigRepository).cleanTable();
    }

    @Test
    void deleteConfig_ShouldThrowException_WhenNameIsDefault() {
        assertThatThrownBy(() -> configsService.deleteConfig("default"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("system-reserved");
    }

    @Test
    void deleteConfig_ShouldThrowException_WhenConfigNotFound() {
        when(hubConfigRepository.findByName("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configsService.deleteConfig("missing"))
                .isInstanceOf(ConfigNotFoundException.class);
    }

    @Test
    void deleteConfig_ShouldEnableDefault_WhenNoOtherEnabled() {
        String configName = "custom";
        HubConfig config = new HubConfig();
        config.setName(configName);

        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.of(config));
        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(Collections.emptyList());

        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setName("default");
        defaultConfig.setEnabled(false);
        when(hubConfigRepository.findByName("default")).thenReturn(Optional.of(defaultConfig));

        configsService.deleteConfig(configName);

        verify(hubConfigRepository).delete(config);
        verify(hubConfigRepository).save(defaultConfig);
        assertThat(defaultConfig.getEnabled()).isTrue();
    }

    @Test
    void deleteConfig_ShouldNotEnableDefault_WhenOtherConfigIsEnabled() {
        String configName = "custom";
        HubConfig config = new HubConfig();
        config.setName(configName);

        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.of(config));
        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(new HubConfig()));

        configsService.deleteConfig(configName);

        verify(hubConfigRepository).delete(config);
        verify(hubConfigRepository, never()).findByName("default");
    }

    @Test
    void deleteConfig_ShouldNotUpdateDefault_WhenDefaultIsAlreadyEnabled() {
        String configName = "custom";
        HubConfig config = new HubConfig();
        config.setName(configName);

        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.of(config));
        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(Collections.emptyList());

        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setName("default");
        defaultConfig.setEnabled(true);
        when(hubConfigRepository.findByName("default")).thenReturn(Optional.of(defaultConfig));

        configsService.deleteConfig(configName);

        verify(hubConfigRepository).delete(config);
        verify(hubConfigRepository, never()).save(defaultConfig);
    }

    @SuppressWarnings("null")
    @Test
    void saveConfig_ShouldUpdateExistingConfig() {
        String configName = "existing";
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn(configName);
        when(command.getEnabled()).thenReturn(Optional.of(true));
        when(command.getYoutubeApiKey()).thenReturn(Optional.of("new-key"));
        when(command.getClientId()).thenReturn(Optional.empty());
        when(command.getClientSecret()).thenReturn(Optional.empty());
        when(command.getAutoStartFetchScheduler()).thenReturn(Optional.of(true));
        when(command.getSchedulerType()).thenReturn(Optional.empty());
        when(command.getFixedRate()).thenReturn(Optional.empty());
        when(command.getCronExpression()).thenReturn(Optional.empty());
        when(command.getCronTimeZone()).thenReturn(Optional.empty());

        HubConfig existingConfig = new HubConfig();
        existingConfig.setName(configName);
        existingConfig.setEnabled(false);
        existingConfig.setYoutubeApiKey("old-key");

        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.of(existingConfig));
        when(hubConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(existingConfig));

        HubConfig updated = configsService.saveConfig(command);

        assertThat(updated.getEnabled()).isTrue();
        assertThat(updated.getYoutubeApiKey()).isEqualTo("new-key");
        assertThat(updated.getAutoStartFetchScheduler()).isTrue();
        verify(hubConfigRepository).save(existingConfig);
    }

    @SuppressWarnings("null")
    @Test
    void getResolvedConfig_ShouldFallbackToDefault_WhenConfigNotFound() {
        when(hubConfigRepository.findByName("non-existent")).thenReturn(Optional.empty());
        when(hubConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.empty());
        when(hubConfigRepository.count()).thenReturn(0L);

        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setName("default");

        // Mock findOrCreateDefaultConfig
        when(hubConfigRepository.findByName("default")).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);
        when(hubConfigRepository.save(any())).thenReturn(defaultConfig);

        HubConfig result = configsService.getResolvedConfig("non-existent");

        assertThat(result).isEqualTo(defaultConfig);
    }

    @Test
    void getResolvedConfig_ShouldPopulateDefaults_WhenFieldsAreMissing() {
        HubConfig dbConfig = new HubConfig();
        dbConfig.setName("custom");
        dbConfig.setEnabled(null);
        dbConfig.setFixedRate(null);
        dbConfig.setAutoStartFetchScheduler(null);
        dbConfig.setSchedulerType(null);
        dbConfig.setCronExpression(null);
        dbConfig.setCronTimeZone("");
        // enabled is null, apiKey is null

        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setEnabled(true);
        defaultConfig.setYoutubeApiKey("default-key");
        defaultConfig.setClientId("default-client");
        defaultConfig.setFixedRate(123L);
        defaultConfig.setAutoStartFetchScheduler(true);
        defaultConfig.setSchedulerType(SchedulerType.CRON);
        defaultConfig.setCronExpression("0 0 * * * *");
        defaultConfig.setCronTimeZone("UTC");

        when(hubConfigRepository.findByName("custom")).thenReturn(Optional.of(dbConfig));
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);

        HubConfig result = configsService.getResolvedConfig("custom");

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getYoutubeApiKey()).isEqualTo("default-key");
        assertThat(result.getClientId()).isEqualTo("default-client");
        assertThat(result.getFixedRate()).isEqualTo(123L);
        assertThat(result.getAutoStartFetchScheduler()).isTrue();
        assertThat(result.getSchedulerType()).isEqualTo(SchedulerType.CRON);
        assertThat(result.getCronExpression()).isEqualTo("0 0 * * * *");
        assertThat(result.getCronTimeZone()).isEqualTo("UTC");
    }

    @Test
    void getConfig_ShouldReturnConfig_WhenFound() {
        String configName = "existing";
        HubConfig config = new HubConfig();
        config.setName(configName);
        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.of(config));

        HubConfig result = configsService.getConfig(configName);

        assertThat(result).isEqualTo(config);
    }

    @SuppressWarnings("null")
    @Test
    void getConfig_ShouldReturnDefault_WhenNameIsDefaultAndNotFound() {
        String configName = "default";
        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setName("default");

        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);
        when(hubConfigRepository.save(any())).thenReturn(defaultConfig);

        HubConfig result = configsService.getConfig(configName);

        assertThat(result).isEqualTo(defaultConfig);
        ArgumentCaptor<HubConfig> captor = ArgumentCaptor.forClass(HubConfig.class);
        verify(hubConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("default");
    }

    @Test
    void getConfig_ShouldThrowException_WhenNameIsNotDefaultAndNotFound() {
        String configName = "missing";
        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configsService.getConfig(configName))
                .isInstanceOf(ConfigNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @SuppressWarnings("null")
    @Test
    void saveConfig_ShouldCreateNewConfig_WhenNotFound() {
        String configName = "new-config";
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn(configName);
        when(command.getEnabled()).thenReturn(Optional.of(true));
        when(command.getYoutubeApiKey()).thenReturn(Optional.empty());
        when(command.getClientId()).thenReturn(Optional.empty());
        when(command.getClientSecret()).thenReturn(Optional.empty());
        when(command.getAutoStartFetchScheduler()).thenReturn(Optional.empty());
        when(command.getSchedulerType()).thenReturn(Optional.empty());
        when(command.getFixedRate()).thenReturn(Optional.empty());
        when(command.getCronExpression()).thenReturn(Optional.empty());
        when(command.getCronTimeZone()).thenReturn(Optional.empty());

        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.empty());
        when(hubConfigRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HubConfig existingEnabledConfig = new HubConfig();
        existingEnabledConfig.setName("existing-enabled");
        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(existingEnabledConfig));

        HubConfig saved = configsService.saveConfig(command);

        assertThat(saved.getName()).isEqualTo(configName);
        ArgumentCaptor<HubConfig> captor = ArgumentCaptor.forClass(HubConfig.class);
        verify(hubConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(configName);
    }

    @SuppressWarnings("null")
    @Test
    void saveConfig_ShouldDisableOtherConfigs_WhenEnabledIsTrue() {
        String configName = "current";
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn(configName);
        when(command.getEnabled()).thenReturn(Optional.of(true));
        when(command.getYoutubeApiKey()).thenReturn(Optional.empty());
        when(command.getClientId()).thenReturn(Optional.empty());
        when(command.getClientSecret()).thenReturn(Optional.empty());
        when(command.getAutoStartFetchScheduler()).thenReturn(Optional.empty());
        when(command.getSchedulerType()).thenReturn(Optional.empty());
        when(command.getFixedRate()).thenReturn(Optional.empty());
        when(command.getCronExpression()).thenReturn(Optional.empty());
        when(command.getCronTimeZone()).thenReturn(Optional.empty());

        HubConfig otherConfig = new HubConfig();
        otherConfig.setName("other");
        otherConfig.setEnabled(true);

        HubConfig currentConfig = new HubConfig();
        currentConfig.setName("current");
        currentConfig.setEnabled(true);

        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(otherConfig, currentConfig));
        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.of(currentConfig));
        when(hubConfigRepository.save(any())).thenReturn(currentConfig);

        configsService.saveConfig(command);

        assertThat(otherConfig.getEnabled()).isFalse();
    }

    @Test
    void saveConfig_ShouldEnableDefault_WhenNoConfigEnabledAfterSave() {
        String configName = "current";
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn(configName);
        when(command.getEnabled()).thenReturn(Optional.of(false));
        when(command.getYoutubeApiKey()).thenReturn(Optional.empty());
        when(command.getClientId()).thenReturn(Optional.empty());
        when(command.getClientSecret()).thenReturn(Optional.empty());
        when(command.getAutoStartFetchScheduler()).thenReturn(Optional.empty());
        when(command.getSchedulerType()).thenReturn(Optional.empty());
        when(command.getFixedRate()).thenReturn(Optional.empty());
        when(command.getCronExpression()).thenReturn(Optional.empty());
        when(command.getCronTimeZone()).thenReturn(Optional.empty());

        HubConfig currentConfig = new HubConfig();
        currentConfig.setName(configName);
        currentConfig.setEnabled(true);

        when(hubConfigRepository.findByName(configName)).thenReturn(Optional.of(currentConfig));
        when(hubConfigRepository.save(currentConfig)).thenReturn(currentConfig);
        when(hubConfigRepository.findAllByEnabledTrue()).thenReturn(Collections.emptyList());

        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setName("default");
        when(hubConfigRepository.findByName("default")).thenReturn(Optional.of(defaultConfig));
        when(hubConfigRepository.save(defaultConfig)).thenReturn(defaultConfig);

        configsService.saveConfig(command);

        assertThat(defaultConfig.getEnabled()).isTrue();
        verify(hubConfigRepository).save(defaultConfig);
    }

    @Test
    void getResolvedConfig_ShouldSearchDefaultEnabled_WhenConfigNameEmpty() {
        when(hubConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.empty());
        when(hubConfigRepository.count()).thenReturn(1L);

        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setName("default");
        when(hubConfigRepository.findByName("default")).thenReturn(Optional.of(defaultConfig));

        configsService.getResolvedConfig(null);

        verify(hubConfigRepository, never()).findByName(null);
        verify(hubConfigRepository).findFirstByEnabledTrue();
    }

    @Test
    void getResolvedConfig_ShouldNotOverwriteExistingValues() {
        HubConfig dbConfig = new HubConfig();
        dbConfig.setName("custom");
        dbConfig.setEnabled(false);
        dbConfig.setYoutubeApiKey("existing-key");
        dbConfig.setClientId("existing-client");
        dbConfig.setSchedulerType(SchedulerType.FIXED_RATE);
        dbConfig.setCronExpression("existing-cron");
        dbConfig.setCronTimeZone("existing-zone");

        when(hubConfigRepository.findByName("custom")).thenReturn(Optional.of(dbConfig));

        HubConfig defaultConfig = new HubConfig();
        defaultConfig.setEnabled(true);
        defaultConfig.setYoutubeApiKey("default-key");
        defaultConfig.setClientId("default-client");
        defaultConfig.setSchedulerType(SchedulerType.CRON);
        defaultConfig.setCronExpression("default-cron");
        defaultConfig.setCronTimeZone("default-zone");
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);

        HubConfig result = configsService.getResolvedConfig("custom");

        assertThat(result.getEnabled()).isFalse();
        assertThat(result.getYoutubeApiKey()).isEqualTo("existing-key");
        assertThat(result.getClientId()).isEqualTo("existing-client");
        assertThat(result.getSchedulerType()).isEqualTo(SchedulerType.FIXED_RATE);
        assertThat(result.getCronExpression()).isEqualTo("existing-cron");
        assertThat(result.getCronTimeZone()).isEqualTo("existing-zone");
    }

    @SuppressWarnings("null")
    @Test
    void findOrCreateDefaultConfig_ShouldThrowException_WhenSaveFails() {
        when(hubConfigRepository.count()).thenReturn(0L);
        when(hubConfigRepository.findByName("default")).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(new HubConfig());
        when(hubConfigRepository.save(any())).thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> configsService.getAllConfigs())
                .isInstanceOf(ConfigCreationException.class)
                .hasMessageContaining("Cannot create default config");
    }

    @Test
    void getTimeZones_ShouldReturnSortedAndFilteredList() {
        // Set a specific timezone to ensure deterministic sorting and full code coverage
        // regardless of the machine's actual timezone.
        java.util.TimeZone originalTimeZone = java.util.TimeZone.getDefault();
        try {
            // Set to New York so it's different from Tokyo and Taipei
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"));

            List<TimeZoneOption> timeZones = configsService.getTimeZones();

            assertThat(timeZones).isNotEmpty();
            // Check filtering
            assertThat(timeZones).noneMatch(tz -> tz.id().startsWith("Etc/"));
            assertThat(timeZones).noneMatch(tz -> tz.id().startsWith("SystemV/"));
            assertThat(timeZones).allMatch(tz -> tz.id().contains("/"));

            // Check sorting: System Default (NY) -> Tokyo -> Taipei -> Others
            assertThat(timeZones.get(0).id()).isEqualTo("America/New_York");
            assertThat(timeZones.get(1).id()).isEqualTo("Asia/Tokyo");
            assertThat(timeZones.get(2).id()).isEqualTo("Asia/Taipei");
        } finally {
            // Always restore the original timezone to avoid affecting other tests
            java.util.TimeZone.setDefault(originalTimeZone);
        }
    }
}
