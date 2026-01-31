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
package ch.lin.authentication.service.backend.api.app.service.command;

import lombok.Builder;
import lombok.Value;

/**
 * Command object for partially updating an existing
 * {@link ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig}.
 * <p>
 * This class is an immutable data carrier that transfers data from the
 * controller to the service layer. Fields are optional to allow clients to
 * update only specific properties of a configuration.
 */
@Value
@Builder
public class UpdateConfigCommand {

    /**
     * The unique name of the configuration to update. This field is mandatory.
     */
    String name;
    /**
     * An optional new value for the 'enabled' state. If null, the existing
     * value is not changed.
     */
    Boolean enabled;
    /**
     * An optional new value for the JWT access token expiration in
     * milliseconds. If null, the existing value is not changed.
     */
    Long jwtExpiration;
    /**
     * An optional new value for the JWT refresh token expiration in
     * milliseconds. If null, the existing value is not changed.
     */
    Long jwtRefreshExpiration;

    /**
     * An optional new value for the issuer URI to be included in the 'iss'
     * claim of the JWT. If null, the existing value is not changed.
     */
    String jwtIssuerUri;
}
