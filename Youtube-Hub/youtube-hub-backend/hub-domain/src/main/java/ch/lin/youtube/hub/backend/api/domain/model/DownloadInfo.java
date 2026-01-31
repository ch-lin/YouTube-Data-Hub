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

import static ch.lin.youtube.hub.backend.api.domain.model.DownloadInfo.TABLE_NAME;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents download information for a video item, stored as a JPA entity.
 */
@Table(name = TABLE_NAME, indexes = {
    @Index(name = BaseEntity.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = DownloadInfo.DOWNLOAD_TASK_ID_COLUMN, columnList = DownloadInfo.DOWNLOAD_TASK_ID_COLUMN)
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"downloadTaskId", "fileSize", "filePath"}, callSuper = false)
public class DownloadInfo extends BaseEntity {

    /**
     * The name of the download info table in the database.
     */
    public static final String TABLE_NAME = "download_info";

    /**
     * The name of the download task ID column in the database.
     */
    public static final String DOWNLOAD_TASK_ID_COLUMN = "download_task_id";

    /**
     * The name of the file size column in the database.
     */
    public static final String FILE_SIZE_COLUMN = "file_size";

    /**
     * The name of the file path column in the database.
     */
    public static final String FILE_PATH_COLUMN = "file_path";

    /**
     * The name of the item column in the database, used for the foreign key.
     */
    public static final String ITEM_COLUMN = "item_id";

    /**
     * The name of the foreign key constraint for the item relationship.
     */
    public static final String FK_DOWNLOAD_INFO_ITEM = "fk_download_info_item";

    /**
     * An identifier for the download task, typically from an external
     * downloader service. This may be null if the video was not downloaded via
     * an integrated task manager.
     */
    @Column(name = DOWNLOAD_TASK_ID_COLUMN)
    private String downloadTaskId;

    /**
     * The size of the downloaded video file in bytes.
     */
    @Column(name = FILE_SIZE_COLUMN)
    private long fileSize;

    /**
     * The local path where the downloaded video file is stored.
     */
    @Column(name = FILE_PATH_COLUMN)
    private String filePath;

    /**
     * The {@link Item} to which this download information belongs. This
     * establishes a non-nullable, many-to-one relationship from
     * {@code DownloadInfo} to {@code Item}.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = ITEM_COLUMN, referencedColumnName = Item.ID_COLUMN, nullable = false, updatable = false, foreignKey = @ForeignKey(name = FK_DOWNLOAD_INFO_ITEM))
    private Item item;
}
