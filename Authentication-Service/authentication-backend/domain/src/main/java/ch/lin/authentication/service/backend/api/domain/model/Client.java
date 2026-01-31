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
package ch.lin.authentication.service.backend.api.domain.model;

import static ch.lin.authentication.service.backend.api.domain.model.Client.TABLE_NAME;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a machine client for service-to-service authentication. This class
 * is a JPA entity that maps to the "client" table in the database.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = TABLE_NAME)
public class Client {

    /**
     * The name of the database table for clients.
     */
    public static final String TABLE_NAME = "client";

    /**
     * The unique identifier for the client entity (primary key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * A human-readable name for the client application.
     */
    @NotNull
    @Column(nullable = false)
    private String clientName;

    /**
     * The public, unique identifier for the client, used for authentication.
     */
    @NotNull
    @Column(nullable = false, unique = true)
    private String clientId;

    /**
     * The confidential secret for the client, used for authentication. This
     * should be stored in a hashed format in the database.
     */
    @NotNull
    @Column(nullable = false)
    private String clientSecret;

    /**
     * The role assigned to the client, which determines its permissions.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
