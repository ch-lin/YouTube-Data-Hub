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
package ch.lin.youtube.hub.backend.api.domain.model;

import static ch.lin.youtube.hub.backend.api.domain.model.HubConfig.TABLE_NAME;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a named configuration profile for the hub, stored as a JPA entity.
 * This allows for storing different sets of configurations, each identified by
 * a unique name.
 */
@Table(name = TABLE_NAME)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"name", "enabled", "youtubeApiKey", "clientId", "clientSecret",
    "autoStartFetchScheduler", "schedulerType", "fixedRate", "cronExpression",
    "cronTimeZone"}, callSuper = false)
public class HubConfig {

    /**
     * The name of the hub configuration table in the database.
     */
    public static final String TABLE_NAME = "hub_config";

    /**
     * The name of the name column in the database.
     */
    public static final String NAME_COLUMN = "name";

    /**
     * The name of the enabled column in the database.
     */
    public static final String ENABLED_COLUMN = "enabled";

    /**
     * The name of the youtube api key column in the database.
     */
    public static final String YOUTUBE_API_KEY_COLUMN = "youtube_api_key";

    /**
     * The name of the client id column in the database.
     */
    public static final String CLIENT_ID_COLUMN = "client_id";

    /**
     * The name of the client secret column in the database.
     */
    public static final String CLIENT_SECRET_COLUMN = "client_secret";

    /**
     * The name of the auto start fetch scheduler column in the database.
     */
    public static final String AUTO_START_FETCH_SCHEDULER_COLUMN = "auto_start_fetch_scheduler";

    /**
     * The name of the scheduler type column in the database.
     */
    public static final String SCHEDULER_TYPE_COLUMN = "scheduler_type";

    /**
     * The name of the fixed rate column in the database.
     */
    public static final String FIXED_RATE_COLUMN = "fixed_rate";

    /**
     * The name of the cron expression column in the database.
     */
    public static final String CRON_EXPRESSION_COLUMN = "cron_expression";

    /**
     * The name of the cron time zone column in the database.
     */
    public static final String CRON_TIME_ZONE_COLUMN = "cron_time_zone";

    /**
     * The primary key and unique name for this configuration profile (e.g.,
     * "default", "test").
     */
    @Id
    @NotNull
    @Column(name = HubConfig.NAME_COLUMN)
    private String name;

    /**
     * A flag to indicate whether this configuration profile is active. A null
     * value can be treated as false.
     */
    @NotNull
    @Column(name = HubConfig.ENABLED_COLUMN, nullable = false)
    private Boolean enabled = false;

    /**
     * The API key for accessing the YouTube Data API.
     */
    @Column(name = HubConfig.YOUTUBE_API_KEY_COLUMN)
    private String youtubeApiKey;

    /**
     * The client ID for accessing the downloader REST API.
     */
    @Column(name = HubConfig.CLIENT_ID_COLUMN)
    private String clientId;

    /**
     * The client secret for accessing the downloader REST API.
     */
    @Column(name = HubConfig.CLIENT_SECRET_COLUMN)
    private String clientSecret;

    /**
     * A flag to indicate whether the fetch scheduler should start
     * automatically.
     */
    @Column(name = HubConfig.AUTO_START_FETCH_SCHEDULER_COLUMN)
    private Boolean autoStartFetchScheduler = false;

    /**
     * The type of scheduler to use (e.g., "FIXED_RATE" or "CRON").
     */
    @Enumerated(EnumType.STRING)
    @Column(name = HubConfig.SCHEDULER_TYPE_COLUMN)
    private SchedulerType schedulerType;

    /**
     * The fixed rate for the scheduler in milliseconds.
     */
    @Column(name = HubConfig.FIXED_RATE_COLUMN)
    private Long fixedRate = 86400000L;

    /**
     * The cron expression for the scheduler.
     */
    @Column(name = HubConfig.CRON_EXPRESSION_COLUMN)
    private String cronExpression;

    /**
     * The time zone for the cron expression (e.g., "Asia/Taipei", "UTC").
     */
    @Column(name = HubConfig.CRON_TIME_ZONE_COLUMN)
    private String cronTimeZone;
}
