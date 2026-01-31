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
package ch.lin.authentication.service.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for creating a new
 * {@link ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig}.
 * <p>
 * This class represents the request body for the POST /configs endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateConfigRequest {

    @NotBlank(message = "Configuration name cannot be blank.")
    private String name;

    /**
     * Indicates whether the configuration is enabled. Defaults to false if not
     * provided.
     */
    private Boolean enabled = false;

    /**
     * The expiration time for the JWT access token in milliseconds.
     */
    @NotNull(message = "JWT expiration must be provided.")
    private Long jwtExpiration;

    /**
     * The expiration time for the JWT refresh token in milliseconds.
     */
    @NotNull(message = "JWT refresh expiration must be provided.")
    private Long jwtRefreshExpiration;

    /**
     * The issuer URI to be included in the 'iss' claim of the JWT.
     */
    @NotBlank(message = "JWT issuer URI cannot be blank.")
    private String jwtIssuerUri;

}
