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

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.youtube.hub.backend.api.domain.model.Tag;

/**
 * Spring Data JPA repository for {@link Tag} entities.
 * <p>
 * This interface provides the mechanism for CRUD operations on tags, leveraging
 * the standard methods provided by {@link JpaRepository}.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Deletes all records from the 'tag' table.
     * <p>
     * This custom bulk delete operation is intended for cleanup or reset
     * purposes. It is more efficient than standard JPA deletion because it
     * bypasses the persistence context and issues a direct SQL `DELETE`
     * statement.
     */
    @Transactional
    @Modifying // Indicates a data-changing query
    @Query("DELETE FROM Tag t")
    void cleanTable();

    /**
     * Resets the sequence for the 'tag' table's primary key.
     * <p>
     * <b>Note:</b> This is a native SQL query for resetting the
     * {@code AUTO_INCREMENT} value on the primary key column, commonly used in
     * databases like MySQL. It should be used with caution, typically after
     * cleaning the table, to reset ID generation.
     */
    @Transactional
    @Modifying
    @Query(value = "ALTER TABLE tag AUTO_INCREMENT = 1", nativeQuery = true)
    void resetSequence();

    /**
     * Finds a {@link Tag} by its unique name (case-sensitive).
     *
     * @param name The non-null, unique name of the tag.
     * @return An {@link Optional} containing the found tag, or an empty
     * Optional if no tag matches the name.
     */
    Optional<Tag> findByName(String name);

    /**
     * Finds all tags whose names are present as a substring within the given
     * file path.
     * <p>
     * This query is used to automatically associate an item with a tag based on
     * its download location. For example, if a tag with the name "Tutorials"
     * exists, a file path like "/downloads/tech/Tutorials/video.mp4" would
     * match.
     *
     * @param filePath The file path string to search within.
     * @return A list of matching {@link Tag} entities. The list will be empty
     * if no tags are found.
     */
    @Query("SELECT t FROM Tag t WHERE :filePath LIKE CONCAT('%', t.name, '%')")
    List<Tag> findTagsWithinFilePath(@Param("filePath") String filePath);

    /**
     * Deletes a tag by its unique name.
     * <p>
     * This is a derived delete query provided by Spring Data JPA. It should be
     * executed within a transactional context.
     *
     * @param name The name of the tag to delete.
     */
    void deleteByName(String name);

}
