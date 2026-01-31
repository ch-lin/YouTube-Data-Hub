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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the request body for partially updating a
 * {@link ch.lin.youtube.hub.backend.api.domain.model.HubConfig}.
 * <p>
 * This DTO is used by the
 * {@link ch.lin.youtube.hub.backend.api.controller.ConfigsController#saveConfig(String, UpdateConfigRequest)}
 * endpoint. All fields are optional to allow for partial updates (PATCH).
 * <p>
 * Example JSON request body for a partial update:
 * <pre>
 * {@code
 * {
 *   "enabled": true,
 *   "youtubeApiKey": "new-api-key-..."
 * }
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateConfigRequest {

    /**
     * Sets whether this configuration should be the active one. If multiple
     * configurations are marked as enabled, the system's behavior may be
     * unpredictable.
     */
    private Boolean enabled;

    /**
     * Updates the YouTube Data API key for this configuration.
     */
    private String youtubeApiKey;

    /**
     * Updates the Google Cloud project's client ID for OAuth 2.0
     * authentication.
     */
    private String clientId;

    /**
     * Updates the Google Cloud project's client secret for OAuth 2.0
     * authentication.
     */
    private String clientSecret;
}
