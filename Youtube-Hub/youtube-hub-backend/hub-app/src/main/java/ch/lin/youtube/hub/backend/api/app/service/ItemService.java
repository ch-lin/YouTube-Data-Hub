package ch.lin.youtube.hub.backend.api.app.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ch.lin.youtube.hub.backend.api.app.service.model.ItemUpdateResult;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;

/**
 * Defines the service layer contract for managing YouTube video items
 * ({@link Item} entities).
 * <p>
 * This interface outlines the core business logic for retrieving items with
 * complex filtering, updating item information after a download, and performing
 * cleanup operations.
 */
public interface ItemService {

    /**
     * Performs cleanup operations on item-related tables.
     * <p>
     * This is a destructive operation that typically involves deleting all
     * records from the 'item' and related tables and resetting their primary
     * key sequences. It is intended for system reset or testing purposes.
     */
    void cleanup();

    /**
     * Retrieves a list of {@link Item} entities based on a dynamic set of
     * filter criteria.
     *
     * @param notDownloaded If true, returns only items that have not been
     * downloaded.
     * @param filterNoFileSize If true, excludes items where the file size is
     * zero or null.
     * @param liveBroadcastContent Filters by the live broadcast content status
     * (e.g., 'live', 'upcoming', 'none').
     * @param pastOnly If true, returns only items with a published date in the
     * past.
     * @param filterNoTag If true, excludes items that do not have an associated
     * tag.
     * @param filterDeleted If true, returns only items that are marked as
     * deleted.
     * @param channelIds A list of channel IDs to filter by. If provided, only
     * items from these channels are returned.
     * @param pageable The pagination information.
     * @return A page of {@link Item} entities matching the filter criteria.
     */
    Page<Item> getItems(Boolean notDownloaded, Boolean filterNoFileSize, String liveBroadcastContent,
            Boolean pastOnly, Boolean filterNoTag, Boolean filterDeleted, List<String> channelIds, Pageable pageable);

    /**
     * Updates an item's information after a download attempt.
     * <p>
     * This method is typically called by a background process to record the
     * outcome of a download task. It also checks for potential duplicate files.
     *
     * @param videoId The unique YouTube video ID of the item to update.
     * @param downloadTaskId The ID of the download task, for tracking purposes.
     * @param fileSize The size of the downloaded file in bytes.
     * @param filePath The local path where the file was saved.
     * @param status The final {@link ProcessingStatus} of the download task.
     * @return An {@link ItemUpdateResult} containing the updated item and a
     * list of any warnings (e.g., potential duplicates).
     */
    ItemUpdateResult updateItemFileInfo(String videoId, String downloadTaskId, Long fileSize, String filePath, ProcessingStatus status);
}
