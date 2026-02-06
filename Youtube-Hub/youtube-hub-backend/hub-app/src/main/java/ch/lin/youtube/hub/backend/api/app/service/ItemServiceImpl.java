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
package ch.lin.youtube.hub.backend.api.app.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.TagRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.ItemUpdateResult;
import ch.lin.youtube.hub.backend.api.common.exception.ItemNotFoundException;
import ch.lin.youtube.hub.backend.api.domain.model.DownloadInfo;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.LiveBroadcastContent;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import ch.lin.youtube.hub.backend.api.domain.model.Tag;
import jakarta.persistence.criteria.JoinType;

/**
 * Service implementation for managing video items.
 * <p>
 * This class provides the concrete logic for operations defined in the
 * {@link ItemService} interface. It handles interactions with the
 * {@link ItemRepository} and {@link TagRepository} to retrieve, filter, and
 * update video item entities based on various criteria.
 */
@Service
public class ItemServiceImpl implements ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemServiceImpl.class);

    private final ItemRepository itemRepository;
    private final TagRepository tagRepository;

    /**
     * Constructs the service with its required repository dependencies.
     *
     * @param itemRepository The repository for {@link Item} entities.
     * @param tagRepository The repository for {@link Tag} entities.
     */
    public ItemServiceImpl(ItemRepository itemRepository, TagRepository tagRepository) {
        this.itemRepository = itemRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses a custom bulk delete query for efficiency. Note
     * that it does not reset the primary key sequence.
     */
    @Override
    public void cleanup() {
        itemRepository.cleanTable();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method supports complex filtering logic:
     * <ul>
     * <li><b>notDownloaded:</b> If true, this parameter takes precedence and
     * returns items that are considered available for download. An item is
     * available if its status is {@code NEW}, {@code PENDING},
     * {@code DOWNLOADING}, or {@code FAILED}, AND it is either a standard video
     * (liveBroadcastContent is {@code NONE}) or a past live stream/premiere
     * whose scheduled start time has passed.</li>
     * <li><b>filterNoFileSize:</b> If true (and notDownloaded is not used), it
     * filters for items where at least one download has a file size of 0.</li>
     * <li><b>liveBroadcastContent:</b> Can be "none" to fetch only standard
     * videos, or "not_none" to fetch past or upcoming live streams/premieres.
     * Other values are ignored.</li>
     * <li><b>pastOnly:</b> When used with
     * {@code liveBroadcastContent="not_none"}, this filters for streams that
     * are in the past (true) or future (false).</li>
     * <li><b>filterDeleted:</b> If true, filters for items that are marked as
     * deleted.</li>
     * </ul>
     */
    @Override
    @Transactional(readOnly = true)
    public List<Item> getItems(Boolean notDownloaded, Boolean filterNoFileSize, String liveBroadcastContent,
            Boolean pastOnly, Boolean filterNoTag, Boolean filterDeleted, List<String> channelIds) {
        final OffsetDateTime now = OffsetDateTime.now();

        // Eagerly fetch associated entities to prevent LazyInitializationException in
        // the controller layer when mapping to DTOs.
        Specification<Item> spec = (root, query, cb) -> {
            // This check prevents fetching on count queries, which would throw an
            // exception.te
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("playlist", JoinType.LEFT).fetch("channel", JoinType.LEFT);
                root.fetch("tag", JoinType.LEFT);
            }
            return cb.conjunction(); // Returns a predicate that is always true
        };
        if (notDownloaded != null && notDownloaded) {
            logger.info("Adding filter for not-downloaded items.");

            // Specification for items that are not yet successfully downloaded (i.e., are
            // NEW, PENDING, DOWNLOADING, FAILED, or WATCHED).
            Specification<Item> isNotDownloaded = (root, query, cb) -> root.get("status").in(ProcessingStatus.NEW,
                    ProcessingStatus.PENDING, ProcessingStatus.DOWNLOADING, ProcessingStatus.FAILED, ProcessingStatus.WATCHED);

            // Specification for condition 1: A standard video that is not a live stream or
            // premiere.
            Specification<Item> isStandardVideo = (root, query, cb) -> cb.equal(root.get("liveBroadcastContent"),
                    LiveBroadcastContent.NONE);

            // Specification for condition 2: A past live stream or premiere that should now
            // be available as a video.
            Specification<Item> isLiveOrUpcoming = (root, query, cb) -> cb.notEqual(root.get("liveBroadcastContent"),
                    LiveBroadcastContent.NONE);

            Specification<Item> scheduledTimeIsInThePast = (root, query, cb) -> {
                return cb.and(cb.isNotNull(root.get("scheduledStartTime")),
                        cb.lessThan(root.get("scheduledStartTime"), now));
            };

            Specification<Item> isProcessableLiveStream = isLiveOrUpcoming.and(scheduledTimeIsInThePast);

            // An item is considered non-processed if its 'processed' flag is false AND
            // (it's a standard video OR it's a past live stream that is now processable).
            spec = spec.and(isNotDownloaded.and(isStandardVideo.or(isProcessableLiveStream)));
        } else {
            if (filterNoFileSize != null && filterNoFileSize) {
                logger.info("Adding filter for items with no file size (fileSize = 0).");
                spec = spec.and((root, query, cb) -> {
                    if (query != null) {
                        query.distinct(true);
                    }
                    return cb.equal(root.join("downloadInfos").get("fileSize"), 0L);
                });
            }

            if (filterNoTag != null && filterNoTag) {
                logger.info("Adding filter for items with no tag.");
                spec = spec.and((root, query, cb) -> cb.isNull(root.get("tag")));
            }

            if (filterDeleted != null && filterDeleted) {
                logger.info("Adding filter for deleted items.");
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), ProcessingStatus.DELETED));
            }

            if (liveBroadcastContent != null && !liveBroadcastContent.isBlank()) {
                logger.info("Adding filter for liveBroadcastContent: {}", liveBroadcastContent);
                if ("none".equalsIgnoreCase(liveBroadcastContent)) {
                    spec = spec.and(
                            (root, query, cb) -> cb.equal(root.get("liveBroadcastContent"), LiveBroadcastContent.NONE));
                } else if ("not_none".equalsIgnoreCase(liveBroadcastContent)) {
                    // Filter for items that are not 'NONE' (e.g., 'LIVE' or 'UPCOMING')
                    Specification<Item> notNoneSpec = (root, query, cb) -> cb.notEqual(root.get("liveBroadcastContent"),
                            LiveBroadcastContent.NONE);
                    spec = spec.and(notNoneSpec);

                    if (pastOnly != null) {
                        logger.info("Adding filter for scheduled time. pastOnly={}", pastOnly);
                        Specification<Item> scheduledTimeSpec = (root, query, cb) -> {
                            var scheduledStartTime = root.get("scheduledStartTime");
                            var isNotNull = cb.isNotNull(scheduledStartTime);
                            if (pastOnly) {
                                // pastOnly is true -> scheduled time is in the past
                                return cb.and(isNotNull, cb.lessThan(scheduledStartTime.as(OffsetDateTime.class), now));
                            } else {
                                // pastOnly is false -> scheduled time is in the future (or now)
                                return cb.and(isNotNull, cb.greaterThanOrEqualTo(
                                        scheduledStartTime.as(OffsetDateTime.class), now));
                            }
                        };
                        spec = spec.and(scheduledTimeSpec);
                    }
                } else {
                    logger.warn("Invalid value for liveBroadcastContent filter: '{}'. Ignoring filter.",
                            liveBroadcastContent);
                }
            }
        }

        if (channelIds != null && !channelIds.isEmpty()) {
            logger.info("Adding filter for channel IDs: {}", channelIds);
            spec = spec.and((root, query, cb) -> root.get("playlist").get("channel").get("channelId").in(channelIds));
        }

        logger.info("Fetching items from the database with applied filters.");
        return itemRepository.findAll(spec);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation performs an "upsert" on the {@link DownloadInfo}: if
     * a {@code downloadTaskId} is provided, it updates the existing record;
     * otherwise, it creates a new one.
     * <p>
     * If a {@code filePath} is provided, it attempts to find a matching
     * {@link Tag} by name within the path and associates the most specific
     * (longest name) tag. It also checks for potential duplicate files based on
     * tag and file size, adding warnings to the result if any are found.
     *
     * @throws InvalidRequestException if the video ID is invalid or the file
     * size is negative.
     * @throws ItemNotFoundException if no item with the given video ID is
     * found.
     */
    @Override
    @Transactional
    public ItemUpdateResult updateItemFileInfo(String videoId, String downloadTaskId, Long fileSize, String filePath,
            ProcessingStatus status) {
        if (videoId == null || videoId.isBlank()) {
            throw new InvalidRequestException("Video ID cannot be null or empty.");
        }
        if (fileSize != null && fileSize < 0) {
            throw new InvalidRequestException("File size cannot be negative.");
        }
        // It's okay for filePath to be null or blank.

        Item item = itemRepository.findByVideoIdWithAssociations(videoId)
                .orElseThrow(() -> new ItemNotFoundException("Item with videoId " + videoId + " not found."));

        // First, handle the status update, which is independent of DownloadInfo.
        if (status != null) {
            item.setStatus(status);
        }

        List<String> warnings = new ArrayList<>();

        if (fileSize != null || filePath != null) {
            // Only interact with DownloadInfo if there's file information to update.
            DownloadInfo downloadInfo;
            if (downloadTaskId != null && !downloadTaskId.isBlank()) {
                // Find existing DownloadInfo by task ID
                downloadInfo = item.getDownloadInfos().stream()
                        .filter(di -> downloadTaskId.equals(di.getDownloadTaskId()))
                        .findFirst()
                        .orElseThrow(() -> new ItemNotFoundException(
                        "DownloadInfo with taskId " + downloadTaskId + " not found for item " + videoId));
            } else {
                // Try to find an existing DownloadInfo with no task ID (manual/external)
                downloadInfo = item.getDownloadInfos().stream()
                        .filter(di -> di.getDownloadTaskId() == null)
                        .findFirst()
                        .orElseGet(() -> {
                            // Create a new DownloadInfo for manual/external downloads
                            DownloadInfo newInfo = new DownloadInfo();
                            newInfo.setItem(item);
                            item.getDownloadInfos().add(newInfo);
                            return newInfo;
                        });
            }

            if (fileSize != null) {
                downloadInfo.setFileSize(fileSize);
            }
            if (filePath != null) {
                downloadInfo.setFilePath(filePath);
            }

            if (filePath != null && !filePath.isBlank()) {
                logger.info("Attempting to find and associate a tag for item with videoId {} from filePath.", videoId);
                List<Tag> foundTags = tagRepository.findTagsWithinFilePath(filePath);

                if (!foundTags.isEmpty()) {
                    // Find the tag with the longest name, as it's likely the most specific match.
                    Optional<Tag> bestMatch = foundTags.stream()
                            .max(Comparator.comparing(tag -> tag.getName().length()));

                    Tag tagToSet = bestMatch.get();
                    item.setTag(tagToSet);
                    logger.info("Found {} matching tag(s). Associated the most specific tag '{}' with item '{}'.",
                            foundTags.size(), tagToSet.getName(), videoId);

                    // Check for other items with the same tag and file size
                    if (fileSize != null && fileSize > 0) {
                        List<Item> duplicates = itemRepository.findByTagAndVideoIdNotAndDownloadInfosFileSize(tagToSet,
                                videoId, fileSize);
                        if (!duplicates.isEmpty()) {
                            String duplicateDetails = duplicates.stream()
                                    .map(duplicateItem -> String.format("'%s' (ID: %s)", duplicateItem.getTitle(),
                                    duplicateItem.getVideoId()))
                                    .reduce((s1, s2) -> s1 + ", " + s2).orElse("");
                            warnings.add(
                                    String.format(
                                            "Potential duplicate: %d other item(s) with the same tag and file size exist: %s",
                                            duplicates.size(), duplicateDetails));
                        }
                    }
                } else {
                    logger.info("No matching tags found for filePath: {}", filePath);
                    warnings.add("No matching tag found for the given file path.");
                }
            }
        }

        return ItemUpdateResult.of(item, warnings);
    }
}
