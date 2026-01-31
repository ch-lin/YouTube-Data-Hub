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
package ch.lin.authentication.service.backend.api.app.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.lin.authentication.service.backend.api.app.config.AuthenticationDefaultProperties;
import ch.lin.authentication.service.backend.api.app.config.DefaultConfigFactory;
import ch.lin.authentication.service.backend.api.app.repository.AuthenticationConfigRepository;
import ch.lin.authentication.service.backend.api.app.service.command.CreateConfigCommand;
import ch.lin.authentication.service.backend.api.app.service.command.UpdateConfigCommand;
import ch.lin.authentication.service.backend.api.app.service.model.AllConfigsData;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;
import ch.lin.platform.exception.ConfigCreationException;
import ch.lin.platform.exception.ConfigNotFoundException;
import ch.lin.platform.exception.InvalidRequestException;

@ExtendWith(MockitoExtension.class)
class ConfigsServiceImplTest {

    @Mock
    private AuthenticationConfigRepository authenticationConfigRepository;
    @Mock
    private AuthenticationDefaultProperties defaultProperties;
    @Mock
    private DefaultConfigFactory defaultConfigFactory;

    @InjectMocks
    private ConfigsServiceImpl configsService;

    private AuthenticationConfig defaultConfig;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        defaultConfig = new AuthenticationConfig();
        defaultConfig.setName("default");
        defaultConfig.setEnabled(true);
        defaultConfig.setJwtExpiration(3600L);
        defaultConfig.setJwtRefreshExpiration(7200L);
        defaultConfig.setJwtIssuerUri("http://test");
    }

    @Test
    void getAllConfigs_ShouldReturnDefault_WhenDbEmpty() {
        when(authenticationConfigRepository.count()).thenReturn(0L);
        when(authenticationConfigRepository.findByName("default")).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenReturn(defaultConfig);
        when(authenticationConfigRepository.findAll()).thenReturn(List.of(defaultConfig));
        when(authenticationConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.of(defaultConfig));

        AllConfigsData result = configsService.getAllConfigs();

        assertThat(result.getEnabledConfigName()).isEqualTo("default");
        assertThat(result.getAllConfigNames()).containsExactly("default");
        verify(authenticationConfigRepository).save(Objects.requireNonNull(anyAuthenticationConfig()));
    }

    @Test
    void getAllConfigs_ShouldReturnConfigs_WhenDbNotEmpty() {
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName("custom");
        config.setEnabled(true);

        when(authenticationConfigRepository.count()).thenReturn(1L);
        when(authenticationConfigRepository.findAll()).thenReturn(List.of(config));
        when(authenticationConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.of(config));

        AllConfigsData result = configsService.getAllConfigs();

        assertThat(result.getEnabledConfigName()).isEqualTo("custom");
        assertThat(result.getAllConfigNames()).containsExactly("custom");
    }

    @Test
    void getAllConfigs_ShouldThrow_WhenDefaultConfigCreationFails() {
        when(authenticationConfigRepository.count()).thenReturn(0L);
        when(authenticationConfigRepository.findByName("default")).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);
        when(authenticationConfigRepository.save(Objects.requireNonNull(defaultConfig))).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> configsService.getAllConfigs())
                .isInstanceOf(ConfigCreationException.class)
                .hasMessageContaining("Cannot create default config");
    }

    @Test
    void createConfig_ShouldThrow_WhenNameIsDefault() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("default");

        assertThatThrownBy(() -> configsService.createConfig(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("system-reserved");
    }

    @Test
    void createConfig_ShouldThrow_WhenConfigExists() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("existing");
        when(authenticationConfigRepository.findByName("existing")).thenReturn(Optional.of(new AuthenticationConfig()));

        assertThatThrownBy(() -> configsService.createConfig(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createConfig_ShouldSaveAndDisableOthers_WhenEnabled() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("new");
        when(command.getEnabled()).thenReturn(true);

        when(authenticationConfigRepository.findByName("new")).thenReturn(Optional.empty());
        AuthenticationConfig otherConfig = new AuthenticationConfig();
        otherConfig.setName("other");
        otherConfig.setEnabled(true);
        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(otherConfig));
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenAnswer(i -> i.getArguments()[0]);

        AuthenticationConfig result = configsService.createConfig(command);

        assertThat(result.getName()).isEqualTo("new");
        assertThat(result.getEnabled()).isTrue();
        assertThat(otherConfig.getEnabled()).isFalse();
        verify(authenticationConfigRepository).saveAll(Objects.requireNonNull(anyList()));
    }

    @Test
    void createConfig_ShouldSaveWithoutDisablingOthers_WhenDisabled() {
        CreateConfigCommand command = mock(CreateConfigCommand.class);
        when(command.getName()).thenReturn("newDisabled");
        when(command.getEnabled()).thenReturn(false);

        when(authenticationConfigRepository.findByName("newDisabled")).thenReturn(Optional.empty());
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenAnswer(i -> i.getArguments()[0]);

        AuthenticationConfig result = configsService.createConfig(command);

        assertThat(result.getName()).isEqualTo("newDisabled");
        assertThat(result.getEnabled()).isFalse();
        verify(authenticationConfigRepository, never()).saveAll(Objects.requireNonNull(anyList()));
    }

    @Test
    void deleteAllConfigs_ShouldCallCleanTable() {
        configsService.deleteAllConfigs();
        verify(authenticationConfigRepository).cleanTable();
    }

    @Test
    void deleteConfig_ShouldThrow_WhenNameIsDefault() {
        assertThatThrownBy(() -> configsService.deleteConfig("default"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("system-reserved");
    }

    @Test
    void deleteConfig_ShouldDeleteAndEnableDefault_WhenNoOthersEnabled() {
        String configName = "toDelete";
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName(configName);
        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.of(config));
        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(Collections.emptyList());

        // Mock finding default config to enable it
        when(authenticationConfigRepository.findByName("default")).thenReturn(Optional.of(defaultConfig));
        defaultConfig.setEnabled(false); // Initially false to verify it gets enabled

        configsService.deleteConfig(configName);

        verify(authenticationConfigRepository).delete(config);
        assertThat(defaultConfig.getEnabled()).isTrue();
        verify(authenticationConfigRepository).save(Objects.requireNonNull(defaultConfig));
    }

    @Test
    void deleteConfig_ShouldThrow_WhenConfigNotFound() {
        String configName = "nonExistent";
        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configsService.deleteConfig(configName))
                .isInstanceOf(ConfigNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteConfig_ShouldNotEnableDefault_WhenOtherConfigsEnabled() {
        String configName = "toDelete";
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName(configName);
        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.of(config));

        AuthenticationConfig otherConfig = new AuthenticationConfig();
        otherConfig.setName("other");
        otherConfig.setEnabled(true);
        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(otherConfig));

        configsService.deleteConfig(configName);

        verify(authenticationConfigRepository).delete(config);
        verify(authenticationConfigRepository, never()).findByName("default");
    }

    @Test
    void deleteConfig_ShouldNotSaveDefault_WhenDefaultAlreadyEnabled() {
        String configName = "toDelete";
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName(configName);
        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.of(config));
        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(Collections.emptyList());

        when(authenticationConfigRepository.findByName("default")).thenReturn(Optional.of(defaultConfig));
        defaultConfig.setEnabled(true);

        configsService.deleteConfig(configName);

        verify(authenticationConfigRepository).delete(config);
        verify(authenticationConfigRepository).findByName("default");
        verify(authenticationConfigRepository, never()).save(Objects.requireNonNull(defaultConfig));
    }

    @Test
    void getResolvedConfig_ShouldReturnConfig_WhenExists() {
        String configName = "custom";
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName(configName);
        config.setJwtExpiration(100L);

        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.of(config));
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);

        AuthenticationConfig result = configsService.getResolvedConfig(configName);

        assertThat(result.getName()).isEqualTo(configName);
        // Should fill missing properties from default
        assertThat(result.getJwtRefreshExpiration()).isEqualTo(defaultConfig.getJwtRefreshExpiration());
    }

    @Test
    void getResolvedConfig_ShouldFallbackToDefault_WhenNameNotFound() {
        when(authenticationConfigRepository.findByName("unknown")).thenReturn(Optional.empty());
        when(authenticationConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.empty());
        when(authenticationConfigRepository.count()).thenReturn(0L);

        // Mock creation of default
        when(authenticationConfigRepository.findByName("default")).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenReturn(defaultConfig);

        AuthenticationConfig result = configsService.getResolvedConfig("unknown");

        assertThat(result.getName()).isEqualTo("default");
    }

    @Test
    void getResolvedConfig_ShouldFallbackToDefault_WhenDbNotEmptyButNoConfigFound() {
        when(authenticationConfigRepository.findByName("unknown")).thenReturn(Optional.empty());
        when(authenticationConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.empty());
        when(authenticationConfigRepository.count()).thenReturn(1L);

        // Mock creation of default
        when(authenticationConfigRepository.findByName("default")).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenReturn(defaultConfig);

        AuthenticationConfig result = configsService.getResolvedConfig("unknown");

        assertThat(result.getName()).isEqualTo("default");
    }

    @Test
    void getResolvedConfig_ShouldReturnEnabledConfig_WhenNameIsNull() {
        AuthenticationConfig enabledConfig = new AuthenticationConfig();
        enabledConfig.setName("enabled");
        enabledConfig.setEnabled(true);

        when(authenticationConfigRepository.findFirstByEnabledTrue()).thenReturn(Optional.of(enabledConfig));
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);

        AuthenticationConfig result = configsService.getResolvedConfig(null);

        assertThat(result.getName()).isEqualTo("enabled");
    }

    @Test
    void getResolvedConfig_ShouldNotOverwriteFields_WhenDbConfigIsComplete() {
        String configName = "complete";
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName(configName);
        config.setEnabled(true);
        config.setJwtExpiration(111L);
        config.setJwtRefreshExpiration(222L);
        config.setJwtIssuerUri("http://db-issuer");

        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.of(config));
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);

        AuthenticationConfig result = configsService.getResolvedConfig(configName);

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getJwtExpiration()).isEqualTo(111L);
        assertThat(result.getJwtRefreshExpiration()).isEqualTo(222L);
        assertThat(result.getJwtIssuerUri()).isEqualTo("http://db-issuer");
    }

    @Test
    void getResolvedConfig_ShouldFillAllFields_WhenDbConfigIsEmpty() {
        String configName = "empty";
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName(configName);
        // All fields null

        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.of(config));
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);

        AuthenticationConfig result = configsService.getResolvedConfig(configName);

        assertThat(result.getEnabled()).isEqualTo(defaultConfig.getEnabled());
        assertThat(result.getJwtExpiration()).isEqualTo(defaultConfig.getJwtExpiration());
        assertThat(result.getJwtRefreshExpiration()).isEqualTo(defaultConfig.getJwtRefreshExpiration());
        assertThat(result.getJwtIssuerUri()).isEqualTo(defaultConfig.getJwtIssuerUri());
    }

    @Test
    void getConfig_ShouldReturnConfig_WhenExists() {
        String configName = "existing";
        AuthenticationConfig config = new AuthenticationConfig();
        config.setName(configName);
        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.of(config));

        AuthenticationConfig result = configsService.getConfig(configName);

        assertThat(result).isEqualTo(config);
    }

    @Test
    void getConfig_ShouldReturnDefault_WhenNameIsDefaultAndNotExists() {
        when(authenticationConfigRepository.findByName("default")).thenReturn(Optional.empty());
        when(defaultConfigFactory.create(defaultProperties)).thenReturn(defaultConfig);
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenReturn(defaultConfig);

        AuthenticationConfig result = configsService.getConfig("default");

        assertThat(result).isEqualTo(defaultConfig);
    }

    @Test
    void getConfig_ShouldThrow_WhenNotFoundAndNotDefault() {
        String configName = "nonExistent";
        when(authenticationConfigRepository.findByName(configName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configsService.getConfig(configName))
                .isInstanceOf(ConfigNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void saveConfig_ShouldUpdateExisting() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("existing");
        when(command.getEnabled()).thenReturn(true);

        AuthenticationConfig existing = new AuthenticationConfig();
        existing.setName("existing");
        existing.setEnabled(false);

        when(authenticationConfigRepository.findByName("existing")).thenReturn(Optional.of(existing));
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenAnswer(i -> i.getArguments()[0]);
        // Mock that we have enabled configs so it doesn't try to create default
        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(existing));

        AuthenticationConfig result = configsService.saveConfig(command);

        assertThat(result.getEnabled()).isTrue();
    }

    @Test
    void saveConfig_ShouldCreateNew_WhenNotExists() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("newConfig");
        when(command.getEnabled()).thenReturn(false);

        when(authenticationConfigRepository.findByName("newConfig")).thenReturn(Optional.empty());
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenAnswer(i -> i.getArguments()[0]);
        // Mock default config check
        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(defaultConfig));

        AuthenticationConfig result = configsService.saveConfig(command);

        assertThat(result.getName()).isEqualTo("newConfig");
    }

    @Test
    void saveConfig_ShouldDisableOtherConfigs_WhenEnabled() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("targetConfig");
        when(command.getEnabled()).thenReturn(true);

        AuthenticationConfig otherConfig = new AuthenticationConfig();
        otherConfig.setName("otherConfig");
        otherConfig.setEnabled(true);

        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(otherConfig));

        AuthenticationConfig targetConfig = new AuthenticationConfig();
        targetConfig.setName("targetConfig");
        targetConfig.setEnabled(false);
        when(authenticationConfigRepository.findByName("targetConfig")).thenReturn(Optional.of(targetConfig));

        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenAnswer(i -> i.getArguments()[0]);

        configsService.saveConfig(command);

        assertThat(otherConfig.getEnabled()).isFalse();
        assertThat(targetConfig.getEnabled()).isTrue();
    }

    @Test
    void saveConfig_ShouldUpdateAllFields_WhenProvided() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("existing");
        when(command.getEnabled()).thenReturn(true);
        when(command.getJwtExpiration()).thenReturn(5000L);
        when(command.getJwtRefreshExpiration()).thenReturn(10000L);
        when(command.getJwtIssuerUri()).thenReturn("http://new-issuer");

        AuthenticationConfig existing = new AuthenticationConfig();
        existing.setName("existing");
        existing.setEnabled(false);
        existing.setJwtExpiration(1000L);
        existing.setJwtRefreshExpiration(2000L);
        existing.setJwtIssuerUri("http://old-issuer");

        when(authenticationConfigRepository.findByName("existing")).thenReturn(Optional.of(existing));
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenAnswer(i -> i.getArguments()[0]);
        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(existing));

        AuthenticationConfig result = configsService.saveConfig(command);

        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getJwtExpiration()).isEqualTo(5000L);
        assertThat(result.getJwtRefreshExpiration()).isEqualTo(10000L);
        assertThat(result.getJwtIssuerUri()).isEqualTo("http://new-issuer");
    }

    @Test
    void saveConfig_ShouldNotUpdateFields_WhenNull() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("existing");
        when(command.getEnabled()).thenReturn(null);
        when(command.getJwtExpiration()).thenReturn(null);
        when(command.getJwtRefreshExpiration()).thenReturn(null);
        when(command.getJwtIssuerUri()).thenReturn(null);

        AuthenticationConfig existing = new AuthenticationConfig();
        existing.setName("existing");
        existing.setEnabled(true);
        existing.setJwtExpiration(1000L);
        existing.setJwtRefreshExpiration(2000L);
        existing.setJwtIssuerUri("http://old-issuer");

        when(authenticationConfigRepository.findByName("existing")).thenReturn(Optional.of(existing));
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenAnswer(i -> i.getArguments()[0]);
        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(List.of(existing));

        AuthenticationConfig result = configsService.saveConfig(command);

        assertThat(result.getEnabled()).isTrue(); // Should remain true
        assertThat(result.getJwtExpiration()).isEqualTo(1000L); // Should remain 1000L
        assertThat(result.getJwtRefreshExpiration()).isEqualTo(2000L); // Should remain 2000L
        assertThat(result.getJwtIssuerUri()).isEqualTo("http://old-issuer"); // Should remain old-issuer
    }

    @Test
    void saveConfig_ShouldEnableDefault_WhenNoConfigEnabled() {
        UpdateConfigCommand command = mock(UpdateConfigCommand.class);
        when(command.getName()).thenReturn("myConfig");
        when(command.getEnabled()).thenReturn(false);

        AuthenticationConfig myConfig = new AuthenticationConfig();
        myConfig.setName("myConfig");
        myConfig.setEnabled(true);

        when(authenticationConfigRepository.findByName("myConfig")).thenReturn(Optional.of(myConfig));
        when(authenticationConfigRepository.save(Objects.requireNonNull(anyAuthenticationConfig()))).thenAnswer(i -> i.getArguments()[0]);

        when(authenticationConfigRepository.findAllByEnabledTrue()).thenReturn(Collections.emptyList());

        when(authenticationConfigRepository.findByName("default")).thenReturn(Optional.of(defaultConfig));
        defaultConfig.setEnabled(false);

        configsService.saveConfig(command);

        assertThat(defaultConfig.getEnabled()).isTrue();
        verify(authenticationConfigRepository).save(Objects.requireNonNull(defaultConfig));
    }

    private AuthenticationConfig anyAuthenticationConfig() {
        any(AuthenticationConfig.class);
        return new AuthenticationConfig();
    }
}
