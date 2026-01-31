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

import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the request body for updating a YouTube video item, typically
 * after a download attempt.
 * <p>
 * This DTO is used by the
 * {@link ch.lin.youtube.hub.backend.api.controller.ItemController#updateItemFileInfo(String, UpdateItemRequest)}
 * endpoint to update properties of an existing item, such as its file size,
 * local path, and status.
 * <p>
 * Example JSON request body:
 * <pre>
 * {@code
 * {
 *   "downloadTaskId": "task-123",
 *   "fileSize": 12345678,
 *   "filePath": "/path/to/downloaded/video.mp4",
 *   "status": "DOWNLOADED"
 * }
 * }
 * </pre>
 */
@Getter
@Setter
public class UpdateItemRequest {

    /**
     * The ID of the download task. If provided, the specific DownloadInfo
     * record is updated. If null, a new DownloadInfo record is created.
     */
    private String downloadTaskId;

    /**
     * The new file size of the video in bytes. This value cannot be negative.
     */
    @Min(value = 0, message = "fileSize cannot be negative.")
    private Long fileSize;

    /**
     * The local file path where the downloaded video is stored.
     */
    private String filePath;

    /**
     * The new processing status of the item (e.g., DOWNLOADED, FAILED).
     *
     * @see ProcessingStatus
     */
    private ProcessingStatus status;

}
