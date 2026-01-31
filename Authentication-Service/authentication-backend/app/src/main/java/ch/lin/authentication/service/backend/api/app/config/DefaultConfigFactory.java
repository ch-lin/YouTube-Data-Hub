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

import org.springframework.stereotype.Component;

import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;

import lombok.NonNull;

/**
 * A factory component responsible for creating {@link AuthenticationConfig}
 * instances from {@link AuthenticationDefaultProperties}.
 * <p>
 * This decouples the creation of the default configuration entity from the
 * service logic that uses it, promoting better separation of concerns.
 */
@Component
public class DefaultConfigFactory {

    /**
     * Creates a new {@link AuthenticationConfig} instance based on the provided
     * default properties.
     *
     * @param properties The {@link AuthenticationDefaultProperties} object
     * containing the values for the new configuration. Must not be null.
     * @return A new, non-persisted {@link AuthenticationConfig} instance.
     */
    public AuthenticationConfig create(@NonNull AuthenticationDefaultProperties properties) {
        return new AuthenticationConfig(properties.getName(), properties.getEnabled(), properties.getJwtExpiration(), properties.getJwtRefreshExpiration(), properties.getJwtIssuerUri());
    }
}
