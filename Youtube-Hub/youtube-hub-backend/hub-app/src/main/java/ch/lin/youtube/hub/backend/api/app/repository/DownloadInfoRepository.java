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
package ch.lin.youtube.hub.backend.api.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.youtube.hub.backend.api.domain.model.DownloadInfo;

/**
 * Spring Data JPA repository for {@link DownloadInfo} entities.
 * <p>
 * This interface provides the mechanism for standard CRUD (Create, Read,
 * Update, Delete) operations on download information records by extending
 * {@link JpaRepository}.
 */
@Repository
public interface DownloadInfoRepository extends JpaRepository<DownloadInfo, Long> {

    /**
     * Deletes all records from the 'download_info' table.
     * <p>
     * This custom bulk delete operation is intended for cleanup or reset
     * purposes. It is more efficient than standard JPA deletion because it
     * bypasses the persistence context and issues a direct SQL `DELETE`
     * statement.
     */
    @Transactional
    @Modifying // Indicates a data-changing query
    @Query("DELETE FROM DownloadInfo c")
    void cleanTable();

    /**
     * Resets the sequence for the 'download_info' table's primary key.
     * <p>
     * <b>Note:</b> This is a native SQL query for resetting the
     * {@code AUTO_INCREMENT} value on the primary key column, commonly used in
     * databases like MySQL. It should be used with caution, typically after
     * cleaning the table, to reset ID generation.
     */
    @Transactional
    @Modifying
    @Query(value = "ALTER TABLE download_info AUTO_INCREMENT = 1", nativeQuery = true)
    void resetSequence();

    /**
     * Deletes the download info entity associated with the given download task
     * ID.
     * <p>
     * This method finds and removes the record where the {@code downloadTaskId}
     * matches the provided value.
     *
     * @param downloadTaskId the unique identifier of the download task
     */
    @Transactional
    void deleteByDownloadTaskId(String downloadTaskId);
}
