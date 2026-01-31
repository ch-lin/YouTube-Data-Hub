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
package ch.lin.youtube.hub.backend.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.lin.platform.api.ApiResponse;
import ch.lin.youtube.hub.backend.api.app.service.ItemService;
import ch.lin.youtube.hub.backend.api.app.service.model.ItemUpdateResult;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.dto.ItemResponse;
import ch.lin.youtube.hub.backend.api.dto.UpdateItemRequest;
import jakarta.validation.Valid;

/**
 * REST controller for managing video items.
 * <p>
 * This controller provides API endpoints for retrieving, updating, and deleting
 * video item data. It delegates the core business logic to the
 * {@link ItemService}.
 */
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Retrieves a list of items based on optional filtering criteria.
     *
     * @param notDownloaded If true, filters for items that have not been
     * downloaded (status is not 'DOWNLOADED' or 'MANUALLY_DOWNLOADED').
     * @param filterNoFileSize If true, filters for items where the file size is
     * zero.
     * @param liveBroadcastContent A string to filter by live broadcast status
     * (e.g., "none", "live", "upcoming").
     * @param filterNoTag If true, filters for items that have no associated
     * tags.
     * @param filterDeleted If true, filters for items that are marked as
     * deleted.
     * @param scheduledTimeIsInThePast If true, filters for items with a
     * scheduled start time that is in the past.
     * @param channelIds An optional list of channel IDs to scope the results.
     * @return A {@link ResponseEntity} with an HTTP 200 OK status, containing a
     * list of {@link ItemResponse} objects matching the criteria.
     * <p>
     * Example cURL requests:      <pre>
     * {@code
     * # Get all items
     * curl -X GET http://localhost:8080/items
     *
     * # Get only not-downloaded items
     * curl -X GET http://localhost:8080/items?notDownloaded=true
     *
     * # Get items with no file size that are standard videos
     * curl -X GET "http://localhost:8080/items?filterNoFileSize=true&liveBroadcastContent=none"
     *
     * # Get items that have not been tagged yet
     * curl -X GET "http://localhost:8080/items?filterNoTag=true"
     * }
     * </pre>
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ItemResponse>> getAllItems(
            @RequestParam(name = "notDownloaded", required = false) final Boolean notDownloaded,
            @RequestParam(name = "filterNoFileSize", required = false) final Boolean filterNoFileSize,
            @RequestParam(name = "liveBroadcastContent", required = false) final String liveBroadcastContent,
            @RequestParam(name = "filterNoTag", required = false) final Boolean filterNoTag,
            @RequestParam(name = "filterDeleted", required = false) final Boolean filterDeleted,
            @RequestParam(name = "scheduledTimeIsInThePast", required = false) final Boolean scheduledTimeIsInThePast,
            @RequestParam(name = "channelIds", required = false) final List<String> channelIds) {
        List<Item> items = itemService.getItems(
                notDownloaded, filterNoFileSize, liveBroadcastContent, scheduledTimeIsInThePast,
                filterNoTag, filterDeleted, channelIds);
        List<ItemResponse> response = items.stream()
                .map(ItemResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the file information for a specific video item. This is typically
     * called after a video has been downloaded to record its file size and
     * location.
     *
     * @param videoId The unique ID of the video item to update.
     * @param request The request body containing the download task ID, file
     * size, file path, and new status.
     * @return A {@link ResponseEntity} with an HTTP 200 OK status, containing
     * an {@link ApiResponse} that wraps the updated {@link ItemResponse} and
     * any warnings generated during the update.
     * <p>
     * Example cURL request:      <pre>
     * {@code
     * curl -X PATCH http://localhost:8080/items/HEvJJb-4B7Q \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "downloadTaskId": "task-123",
     *   "fileSize": 12345678,
     *   "filePath": "/path/to/downloaded/video.mp4",
     *   "status": "DOWNLOADED"
     * }'
     * }
     * </pre>
     */
    @PatchMapping(value = "/{videoId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ItemResponse>> updateItemFileInfo(@PathVariable final String videoId,
            @Valid @RequestBody final UpdateItemRequest request) {
        ItemUpdateResult result = itemService.updateItemFileInfo(videoId, request.getDownloadTaskId(),
                request.getFileSize(), request.getFilePath(), request.getStatus());
        ApiResponse<ItemResponse> response = ApiResponse.success(new ItemResponse(result.updatedItem()),
                result.warnings());
        return ResponseEntity.ok(response);
    }

}
