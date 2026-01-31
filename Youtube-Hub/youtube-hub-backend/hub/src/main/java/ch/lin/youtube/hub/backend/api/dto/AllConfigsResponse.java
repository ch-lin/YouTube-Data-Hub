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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the response for listing all available hub configurations.
 * <p>
 * This DTO is returned by the
 * {@link ch.lin.youtube.hub.backend.api.controller.ConfigsController#getAllConfigs()}
 * endpoint. It provides the name of the currently enabled configuration and a
 * complete list of all configuration names.
 * <p>
 * Example JSON response:
 *
 * <pre>
 * {@code
 * {
 *   "enabled": "default",
 *   "configs": ["default", "audio-only-config", "4k-video-config"]
 * }
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllConfigsResponse {

    /**
     * The name of the configuration that is currently enabled. Can be null if
     * no configuration is enabled.
     */
    private String enabledConfigName;
    /**
     * A list of names of all available configurations.
     */
    private List<String> allConfigNames;
}
