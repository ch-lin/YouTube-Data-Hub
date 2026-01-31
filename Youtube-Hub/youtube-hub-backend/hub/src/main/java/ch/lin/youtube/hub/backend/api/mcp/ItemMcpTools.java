package ch.lin.youtube.hub.backend.api.mcp;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import ch.lin.youtube.hub.backend.api.app.service.ItemService;
import ch.lin.youtube.hub.backend.api.app.service.model.ItemUpdateResult;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import ch.lin.youtube.hub.backend.api.dto.ItemResponse;

@Component
public class ItemMcpTools {

    private final ItemService itemService;

    public ItemMcpTools(ItemService itemService) {
        this.itemService = itemService;
    }

    // Define query parameter Schema (using Java Record)
    public record ItemFilterRequest(
            Boolean notDownloaded,
            Boolean filterNoFileSize,
            String liveBroadcastContent,
            Boolean filterNoTag,
            Boolean filterDeleted,
            Boolean scheduledTimeIsInThePast,
            List<String> channelIds
            ) {

    }

    /**
     * Tool corresponding to GET /items, allowing LLM to search and filter video
     * items.
     */
    @Tool(name = "get-video-items", description = "Searches and filters the list of YouTube video items. Parameters: 1. notDownloaded: If true, prioritizes returning items that have not been downloaded and are processable (status is NEW/PENDING/DOWNLOADING/FAILED, and is a standard video or ended live stream). 2. liveBroadcastContent: 'none' means standard video, 'not_none' means live stream or premiere. 3. scheduledTimeIsInThePast: If true, returns only items with a scheduled start time in the past (commonly used to find ended live streams); if false, returns only future items. 4. filterNoFileSize: If true, excludes items with file size 0. 5. filterNoTag: If true, excludes items without tags. 6. filterDeleted: If true, returns only deleted items. 7. channelIds: List of specific channel IDs.")
    public List<ItemResponse> getVideoItems(ItemFilterRequest request) {
        // Call the original Service
        List<Item> items = itemService.getItems(
                request.notDownloaded(),
                request.filterNoFileSize(),
                request.liveBroadcastContent(),
                request.scheduledTimeIsInThePast(), // Note: parameter order must match Service
                request.filterNoTag(),
                request.filterDeleted(),
                request.channelIds()
        );

        return items.stream()
                .map(ItemResponse::new)
                .collect(Collectors.toList());
    }

    // Define update parameter Schema
    public record UpdateItemRequest(
            String videoId,
            String downloadTaskId,
            Long fileSize,
            String filePath,
            ProcessingStatus status
            ) {

    }

    /**
     * Tool corresponding to PATCH /items/{videoId}, allowing LLM to update
     * video file info (e.g., after download).
     */
    @Tool(name = "update_item_file_info", description = "Updates file information for a video item, including file path, size, and download status. Typically called after a download is complete.")
    public ItemResponse updateItemFileInfo(UpdateItemRequest request) {
        ItemUpdateResult result = itemService.updateItemFileInfo(
                request.videoId(),
                request.downloadTaskId(),
                request.fileSize(),
                request.filePath(),
                request.status()
        );
        return new ItemResponse(result.updatedItem());
    }
}
