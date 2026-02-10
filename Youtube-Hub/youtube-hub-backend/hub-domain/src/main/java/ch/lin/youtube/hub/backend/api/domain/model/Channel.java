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

import java.util.Set;

import static ch.lin.youtube.hub.backend.api.domain.model.Channel.CHANNEL_ID_COLUMN;
import static ch.lin.youtube.hub.backend.api.domain.model.Channel.TABLE_NAME;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
 * Represents a YouTube channel as a JPA entity.
 */
@Table(name = TABLE_NAME, indexes = {
    @Index(name = BaseEntity.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = CHANNEL_ID_COLUMN, columnList = CHANNEL_ID_COLUMN)}, uniqueConstraints = {
    @UniqueConstraint(columnNames = CHANNEL_ID_COLUMN)})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"channelId"}, callSuper = false)
public class Channel extends BaseEntity {

    /**
     * The name of the channel table in the database.
     */
    public static final String TABLE_NAME = "channel";

    /**
     * The name of the channel ID column in the database.
     */
    public static final String CHANNEL_ID_COLUMN = "channel_id";

    /**
     * The name of the title column in the database.
     */
    public static final String TITLE_COLUMN = "title";

    /**
     * The name of the handle column in the database.
     */
    public static final String HANDLE_COLUMN = "handle";

    /**
     * The unique identifier of the YouTube channel (e.g.,
     * "UC_x5XG1OV2P6uZZ5FSM9Ttw"). This serves as the business key and is
     * constrained to be unique in the database.
     */
    @NotNull
    @Column(name = Channel.CHANNEL_ID_COLUMN, nullable = false, unique = true)
    private String channelId;

    /**
     * The title of the YouTube channel.
     */
    @NotNull
    @Column(name = Channel.TITLE_COLUMN, nullable = false)
    private String title;

    /**
     * The handle of the YouTube channel.
     */
    @Column(name = Channel.HANDLE_COLUMN, unique = true)
    private String handle;

    /**
     * The set of playlists belonging to this channel. This is a one-to-many
     * relationship, managed by the {@link Playlist} entity. Changes are
     * cascaded, and orphaned playlists are removed automatically.
     */
    @OneToMany(mappedBy = Playlist.CHANNEL_COLUMN, fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Playlist> playlists;
}
