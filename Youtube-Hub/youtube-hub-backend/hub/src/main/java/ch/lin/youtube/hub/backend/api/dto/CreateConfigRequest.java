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
package ch.lin.youtube.hub.backend.api.dto;

import ch.lin.youtube.hub.backend.api.domain.model.SchedulerType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the request body for creating a new
 * {@link ch.lin.youtube.hub.backend.api.domain.model.HubConfig}.
 * <p>
 * This DTO is used by the
 * {@link ch.lin.youtube.hub.backend.api.controller.ConfigsController#createConfig(CreateConfigRequest)}
 * endpoint.
 * <p>
 * Example JSON request body:
 * <pre>
 * {@code
 * {
 *   "name": "new-audio-config",
 *   "enabled": false,
 *   "youtubeApiKey": "AIza...",
 *   "clientId": "your-client-id.apps.googleusercontent.com",
 *   "clientSecret": "your-client-secret"
 * }
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateConfigRequest {

    /**
     * The unique name for the new configuration. Cannot be blank.
     */
    @NotBlank(message = "Configuration name cannot be blank.")
    private String name;

    /**
     * Indicates whether this configuration should be the active one. If
     * multiple configurations are marked as enabled, the system's behavior may
     * be unpredictable.
     */
    private Boolean enabled;

    /**
     * The YouTube Data API key associated with this configuration.
     */
    private String youtubeApiKey;

    /**
     * The Google Cloud project's client ID for OAuth 2.0 authentication, used
     * for operations requiring user authorization.
     */
    private String clientId;

    /**
     * The Google Cloud project's client secret for OAuth 2.0 authentication.
     */
    private String clientSecret;

    /**
     * Whether to automatically start the fetch scheduler.
     */
    private Boolean autoStartFetchScheduler;

    /**
     * The type of scheduler to use (e.g., "FIXED_RATE" or "CRON").
     */
    private SchedulerType schedulerType;

    /**
     * The fixed rate for the scheduler in milliseconds.
     */
    private Long fixedRate;

    /**
     * The cron expression for the scheduler.
     */
    private String cronExpression;

    /**
     * The time zone for the cron expression.
     */
    private String cronTimeZone;

}
