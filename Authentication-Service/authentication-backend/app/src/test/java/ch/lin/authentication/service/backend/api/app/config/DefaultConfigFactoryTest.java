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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;

class DefaultConfigFactoryTest {

    private final DefaultConfigFactory factory = new DefaultConfigFactory();

    @Test
    @DisplayName("Should create AuthenticationConfig from properties")
    void create_Success() {
        // Given
        AuthenticationDefaultProperties properties = new AuthenticationDefaultProperties();
        properties.setName("test-config");
        properties.setEnabled(true);
        properties.setJwtExpiration(1000L);
        properties.setJwtRefreshExpiration(2000L);
        properties.setJwtIssuerUri("http://test.com");

        // When
        AuthenticationConfig config = factory.create(properties);

        // Then
        assertNotNull(config);
        assertEquals(properties.getName(), config.getName());
        assertEquals(properties.getEnabled(), config.getEnabled());
        assertEquals(properties.getJwtExpiration(), config.getJwtExpiration());
        assertEquals(properties.getJwtRefreshExpiration(), config.getJwtRefreshExpiration());
        assertEquals(properties.getJwtIssuerUri(), config.getJwtIssuerUri());
    }

    @Test
    @DisplayName("Should throw NullPointerException when properties is null")
    @SuppressWarnings("null")
    void create_NullInput() {
        // The @NonNull annotation from Lombok generates a null check
        NullPointerException exception = assertThrows(NullPointerException.class, () -> factory.create(null));
        assertNotNull(exception);
    }
}
