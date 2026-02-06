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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ch.lin.platform.exception.ConfigCreationException;
import ch.lin.platform.exception.ConfigNotFoundException;
import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.youtube.hub.backend.api.app.config.DefaultConfigFactory;
import ch.lin.youtube.hub.backend.api.app.config.HubDefaultProperties;
import ch.lin.youtube.hub.backend.api.app.repository.HubConfigRepository;
import ch.lin.youtube.hub.backend.api.app.service.command.CreateConfigCommand;
import ch.lin.youtube.hub.backend.api.app.service.command.UpdateConfigCommand;
import ch.lin.youtube.hub.backend.api.app.service.model.AllConfigsData;
import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;

/**
 * Service implementation for managing hub configurations.
 * <p>
 * This class provides the concrete logic for operations defined in the
 * {@link ConfigsService} interface. It handles interactions with the
 * {@link HubConfigRepository} and uses a {@link DefaultConfigFactory} to manage
 * a system-level default configuration.
 */
@Service
public class ConfigsServiceImpl implements ConfigsService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigsServiceImpl.class);

    private final HubConfigRepository hubConfigRepository;
    private final HubDefaultProperties defaultProperties;
    private final DefaultConfigFactory defaultConfigFactory;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param hubConfigRepository The repository for {@link HubConfig} entities.
     * @param defaultProperties The externalized properties for the default
     * config.
     * @param defaultConfigFactory The factory for creating default config
     * instances.
     */
    public ConfigsServiceImpl(HubConfigRepository hubConfigRepository, HubDefaultProperties defaultProperties,
            DefaultConfigFactory defaultConfigFactory) {
        this.hubConfigRepository = hubConfigRepository;
        this.defaultProperties = defaultProperties;
        this.defaultConfigFactory = defaultConfigFactory;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation ensures that a 'default' configuration exists by
     * creating one if the database is empty. It also provides 'default' as a
     * fallback name if no other configuration is explicitly enabled.
     */
    @Override
    public AllConfigsData getAllConfigs() {
        if (hubConfigRepository.count() == 0) {
            findOrCreateDefaultConfig();
        }
        List<String> allNames = hubConfigRepository.findAll()
                .stream()
                .map(HubConfig::getName).collect(Collectors.toList());

        String enabledConfigName = hubConfigRepository.findFirstByEnabledTrue()
                .map(HubConfig::getName)
                .orElse("default"); // Fallback to 'default' if no config is explicitly enabled
        return new AllConfigsData(enabledConfigName, allNames);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation prevents the creation of a configuration named
     * 'default' as it is system-reserved. If the new configuration is set to be
     * enabled, it will automatically disable any other currently enabled
     * configuration to ensure only one is active at a time.
     */
    @Override
    @Transactional
    public HubConfig createConfig(CreateConfigCommand command) {
        String configName = command.getName();
        if ("default".equalsIgnoreCase(configName)) {
            throw new InvalidRequestException(
                    "The 'default' configuration is system-reserved and cannot be created manually.");
        }
        hubConfigRepository.findByName(configName).ifPresent(c -> {
            throw new InvalidRequestException("Configuration with name '" + configName + "' already exists.");
        });

        // If this new config is being enabled, disable all others first.
        if (Boolean.TRUE.equals(command.getEnabled())) {
            List<HubConfig> enabledConfigs = hubConfigRepository.findAllByEnabledTrue();
            enabledConfigs.forEach(config -> config.setEnabled(false));
            hubConfigRepository.saveAll(enabledConfigs);
        }

        HubConfig newConfig = new HubConfig();
        newConfig.setName(configName);
        newConfig.setEnabled(command.getEnabled());
        newConfig.setYoutubeApiKey(command.getYoutubeApiKey());
        newConfig.setClientId(command.getClientId());
        newConfig.setClientSecret(command.getClientSecret());

        return hubConfigRepository.save(newConfig);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses a bulk delete operation for efficiency.
     */
    @Override
    public void deleteAllConfigs() {
        logger.info("Deleting all configurations.");
        hubConfigRepository.cleanTable();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation prevents the deletion of the system-reserved
     * 'default' configuration. After deleting a configuration, it checks if any
     * other configuration remains enabled. If not, it automatically enables the
     * 'default' configuration as a fallback. It may also create the default
     * config if it does not exist.
     */
    @Override
    @Transactional
    public void deleteConfig(String name) {
        if ("default".equalsIgnoreCase(name)) {
            throw new InvalidRequestException("The 'default' configuration is system-reserved and cannot be deleted.");
        }
        HubConfig config = hubConfigRepository.findByName(name)
                .orElseThrow(() -> new ConfigNotFoundException("Configuration with name '" + name + "' not found."));

        hubConfigRepository.delete(Objects.requireNonNull(config));
        logger.info("Deleted configuration: {}", name);

        // After deleting, if no other configuration is enabled, enable the default one.
        if (hubConfigRepository.findAllByEnabledTrue().isEmpty()) {
            HubConfig defaultConfig = findOrCreateDefaultConfig();
            if (!Boolean.TRUE.equals(defaultConfig.getEnabled())) {
                defaultConfig.setEnabled(true);
                hubConfigRepository.save(defaultConfig);
                logger.info("Enabled 'default' configuration as no other configuration was active.");
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation provides special handling for the 'default'
     * configuration, creating it if it doesn't exist.
     */
    @Override
    public HubConfig getConfig(String name) {
        return hubConfigRepository.findByName(name)
                .or(() -> "default".equalsIgnoreCase(name) ? Optional.of(findOrCreateDefaultConfig())
                : Optional.empty())
                .orElseThrow(() -> new ConfigNotFoundException("Configuration with name '" + name + "' not found."));
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs an "upsert" operation: it updates the
     * configuration if it exists, or creates a new one if it does not. If a
     * configuration is being enabled, it ensures all others are disabled first.
     * It also guarantees that the 'default' configuration is enabled if no
     * other configuration is active after the save operation.
     */
    @Override
    @Transactional
    public HubConfig saveConfig(UpdateConfigCommand command) {
        String configName = command.getName();

        // If this config is being enabled, disable all others first.
        if (command.getEnabled().orElse(false)) {
            hubConfigRepository.findAllByEnabledTrue().stream()
                    .filter(c -> !c.getName().equals(configName))
                    .forEach(c -> c.setEnabled(false));
        }

        HubConfig config = hubConfigRepository.findByName(configName)
                .orElseGet(() -> {
                    logger.info("No configuration found with name '{}'. Creating a new one.", configName);
                    HubConfig newConfig = new HubConfig();
                    newConfig.setName(configName);
                    return newConfig;
                });

        // Apply updates from the command
        command.getEnabled().ifPresent(config::setEnabled);
        command.getYoutubeApiKey().ifPresent(config::setYoutubeApiKey);
        command.getClientId().ifPresent(config::setClientId);
        command.getClientSecret().ifPresent(config::setClientSecret);

        HubConfig savedConfig = hubConfigRepository.save(config);

        // After saving, if no configuration is enabled, enable the default one.
        if (hubConfigRepository.findAllByEnabledTrue().isEmpty()) {
            HubConfig defaultConfig = findOrCreateDefaultConfig();
            defaultConfig.setEnabled(true);
            hubConfigRepository.save(defaultConfig);
        }

        return savedConfig;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation follows a specific resolution order:
     * <ol>
     * <li>A configuration matching the provided {@code configName}.</li>
     * <li>The first currently enabled configuration found in the database.</li>
     * <li>The 'default' configuration (creating it if it doesn't exist).</li>
     * </ol>
     * When a database-stored configuration is resolved, any of its null or
     * empty fields are populated with values from the default application
     * properties.
     */
    @Override
    @Transactional
    public HubConfig getResolvedConfig(String configName) {
        Optional<HubConfig> configOpt = Optional.empty();

        if (StringUtils.hasText(configName)) {
            configOpt = hubConfigRepository.findByName(configName);
        }

        if (configOpt.isEmpty()) {
            logger.debug("Config '{}' not found or not specified. Searching for a default enabled config.", configName);
            configOpt = hubConfigRepository.findFirstByEnabledTrue();
        }

        // If still no config, and the DB is empty, create the 'default' one.
        if (configOpt.isEmpty() && hubConfigRepository.count() == 0) {
            return findOrCreateDefaultConfig();
        }

        // Use the found config or fall back to application properties if no config is
        // active.
        return configOpt.map(dbConfig -> {
            logger.debug("Using config '{}' from database and resolving with defaults.", dbConfig.getName());
            HubConfig defaultConfig = defaultConfigFactory.create(defaultProperties);

            if (dbConfig.getEnabled() == null) {
                dbConfig.setEnabled(defaultConfig.getEnabled());
            }
            if (!StringUtils.hasText(dbConfig.getYoutubeApiKey())) {
                dbConfig.setYoutubeApiKey(defaultConfig.getYoutubeApiKey());
            }
            if (!StringUtils.hasText(dbConfig.getClientId())) {
                dbConfig.setClientId(defaultConfig.getClientId());
            }
            return dbConfig;
        }).orElseGet(this::findOrCreateDefaultConfig);
    }

    /**
     * Finds the 'default' configuration in the database or creates it if it
     * does not exist. This method is idempotent and ensures a default
     * configuration is always available.
     *
     * @return The 'default' {@link HubConfig} entity.
     * @throws ConfigCreationException if the default configuration cannot be
     * created from application properties.
     */
    private HubConfig findOrCreateDefaultConfig() {
        return hubConfigRepository.findByName("default").orElseGet(() -> {
            logger.info("No 'default' configuration found. Creating one with application properties.");
            HubConfig defaultHubConfig = defaultConfigFactory.create(defaultProperties);
            try {
                return hubConfigRepository.save(Objects.requireNonNull(defaultHubConfig));
            } catch (Exception e) {
                logger.error("Failed to create default config", e);
                throw new ConfigCreationException("Cannot create default config. Application properties not found.");
            }
        });
    }
}
