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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;

/**
 * Spring Data JPA repository for {@link HubConfig} entities.
 * <p>
 * This interface provides standard CRUD operations and custom finder methods
 * for managing hub configurations in the database.
 */
@Repository
public interface HubConfigRepository extends JpaRepository<HubConfig, String> {

    /**
     * Deletes all records from the {@code HubConfig} entity table.
     * <p>
     * This custom bulk delete operation is intended for cleanup or reset
     * purposes. It is more efficient than standard JPA deletion because it
     * bypasses the persistence context and issues a direct SQL `DELETE`
     * statement.
     */
    @Transactional
    @Modifying // Indicates a data-changing query
    @Query("DELETE FROM HubConfig d")
    void cleanTable();

    /**
     * Finds all configurations that are currently enabled.
     *
     * @return A list of all {@link HubConfig} entities where the 'enabled'
     * property is true.
     */
    List<HubConfig> findAllByEnabledTrue();

    /**
     * Finds the first configuration that is currently enabled. Since typically
     * only one configuration should be active, this is the standard method for
     * retrieving the active one.
     *
     * @return An {@link Optional} containing the enabled {@link HubConfig}, or
     * empty if none are enabled.
     */
    Optional<HubConfig> findFirstByEnabledTrue();

    /**
     * Finds a configuration by its unique name.
     *
     * @param name The name of the configuration to find.
     * @return An {@link Optional} containing the {@link HubConfig} if found,
     * otherwise empty.
     */
    Optional<HubConfig> findByName(String name);
}
