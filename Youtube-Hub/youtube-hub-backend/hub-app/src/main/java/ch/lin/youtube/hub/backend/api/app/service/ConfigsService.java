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

import ch.lin.youtube.hub.backend.api.app.service.command.CreateConfigCommand;
import ch.lin.youtube.hub.backend.api.app.service.command.UpdateConfigCommand;
import ch.lin.youtube.hub.backend.api.app.service.model.AllConfigsData;
import ch.lin.youtube.hub.backend.api.app.service.model.TimeZoneOption;
import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;

/**
 * Defines the service layer contract for managing hub configurations.
 * <p>
 * This interface outlines the core business logic for creating, retrieving,
 * updating, and deleting {@link HubConfig} entities. It also provides a
 * mechanism for resolving the active configuration to be used by the
 * application.
 */
public interface ConfigsService {

    /**
     * Retrieves a summary of all available configurations.
     *
     * @return An {@link AllConfigsData} object containing the name of the
     * currently enabled configuration and a list of all configuration names.
     * @throws
     * ch.lin.youtube.hub.backend.api.common.exception.ConfigCreationException
     * if the 'default' configuration needs to be created but fails.
     */
    AllConfigsData getAllConfigs();

    /**
     * Creates and persists a new hub configuration.
     *
     * @param command A {@link CreateConfigCommand} containing the data for the
     * new configuration.
     * @return The newly created {@link HubConfig} entity.
     * @throws
     * ch.lin.youtube.hub.backend.common.exception.InvalidRequestException if
     * the configuration name is 'default' or already exists.
     */
    HubConfig createConfig(CreateConfigCommand command);

    /**
     * Deletes all hub configurations from the database. This is a destructive
     * operation and should be used with caution.
     */
    void deleteAllConfigs();

    /**
     * Deletes a specific hub configuration by its name.
     *
     * @param name The unique name of the configuration to delete.
     * @throws
     * ch.lin.youtube.hub.backend.common.exception.InvalidRequestException if
     * attempting to delete the system-reserved 'default' configuration.
     * @throws
     * ch.lin.youtube.hub.backend.api.common.exception.ConfigNotFoundException
     * if no configuration with the given name is found.
     */
    void deleteConfig(String name);

    /**
     * Retrieves a specific hub configuration by its name.
     *
     * @param name The unique name of the configuration to retrieve.
     * @return The corresponding {@link HubConfig} entity.
     * @throws
     * ch.lin.youtube.hub.backend.api.common.exception.ConfigNotFoundException
     * if no configuration with the given name is found (and the name is not
     * 'default').
     */
    HubConfig getConfig(String name);

    /**
     * Updates an existing hub configuration based on the provided command. This
     * method supports partial updates.
     *
     * @param command An {@link UpdateConfigCommand} containing the fields to
     * update.
     * @return The updated {@link HubConfig} entity.
     */
    HubConfig saveConfig(UpdateConfigCommand command);

    /**
     * Resolves and returns the appropriate hub configuration.
     * <p>
     * If a {@code configName} is provided, it retrieves that specific
     * configuration. If not, it falls back to the currently enabled
     * configuration, and finally to a default configuration if no other is
     * found.
     *
     * @param configName The optional name of the configuration to resolve.
     * @return The resolved {@link HubConfig}. This method is designed to always
     * return a valid configuration.
     */
    HubConfig getResolvedConfig(String configName);

    /**
     * Retrieves a list of clean and sorted time zones.
     *
     * @return A list of {@link TimeZoneOption} containing ID and display name.
     */
    List<TimeZoneOption> getTimeZones();
}
