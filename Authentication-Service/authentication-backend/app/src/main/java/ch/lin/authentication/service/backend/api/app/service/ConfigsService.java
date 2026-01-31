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

import ch.lin.authentication.service.backend.api.app.service.command.CreateConfigCommand;
import ch.lin.authentication.service.backend.api.app.service.command.UpdateConfigCommand;
import ch.lin.authentication.service.backend.api.app.service.model.AllConfigsData;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;

/**
 * Service interface for managing {@link AuthenticationConfig} entities.
 * <p>
 * This interface defines the contract for all business logic related to the
 * creation, retrieval, updating, and deletion of authentication configurations.
 */
public interface ConfigsService {

    /**
     * Retrieves a summary of all available configurations.
     * <p>
     * Implementations should define a fallback (e.g., "default") if no
     * configuration is explicitly enabled.
     *
     * @return An {@link AllConfigsData} object containing the name of the
     * enabled configuration and a list of all configuration names.
     */
    AllConfigsData getAllConfigs();

    /**
     * Creates a new authentication configuration.
     *
     * @param command The command object containing the details for the new
     * configuration.
     * @return The newly created and persisted {@link AuthenticationConfig}
     * entity.
     * @throws
     * ch.lin.authentication.service.backend.api.app.exception.InvalidRequestException
     * if a configuration with the same name already exists, or if the name is
     * 'default'.
     */
    AuthenticationConfig createConfig(CreateConfigCommand command);

    /**
     * Deletes all authentication configurations from the database.
     * <p>
     * This is a destructive operation and should be used with caution.
     */
    void deleteAllConfigs();

    /**
     * Deletes a specific authentication configuration by its name.
     *
     * @param name The name of the configuration to delete.
     * @throws
     * ch.lin.authentication.service.backend.api.app.exception.InvalidRequestException
     * if the 'default' configuration is targeted for deletion.
     * @throws
     * ch.lin.authentication.service.backend.api.app.exception.ConfigNotFoundException
     * if no configuration with the given name is found.
     */
    void deleteConfig(String name);

    /**
     * Retrieves a specific authentication configuration by its name.
     *
     * @param name The name of the configuration to retrieve.
     * @return The {@link AuthenticationConfig} entity.
     * @throws
     * ch.lin.authentication.service.backend.api.app.exception.ConfigNotFoundException
     * if no configuration with the given name is found.
     */
    AuthenticationConfig getConfig(String name);

    /**
     * Creates a new configuration or updates an existing one based on the
     * provided command.
     *
     * If a configuration with the name specified in the command exists, it will
     * be updated. Otherwise, a new one will be created.
     *
     * @param command The command object containing the name and fields to
     * update.
     * @return The saved {@link AuthenticationConfig} entity.
     */
    AuthenticationConfig saveConfig(UpdateConfigCommand command);

    /**
     * Retrieves a configuration by name and merges it with default values for
     * any null properties.
     * <p>
     * If the named configuration is not found, it falls back to the currently
     * enabled configuration. If none are enabled, it falls back to the
     * 'default' configuration, creating it if necessary. This ensures a valid
     * configuration is always returned.
     *
     * @param preferredConfigName The name of the desired configuration. Can be
     * null or empty to get the active default.
     * @return A fully populated, non-null {@link AuthenticationConfig} object.
     */
    AuthenticationConfig getResolvedConfig(String preferredConfigName);
}
