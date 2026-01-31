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
package ch.lin.authentication.service.backend.api.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.authentication.service.backend.api.domain.model.User;

/**
 * Spring Data JPA repository for {@link User} entities.
 * <p>
 * This interface provides the mechanism for CRUD operations on users,
 * leveraging the standard methods provided by {@link JpaRepository}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Deletes all records from the 'user' table.
     * <p>
     * This is a custom bulk delete operation intended for cleanup or reset
     * purposes. It is more efficient than fetching and deleting entities one by
     * one.
     */
    @Transactional
    @Modifying // Indicates a data-changing query
    @Query("DELETE FROM User u")
    void cleanTable();

    /**
     * Resets the sequence for the 'user' table's primary key.
     * <p>
     * <b>Warning:</b> This is a native query specific to MySQL and is not
     * portable to other database systems (e.g., PostgreSQL, H2). It is intended
     * for use in testing or reset scenarios. For production-ready schema
     * management, consider using a database migration tool like Flyway or
     * Liquibase.
     */
    @Transactional
    @Modifying
    @Query(value = "ALTER TABLE user AUTO_INCREMENT = 1", nativeQuery = true)
    void resetSequence();

    /**
     * Finds a user by their unique email address.
     * <p>
     * This method is a key part of the authentication process, used by the
     * {@code UserDetailsService} to load a user by their principal name
     * (email).
     *
     * @param email The email address to search for.
     * @return An {@link Optional} containing the found user, or
     * {@link Optional#empty()} if no user was found with that email.
     */
    Optional<User> findByEmail(String email);
}
