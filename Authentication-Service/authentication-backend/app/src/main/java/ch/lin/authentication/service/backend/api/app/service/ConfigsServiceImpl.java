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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

/**
 * The primary implementation of the {@link ConfigsService} interface.
 * <p>
 * This service class orchestrates all business logic related to managing
 * {@link AuthenticationConfig} entities, including creation, retrieval,
 * updates, and deletion. It coordinates with the repository layer and handles
 * business rules, such as ensuring a default configuration always exists and
 * that only one configuration is active at a time.
 */
@Service
public class ConfigsServiceImpl implements ConfigsService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigsServiceImpl.class);

    private final AuthenticationConfigRepository authenticationConfigRepository;
    private final AuthenticationDefaultProperties defaultProperties;
    private final DefaultConfigFactory defaultConfigFactory;

    /**
     * Constructs the service with its required dependencies.
     *
     * @param authenticationConfigRepository The repository for data access.
     * @param defaultProperties The application's default configuration
     * properties.
     * @param defaultConfigFactory A factory for creating default config
     * instances.
     */
    public ConfigsServiceImpl(AuthenticationConfigRepository authenticationConfigRepository, AuthenticationDefaultProperties defaultProperties,
            DefaultConfigFactory defaultConfigFactory) {
        this.authenticationConfigRepository = authenticationConfigRepository;
        this.defaultProperties = defaultProperties;
        this.defaultConfigFactory = defaultConfigFactory;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If no configuration is explicitly enabled in the database, this method
     * defaults to "default" as the active configuration name. It also ensures
     * the 'default' configuration is created if no configurations exist.
     */
    @Override
    public AllConfigsData getAllConfigs() {
        if (authenticationConfigRepository.count() == 0) {
            findOrCreateDefaultConfig(); // Ensure 'default' exists for the name list.
        }
        List<String> allNames = authenticationConfigRepository.findAll()
                .stream()
                .map(AuthenticationConfig::getName).collect(Collectors.toList());

        String enabledConfigName = authenticationConfigRepository.findFirstByEnabledTrue()
                .map(AuthenticationConfig::getName)
                .orElse("default"); // Fallback to 'default' if no config is explicitly enabled
        return new AllConfigsData(enabledConfigName, allNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AuthenticationConfig createConfig(CreateConfigCommand command) {
        String configName = command.getName();
        if ("default".equalsIgnoreCase(configName)) {
            throw new InvalidRequestException(
                    "The 'default' configuration is system-reserved and cannot be created manually.");
        }
        authenticationConfigRepository.findByName(configName).ifPresent(c -> {
            throw new InvalidRequestException("Configuration with name '" + configName + "' already exists.");
        });

        // If this new config is being enabled, disable all others first.
        if (Boolean.TRUE.equals(command.getEnabled())) {
            List<AuthenticationConfig> enabledConfigs = authenticationConfigRepository.findAllByEnabledTrue();
            enabledConfigs.forEach(config -> config.setEnabled(false));
            authenticationConfigRepository.saveAll(enabledConfigs);
        }

        AuthenticationConfig newConfig = new AuthenticationConfig();
        newConfig.setName(configName);
        newConfig.setEnabled(command.getEnabled());
        newConfig.setJwtExpiration(command.getJwtExpiration());
        newConfig.setJwtRefreshExpiration(command.getJwtRefreshExpiration());
        newConfig.setJwtIssuerUri(command.getJwtIssuerUri());

        return authenticationConfigRepository.save(newConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAllConfigs() {
        logger.info("Deleting all configurations.");
        authenticationConfigRepository.cleanTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteConfig(String name) {
        if ("default".equalsIgnoreCase(name)) {
            throw new InvalidRequestException("The 'default' configuration is system-reserved and cannot be deleted.");
        }
        AuthenticationConfig config = authenticationConfigRepository.findByName(name)
                .orElseThrow(() -> new ConfigNotFoundException("Configuration with name '" + name + "' not found."));

        authenticationConfigRepository.delete(Objects.requireNonNull(config));
        logger.info("Deleted configuration: {}", name);

        // After deleting, if no other configuration is enabled, enable the default one.
        if (authenticationConfigRepository.findAllByEnabledTrue().isEmpty()) {
            AuthenticationConfig defaultConfig = findOrCreateDefaultConfig();
            if (!Boolean.TRUE.equals(defaultConfig.getEnabled())) {
                defaultConfig.setEnabled(true);
                authenticationConfigRepository.save(defaultConfig);
                logger.info("Enabled 'default' configuration as no other configuration was active.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticationConfig getConfig(String name) {
        return authenticationConfigRepository.findByName(name)
                .or(() -> "default".equalsIgnoreCase(name) ? Optional.of(findOrCreateDefaultConfig())
                : Optional.empty())
                .orElseThrow(() -> new ConfigNotFoundException("Configuration with name '" + name + "' not found."));
    }

    /**
     * {@inheritDoc} This method performs an "upsert" operation: it updates the
     * configuration if it exists or creates a new one if it does not. If
     * enabling a configuration, it ensures all other configurations are
     * disabled. It also guarantees that the 'default' configuration becomes
     * active if no other configuration is enabled after the operation.
     */
    @Override
    @Transactional
    public AuthenticationConfig saveConfig(UpdateConfigCommand command) {
        String configName = command.getName();

        // If this config is being enabled, disable all others first.
        if (Boolean.TRUE.equals(command.getEnabled())) {
            authenticationConfigRepository.findAllByEnabledTrue().stream()
                    .filter(c -> !c.getName().equals(configName))
                    .forEach(c -> c.setEnabled(false));
        }

        AuthenticationConfig config = authenticationConfigRepository.findByName(configName)
                .orElseGet(() -> {
                    logger.info("No configuration found with name '{}'. Creating a new one.", configName);
                    AuthenticationConfig newConfig = new AuthenticationConfig();
                    newConfig.setName(configName);
                    return newConfig;
                });

        // Apply updates from the command
        if (command.getEnabled() != null) {
            config.setEnabled(command.getEnabled());
        }
        if (command.getJwtExpiration() != null) {
            config.setJwtExpiration(command.getJwtExpiration());
        }
        if (command.getJwtRefreshExpiration() != null) {
            config.setJwtRefreshExpiration(command.getJwtRefreshExpiration());
        }
        if (command.getJwtIssuerUri() != null) {
            config.setJwtIssuerUri(command.getJwtIssuerUri());
        }

        AuthenticationConfig savedConfig = authenticationConfigRepository.save(Objects.requireNonNull(config));

        // After saving, if no configuration is enabled, enable the default one.
        if (authenticationConfigRepository.findAllByEnabledTrue().isEmpty()) {
            AuthenticationConfig defaultConfig = findOrCreateDefaultConfig();
            defaultConfig.setEnabled(true);
            authenticationConfigRepository.save(defaultConfig);
        }

        return savedConfig;
    }

    /**
     * {@inheritDoc} This method resolves the configuration to use based on a
     * fallback strategy:
     * <ol>
     * <li>Uses the configuration with the specified name, if provided and
     * found.</li>
     * <li>Falls back to the currently enabled configuration in the
     * database.</li>
     * <li>Finally, falls back to the 'default' configuration, creating it from
     * application properties if it doesn't exist.</li>
     * </ol>
     * Any missing properties in a database-loaded configuration are filled in
     * from the application's default properties.
     */
    @Override
    @Transactional
    public AuthenticationConfig getResolvedConfig(String configName) {
        Optional<AuthenticationConfig> configOpt = Optional.empty();

        if (StringUtils.hasText(configName)) {
            configOpt = authenticationConfigRepository.findByName(configName);
        }

        if (configOpt.isEmpty()) {
            logger.debug("Config '{}' not found or not specified. Searching for a default enabled config.", configName);
            configOpt = authenticationConfigRepository.findFirstByEnabledTrue();
        }

        // If still no config, and the DB is empty, create the 'default' one.
        if (configOpt.isEmpty() && authenticationConfigRepository.count() == 0) {
            return findOrCreateDefaultConfig();
        }

        // Use the found config or fall back to application properties if no config is
        // active.
        return configOpt.map(dbConfig -> {
            logger.debug("Using config '{}' from database and resolving with defaults.", dbConfig.getName());
            AuthenticationConfig defaultConfig = defaultConfigFactory.create(defaultProperties);

            if (dbConfig.getEnabled() == null) {
                dbConfig.setEnabled(defaultConfig.getEnabled());
            }
            if (dbConfig.getJwtExpiration() == null) {
                dbConfig.setJwtExpiration(defaultConfig.getJwtExpiration());
            }
            if (dbConfig.getJwtRefreshExpiration() == null) {
                dbConfig.setJwtRefreshExpiration(defaultConfig.getJwtRefreshExpiration());
            }
            if (dbConfig.getJwtIssuerUri() == null) {
                dbConfig.setJwtIssuerUri(defaultConfig.getJwtIssuerUri());
            }
            return dbConfig;
        }).orElseGet(this::findOrCreateDefaultConfig);
    }

    /**
     * Finds the 'default' configuration in the database or creates it if it
     * does not exist. This method is idempotent and ensures a default
     * configuration is always available.
     *
     * @return The 'default' {@link AuthenticationConfig} entity.
     */
    private AuthenticationConfig findOrCreateDefaultConfig() {
        return authenticationConfigRepository.findByName("default").orElseGet(() -> {
            logger.info("No 'default' configuration found. Creating one with application properties.");
            AuthenticationConfig defaultAuthenticationConfig = defaultConfigFactory.create(defaultProperties);
            try {
                return authenticationConfigRepository.save(Objects.requireNonNull(defaultAuthenticationConfig));
            } catch (Exception e) {
                logger.error("Failed to create default config", e);
                throw new ConfigCreationException("Cannot create default config. Application properties not found.");
            }
        });
    }
}
