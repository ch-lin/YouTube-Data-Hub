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
@EqualsAndHashCode(of = {"name", "enabled", "youtubeApiKey", "clientId",
    "clientSecret"}, callSuper = false)
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
}
