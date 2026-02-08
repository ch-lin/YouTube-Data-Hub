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
package ch.lin.youtube.hub.backend.api.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ch.lin.youtube.hub.backend.api.domain.model.SchedulerType;
import lombok.Data;

/**
 * A type-safe configuration properties class for the default hub configuration.
 * <p>
 * This class is bound to properties prefixed with
 * {@code youtube.hub.default-config} in the application's configuration files
 * (e.g., {@code application.yml}). It provides the values used by
 * {@link DefaultConfigFactory} to create a default
 * {@link ch.lin.youtube.hub.backend.api.domain.model.HubConfig} instance.
 * <p>
 * Example in {@code application.yml}:
 * <pre>
 * youtube:
 *   hub:
 *     default-config:
 *       name: default
 *       enabled: true
 *       youtube-api-key: "your-api-key"
 *       client-id: "your-client-id"
 *       client-secret: "your-client-secret"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "youtube.hub.default-config")
@Data
public class HubDefaultProperties {

    /**
     * The name of the default configuration. Defaults to "default".
     */
    private String name = "default";
    /**
     * Whether the default configuration is enabled. Defaults to true.
     */
    private Boolean enabled = true;
    /**
     * The default YouTube Data API key.
     */
    private String youtubeApiKey;
    /**
     * The default client ID for accessing downloader REST API.
     */
    private String clientId;
    /**
     * The default client secret for accessing downloader REST API.
     */
    private String clientSecret;

    /**
     * Whether to automatically start the fetch scheduler. Defaults to false.
     */
    private Boolean autoStartFetchScheduler = false;

    /**
     * The type of scheduler to use. Defaults to CRON.
     */
    private SchedulerType schedulerType = SchedulerType.CRON;

    /**
     * The fixed rate for the scheduler in milliseconds. Defaults to 24 hours.
     */
    private Long fixedRate = 86400000L;

    /**
     * The cron expression for the scheduler.
     */
    private String cronExpression = "0 0 9,15,21 * * *";

    /**
     * The time zone for the cron expression.
     */
    private String cronTimeZone = "Asia/Taipei";
}
