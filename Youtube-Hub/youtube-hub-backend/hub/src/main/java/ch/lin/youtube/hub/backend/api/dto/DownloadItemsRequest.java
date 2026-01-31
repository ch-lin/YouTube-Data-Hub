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
import lombok.Setter;

/**
 * Represents the request body for triggering the download of specific video
 * items.
 * <p>
 * This DTO is used by the
 * {@link ch.lin.youtube.hub.backend.api.controller.YoutubeHubController#downloadItems(DownloadItemsRequest, String)}
 * endpoint. It specifies which videos to download and optionally which download
 * configuration to use.
 * <p>
 * Example JSON request body:
 * <pre>
 * {@code
 * {
 *   "configName": "default",
 *   "videoIds": ["dQw4w9WgXcQ", "C0DPdy98e4c"]
 * }
 * }
 * </pre>
 */
@Getter
@Setter
public class DownloadItemsRequest {

    /**
     * The name of the download configuration to use for this request. If not
     * provided, the system will use the currently enabled configuration.
     */
    private String configName;

    /**
     * A list of unique YouTube video IDs to be downloaded. This list cannot be
     * empty.
     */
    @NotEmpty(message = "videoIds cannot be empty.")
    private List<String> videoIds;
}
