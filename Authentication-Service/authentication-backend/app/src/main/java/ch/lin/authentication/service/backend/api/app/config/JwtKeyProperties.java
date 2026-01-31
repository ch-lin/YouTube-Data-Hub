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
package ch.lin.authentication.service.backend.api.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import lombok.Data;

/**
 * Configuration properties for locating the RSA key pair used for JWT signing.
 * <p>
 * This class is bound to properties prefixed with {@code jwt.key}. It uses
 * Spring's {@link Resource} abstraction to handle various location formats
 * (e.g., {@code classpath:}, {@code file:}).
 *
 * <h3>Example Configuration (application.yml):</h3>
 * <pre>{@code
 * jwt:
 *   key:
 *     public-key-location: "classpath:keys/public.pem"
 *     private-key-location: "classpath:keys/private.pem"
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "jwt.key")
public class JwtKeyProperties {

    /**
     * The location of the RSA public key file, used for verifying JWT
     * signatures.
     */
    private Resource publicKeyLocation;

    /**
     * The location of the RSA private key file, used for signing new JWTs.
     */
    private Resource privateKeyLocation;
}
