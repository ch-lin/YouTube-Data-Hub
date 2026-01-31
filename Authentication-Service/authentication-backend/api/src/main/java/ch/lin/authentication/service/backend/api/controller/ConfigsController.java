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

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ch.lin.authentication.service.backend.api.app.service.ConfigsService;
import ch.lin.authentication.service.backend.api.app.service.command.CreateConfigCommand;
import ch.lin.authentication.service.backend.api.app.service.command.UpdateConfigCommand;
import ch.lin.authentication.service.backend.api.app.service.model.AllConfigsData;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;
import ch.lin.authentication.service.backend.api.dto.AllConfigsResponse;
import ch.lin.authentication.service.backend.api.dto.CreateConfigRequest;
import ch.lin.authentication.service.backend.api.dto.UpdateConfigRequest;
import jakarta.validation.Valid;

/**
 * REST controller for managing authentication service configurations.
 * <p>
 * This controller provides a full set of CRUD (Create, Read, Update, Delete)
 * endpoints for {@link AuthenticationConfig} entities. It allows clients to
 * list, create, view, update, and delete different configurations for the
 * authentication service. It delegates the business logic to the
 * {@link ConfigsService}.
 */
@RestController
public class ConfigsController {

    private final ConfigsService configsService;

    public ConfigsController(ConfigsService configsService) {
        this.configsService = configsService;
    }

    /**
     * Retrieves the names of all available authentication configurations and
     * identifies which one is currently enabled.
     *
     * @return A {@link ResponseEntity} containing an {@link AllConfigsResponse}
     * which includes the name of the enabled configuration and a list of all
     * available configuration names.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X GET http://localhost:8081/configs
     * }
     * </pre>
     */
    @GetMapping("/configs")
    public ResponseEntity<AllConfigsResponse> getAllConfigs() {
        AllConfigsData configsData = configsService.getAllConfigs();
        AllConfigsResponse response = new AllConfigsResponse(configsData.getEnabledConfigName(),
                configsData.getAllConfigNames());
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new authentication configuration.
     *
     * @param request The request body containing the details of the new
     * configuration.
     * @return A {@link ResponseEntity} with the created
     * {@link AuthenticationConfig} and an HTTP 201 Created status. The
     * 'Location' header will contain the URL to the new resource.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X POST http://localhost:8081/configs \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "name": "my-jwt-config",
     *   "enabled": false,
     *   "jwtExpiration": 3600000,
     *   "jwtRefreshExpiration": 86400000
     * }'
     * }
     * </pre>
     */
    @PostMapping("/configs")
    public ResponseEntity<AuthenticationConfig> createConfig(@Valid @RequestBody CreateConfigRequest request) {
        CreateConfigCommand command = CreateConfigCommand.builder()
                .name(request.getName())
                .enabled(request.getEnabled())
                .jwtExpiration(request.getJwtExpiration())
                .jwtRefreshExpiration(request.getJwtRefreshExpiration())
                .jwtIssuerUri(request.getJwtIssuerUri())
                .build();

        AuthenticationConfig createdConfig = configsService.createConfig(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{name}")
                .buildAndExpand(createdConfig.getName())
                .toUri();
        return ResponseEntity.created(location).body(createdConfig);
    }

    /**
     * Deletes all authentication configurations.
     * <p>
     * This endpoint provides a way to reset all configurations. After deletion,
     * the system will automatically recreate the 'default' configuration on the
     * next relevant request.
     *
     * @return A {@link ResponseEntity} with an HTTP 204 No Content status upon
     * successful deletion.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code curl -X DELETE http://localhost:8081/configs}
     * </pre>
     */
    @DeleteMapping("/configs")
    public ResponseEntity<Void> deleteAllConfigs() {
        configsService.deleteAllConfigs();
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a specific authentication configuration by its name.
     *
     * @param name The name of the configuration to retrieve, passed as a path
     * variable.
     * @return A {@link ResponseEntity} containing the
     * {@link AuthenticationConfig} if found. Throws an exception resulting in a
     * 404 Not Found status if no configuration with the given name exists.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X GET http://localhost:8081/configs/default
     * }
     * </pre>
     */
    @GetMapping("/configs/{name}")
    public ResponseEntity<AuthenticationConfig> getConfig(@PathVariable("name") String name) {
        return ResponseEntity.ok(configsService.getConfig(name));
    }

    /**
     * Creates or partially updates a specific authentication configuration.
     * <p>
     * If a configuration with the given name exists, it will be updated with
     * the non-null fields from the request body. If it does not exist, a new
     * configuration will be created with the given name and fields.
     *
     * @param name The name of the configuration to create or update.
     * @param request The request body containing the configuration fields to
     * set.
     * @return A {@link ResponseEntity} containing the saved
     * {@link AuthenticationConfig}.
     * <p>
     * Example cURL request to create/update a config:
     *
     * <pre>
     * {@code
     * curl -X PATCH http://localhost:8081/configs/my-config \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "enabled": true,
     *   "jwtExpiration": 1800000
     * }'
     * }
     * </pre>
     */
    @PatchMapping("/configs/{name}")
    public ResponseEntity<AuthenticationConfig> saveConfig(@PathVariable("name") String name,
            @RequestBody @Valid UpdateConfigRequest request) {
        UpdateConfigCommand command = UpdateConfigCommand.builder()
                .name(name)
                .enabled(request.getEnabled())
                .jwtExpiration(request.getJwtExpiration())
                .jwtRefreshExpiration(request.getJwtRefreshExpiration())
                .jwtIssuerUri(request.getJwtIssuerUri())
                .build();

        AuthenticationConfig savedConfig = configsService.saveConfig(command);
        return ResponseEntity.ok(savedConfig);
    }

    /**
     * Deletes a specific authentication configuration by its name.
     * <p>
     * The 'default' configuration is system-reserved and cannot be deleted.
     *
     * @param name The name of the configuration to delete.
     * @return A {@link ResponseEntity} with an HTTP 204 No Content status upon
     * successful deletion. Throws an exception resulting in a 404 Not Found if
     * the configuration does not exist.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code curl -X DELETE http://localhost:8081/configs/my-jwt-config}
     * </pre>
     */
    @DeleteMapping("/configs/{name}")
    public ResponseEntity<Void> deleteConfig(@PathVariable("name") String name) {
        configsService.deleteConfig(name);
        return ResponseEntity.noContent().build();
    }
}
