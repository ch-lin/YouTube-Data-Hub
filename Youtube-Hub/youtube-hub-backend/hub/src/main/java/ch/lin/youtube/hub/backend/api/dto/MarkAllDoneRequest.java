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

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the optional request body for marking video items as "manually
 * downloaded".
 * <p>
 * This DTO is used by the
 * {@link ch.lin.youtube.hub.backend.api.controller.YoutubeHubController#markAllDone(MarkAllDoneRequest)}
 * endpoint. It allows the bulk update operation to be scoped to a specific list
 * of channels. If the request body is omitted, the operation applies to all
 * channels.
 * <p>
 * Example JSON request body:
 * <pre>
 * {@code
 * {
 *   "channelIds": ["UC-lHJZR3Gqxm24_Vd_AJ5Yw", "UC_x5XG1OV2P6uZZ5FSM9Ttw"]
 * }
 * }
 * </pre>
 */
@Getter
@Setter
public class MarkAllDoneRequest {

    /**
     * An optional list of channel IDs to scope the operation. If provided, only
     * unprocessed items belonging to these channels will be marked as "manually
     * downloaded". If this field is omitted or the list is empty, all
     * unprocessed items across all channels will be affected.
     */
    private List<String> channelIds;

}
