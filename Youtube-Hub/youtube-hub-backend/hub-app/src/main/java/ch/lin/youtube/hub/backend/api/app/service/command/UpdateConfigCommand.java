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

import java.util.Optional;

import ch.lin.youtube.hub.backend.api.domain.model.SchedulerType;
import lombok.Builder;
import lombok.Value;

/**
 * Represents a command to update an existing {@code HubConfig}.
 * <p>
 * This is an immutable data transfer object (DTO) used to carry update
 * information from an entry point (like a controller) to the service layer.
 * Fields are wrapped in {@link Optional} to support partial updates, allowing
 * clients to specify only the fields they wish to change.
 *
 * @see ch.lin.youtube.hub.backend.api.domain.model.HubConfig
 */
@Value
@Builder
public class UpdateConfigCommand {

    /**
     * The unique name of the configuration to update. This field is mandatory
     * for identifying the target entity.
     */
    String name;

    /**
     * An optional new value for the enabled status of the configuration.
     */
    Optional<Boolean> enabled;

    /**
     * An optional new YouTube API key.
     */
    Optional<String> youtubeApiKey;

    /**
     * An optional new OAuth 2.0 Client ID.
     */
    Optional<String> clientId;

    /**
     * An optional new OAuth 2.0 Client Secret.
     */
    Optional<String> clientSecret;

    /**
     * An optional new value for auto start fetch scheduler.
     */
    Optional<Boolean> autoStartFetchScheduler;

    /**
     * An optional new scheduler type.
     */
    Optional<SchedulerType> schedulerType;

    /**
     * An optional new fixed rate.
     */
    Optional<Long> fixedRate;

    /**
     * An optional new cron expression.
     */
    Optional<String> cronExpression;

    /**
     * An optional new cron time zone.
     */
    Optional<String> cronTimeZone;

    /**
     * An optional new daily quota limit.
     */
    Optional<Long> quota;

    /**
     * An optional new safety threshold for the quota.
     */
    Optional<Long> quotaSafetyThreshold;
}
