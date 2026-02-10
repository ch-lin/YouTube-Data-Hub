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
package ch.lin.youtube.hub.backend.api.app.service.command;

import ch.lin.youtube.hub.backend.api.domain.model.SchedulerType;
import lombok.Value;

/**
 * Represents a command to create a new {@code HubConfig}.
 * <p>
 * This is an immutable data transfer object (DTO) used to carry all necessary
 * information from an entry point (like a controller) to the service layer for
 * creating a configuration. All fields are expected to be non-null.
 *
 * @see ch.lin.youtube.hub.backend.api.domain.model.HubConfig
 */
@Value
public class CreateConfigCommand {

    /**
     * The unique name for the new configuration.
     */
    String name;

    /**
     * The enabled status of the new configuration.
     */
    Boolean enabled;

    /**
     * The YouTube API key for the new configuration.
     */
    String youtubeApiKey;

    /**
     * The OAuth 2.0 Client ID for the new configuration.
     */
    String clientId;

    /**
     * The OAuth 2.0 Client Secret for the new configuration.
     */
    String clientSecret;

    /**
     * Whether to automatically start the fetch scheduler.
     */
    Boolean autoStartFetchScheduler;

    /**
     * The type of scheduler to use.
     */
    SchedulerType schedulerType;

    /**
     * The fixed rate for the scheduler in milliseconds.
     */
    Long fixedRate;

    /**
     * The cron expression for the scheduler.
     */
    String cronExpression;

    /**
     * The time zone for the cron expression.
     */
    String cronTimeZone;

    /**
     * The daily quota limit for the YouTube Data API.
     */
    Long quota;

    /**
     * The safety threshold for the quota.
     */
    Long quotaSafetyThreshold;
}
