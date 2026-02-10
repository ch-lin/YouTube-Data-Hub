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

import static ch.lin.youtube.hub.backend.api.domain.model.Playlist.PLAYLIST_ID_COLUMN;
import static ch.lin.youtube.hub.backend.api.domain.model.Playlist.PLAYLIST_ID_INDEX;
import static ch.lin.youtube.hub.backend.api.domain.model.Playlist.TABLE_NAME;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
 * Represents a YouTube playlist as a JPA entity.
 */
@Table(name = TABLE_NAME, indexes = {
    @Index(name = BaseEntity.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = PLAYLIST_ID_INDEX, columnList = PLAYLIST_ID_COLUMN)}, uniqueConstraints = {
    @UniqueConstraint(columnNames = PLAYLIST_ID_COLUMN)})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"playlistId"}, callSuper = false)
public class Playlist extends BaseEntity {

    /**
     * The name of the playlist table in the database.
     */
    public static final String TABLE_NAME = "playlist";

    /**
     * The name of the index for the playlist ID column.
     */
    public static final String PLAYLIST_ID_INDEX = "playlist_playlistid_index";

    /**
     * The name of the playlist ID column in the database.
     */
    public static final String PLAYLIST_ID_COLUMN = "playlist_id";

    /**
     * The name of the title column in the database.
     */
    public static final String TITLE_COLUMN = "title";

    /**
     * The name of the processed at column in the database.
     */
    public static final String PROCESSED_AT = "processed_at";

    /**
     * The name of the last page token column in the database.
     */
    public static final String LAST_PAGE_TOKEN_COLUMN = "last_page_token";

    /**
     * The name of the channel column, used for the foreign key relationship.
     */
    public static final String CHANNEL_COLUMN = "channel";

    /**
     * The name of the foreign key constraint for the channel relationship.
     */
    public static final String FK_PLAYLIST_CHANNEL = "fk_playlist_channel";

    /**
     * The unique identifier of the YouTube playlist (e.g., "PL_000_...). This
     * serves as the business key and is constrained to be unique in the
     * database.
     */
    @NotNull
    @Column(name = Playlist.PLAYLIST_ID_COLUMN, nullable = false)
    private String playlistId;

    /**
     * The title of the playlist.
     */
    @NotNull
    @Column(name = Playlist.TITLE_COLUMN, nullable = false)
    private String title;

    /**
     * The timestamp when the playlist was last processed or synchronized. Can
     * be null if it has never been processed.
     */
    @Column(name = Playlist.PROCESSED_AT, columnDefinition = "TIMESTAMP")
    private OffsetDateTime processedAt;

    /**
     * The token of the next page to be processed. This serves as a checkpoint
     * for resuming interrupted jobs.
     */
    @Column(name = Playlist.LAST_PAGE_TOKEN_COLUMN)
    private String lastPageToken;

    /**
     * The {@link Channel} to which this playlist belongs. This establishes a
     * non-nullable, many-to-one relationship.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = Playlist.CHANNEL_COLUMN, referencedColumnName = Channel.ID_COLUMN, nullable = false, updatable = false, foreignKey = @ForeignKey(name = FK_PLAYLIST_CHANNEL))
    private Channel channel;

    /**
     * The set of {@link Item} entities contained in this playlist. This is a
     * one-to-many relationship managed by the {@code Item} entity. Changes are
     * cascaded, and orphaned items are removed automatically.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = Item.PLAYLIST_COLUMN, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Item> items;

    /**
     * Adds an item to this playlist.
     * <p>
     * This is a convenience helper method to ensure the bidirectional
     * relationship between {@code Playlist} and {@code Item} is correctly
     * maintained on both sides. It adds the item to the playlist's collection
     * and sets this playlist on the item.
     *
     * @param item The item to add.
     */
    public void addItem(Item item) {
        if (this.items == null) {
            this.items = new HashSet<>();
        }
        this.items.add(item);
        item.setPlaylist(this);
    }

    /**
     * Removes an item from this playlist.
     * <p>
     * This is a convenience helper method to ensure the bidirectional
     * relationship between {@code Playlist} and {@code Item} is correctly
     * managed on both sides. It removes the item from the playlist's collection
     * and unsets the playlist reference on the item.
     *
     * @param item The item to remove.
     */
    public void removeItem(Item item) {
        if (this.items != null) {
            this.items.remove(item);
            item.setPlaylist(null);
        }
    }
}
