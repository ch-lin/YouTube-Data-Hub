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
package ch.lin.youtube.hub.backend.api.domain.model;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

import static ch.lin.youtube.hub.backend.api.domain.model.Item.TABLE_NAME;
import static ch.lin.youtube.hub.backend.api.domain.model.Item.VIDEO_ID_COLUMN;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single video item (e.g., a YouTube video) within a playlist,
 * stored as a JPA entity.
 */
@Table(name = TABLE_NAME, indexes = {
    @Index(name = BaseEntity.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = VIDEO_ID_COLUMN, columnList = VIDEO_ID_COLUMN)}, uniqueConstraints = {
    @UniqueConstraint(columnNames = VIDEO_ID_COLUMN)})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"videoId", "title", "kind", "videoPublishedAt", "liveBroadcastContent", "scheduledStartTime",
    "tag", "thumbnailUrl", "status", "description"}, callSuper = false)
public class Item extends BaseEntity {

    /**
     * The name of the item table in the database.
     */
    public static final String TABLE_NAME = "item";

    /**
     * The name of the video ID column in the database.
     */
    public static final String VIDEO_ID_COLUMN = "video_id";

    /**
     * The name of the title column in the database.
     */
    public static final String TITLE_COLUMN = "title";

    /**
     * The name of the description column in the database.
     */
    public static final String DESCRIPTION_COLUMN = "description";

    /**
     * The name of the kind column in the database (e.g., "youtube#video").
     */
    public static final String KIND_COLUMN = "kind";

    /**
     * The name of the video published at column in the database.
     */
    public static final String VIDEO_PUBLISHED_AT_COLUMN = "video_published_at";

    /**
     * The name of the live broadcast content column in the database.
     */
    public static final String LIVE_BROADCAST_CONTENT_COLUMN = "live_broadcast_content";

    /**
     * The name of the scheduled start time column in the database.
     */
    public static final String SCHEDULED_START_TIME_COLUMN = "scheduled_start_time";

    /**
     * The name of the thumbnail URL column in the database.
     */
    public static final String THUMBNAIL_URL_COLUMN = "thumbnail_url";

    /**
     * The name of the playlist column in the database, used for the foreign
     * key.
     */
    public static final String PLAYLIST_COLUMN = "playlist";

    /**
     * The name of the foreign key constraint for the playlist relationship.
     */
    public static final String FK_ITEM_PLAYLIST = "fk_item_playlist";

    /**
     * The name of the tag column in the database, used for the foreign key.
     */
    public static final String TAG_COLUMN = "tag";

    /**
     * The name of the foreign key constraint for the tag relationship.
     */
    public static final String FK_ITEM_TAG = "fk_item_tag";

    /**
     * The name of the status column in the database.
     */
    public static final String STATUS_COLUMN = "status";

    /**
     * The unique identifier of the YouTube video (e.g., "dQw4w9WgXcQ"). This
     * serves as the business key and is constrained to be unique in the
     * database.
     */
    @NotNull
    @Column(name = Item.VIDEO_ID_COLUMN, nullable = false)
    private String videoId;

    /**
     * The title of the video.
     */
    @NotNull
    @Column(name = Item.TITLE_COLUMN, length = 300, nullable = false)
    private String title;

    /**
     * The description of the video. Stored as a large object (LOB) in the
     * database to accommodate long text.
     */
    @Lob
    @Column(name = Item.DESCRIPTION_COLUMN, columnDefinition = "MEDIUMTEXT")
    private String description;

    /**
     * The kind of the YouTube resource (e.g., "youtube#video").
     */
    @NotNull
    @Column(name = Item.KIND_COLUMN, nullable = false)
    private String kind;

    /**
     * The date and time when the video was published.
     */
    @NotNull
    @Column(name = Item.VIDEO_PUBLISHED_AT_COLUMN, columnDefinition = "TIMESTAMP", nullable = false)
    private OffsetDateTime videoPublishedAt;

    /**
     * The live broadcast status of the video.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = Item.LIVE_BROADCAST_CONTENT_COLUMN, nullable = false)
    private LiveBroadcastContent liveBroadcastContent;

    /**
     * The scheduled start time for a live broadcast. This is nullable and only
     * relevant for upcoming live streams.
     */
    @Column(name = Item.SCHEDULED_START_TIME_COLUMN, columnDefinition = "TIMESTAMP")
    private OffsetDateTime scheduledStartTime;

    /**
     * The URL of the thumbnail for the video.
     */
    @Column(name = Item.THUMBNAIL_URL_COLUMN)
    private String thumbnailUrl;

    /**
     * The processing status of the item, indicating its state in a workflow.
     * Defaults to {@link ProcessingStatus#NEW}.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = Item.STATUS_COLUMN, nullable = false)
    private ProcessingStatus status = ProcessingStatus.NEW;

    /**
     * The {@link Playlist} to which this item belongs. This establishes a
     * non-nullable, many-to-one relationship.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = Item.PLAYLIST_COLUMN, referencedColumnName = Playlist.ID_COLUMN, nullable = false, updatable = false, foreignKey = @ForeignKey(name = FK_ITEM_PLAYLIST))
    private Playlist playlist;

    /**
     * The optional {@link Tag} associated with this item. This establishes a
     * nullable, many-to-one relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = Item.TAG_COLUMN, referencedColumnName = Tag.ID_COLUMN, foreignKey = @ForeignKey(name = FK_ITEM_TAG))
    private Tag tag;

    /**
     * A set of {@link DownloadInfo} records associated with this item. This is
     * a one-to-many relationship where changes are cascaded and orphaned
     * {@code DownloadInfo} entities are removed.
     */
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<DownloadInfo> downloadInfos = new HashSet<>();
}
