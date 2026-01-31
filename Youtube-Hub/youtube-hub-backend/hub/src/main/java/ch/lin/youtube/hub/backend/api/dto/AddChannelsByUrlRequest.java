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

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the request body for adding new YouTube channels by providing
 * their URLs.
 * <p>
 * This DTO is used in the
 * {@link ch.lin.youtube.hub.backend.api.controller.ChannelController#addChannelsByUrl(AddChannelsByUrlRequest)}
 * method to capture an optional API key and a list of channel URLs. Example in
 * a cURL request body:
 *
 * <pre>
 * {@code
 * {
 *   "apiKey": "your-optional-api-key",
 *   "configName": "optional-config-name",
 *   "urls": ["https://www.youtube.com/@channelHandle", "https://www.youtube.com/channel/UC-lHJZR3Gqxm24_Vd_AJ5Yw"]
 * }
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
public class AddChannelsByUrlRequest {

    /**
     * The YouTube Data API key for making requests. If not provided, the system
     * will attempt to use a configured default key from the database or
     * application properties.
     */
    private String apiKey;

    /**
     * The name of the configuration to use for resolving the API key. If
     * provided, the system will attempt to find a configuration with this name.
     * If not provided, the default configuration will be used.
     */
    private String configName;

    /**
     * A list of YouTube channel URLs to be added to the system. This list must
     * not be empty.
     */
    @NotEmpty(message = "URLs list cannot be empty.")
    private List<String> urls;
}
