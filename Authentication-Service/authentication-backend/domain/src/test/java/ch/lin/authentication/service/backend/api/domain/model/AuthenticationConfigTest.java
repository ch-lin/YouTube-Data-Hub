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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthenticationConfigTest {

    @Test
    @DisplayName("Should verify Getters and Setters")
    void testGettersAndSetters() {
        // Given
        AuthenticationConfig config = new AuthenticationConfig();
        String name = "default";
        Boolean enabled = true;
        Long jwtExpiration = 3600000L;
        Long jwtRefreshExpiration = 86400000L;
        String issuer = "https://auth.lin.ch";

        // When
        config.setName(name);
        config.setEnabled(enabled);
        config.setJwtExpiration(jwtExpiration);
        config.setJwtRefreshExpiration(jwtRefreshExpiration);
        config.setJwtIssuerUri(issuer);

        // Then
        assertEquals(name, config.getName());
        assertEquals(enabled, config.getEnabled());
        assertEquals(jwtExpiration, config.getJwtExpiration());
        assertEquals(jwtRefreshExpiration, config.getJwtRefreshExpiration());
        assertEquals(issuer, config.getJwtIssuerUri());
    }

    @Test
    @DisplayName("Should verify AllArgsConstructor")
    void testAllArgsConstructor() {
        // Given
        String name = "test-config";
        Boolean enabled = false;
        Long jwtExpiration = 1000L;
        Long jwtRefreshExpiration = 2000L;
        String issuer = "http://localhost";

        // When
        AuthenticationConfig config = new AuthenticationConfig(name, enabled, jwtExpiration, jwtRefreshExpiration, issuer);

        // Then
        assertEquals(name, config.getName());
        assertEquals(enabled, config.getEnabled());
        assertEquals(jwtExpiration, config.getJwtExpiration());
        assertEquals(jwtRefreshExpiration, config.getJwtRefreshExpiration());
        assertEquals(issuer, config.getJwtIssuerUri());
    }

    @Test
    @DisplayName("Should verify Equals and HashCode")
    void testEqualsAndHashCode() {
        // Given
        AuthenticationConfig config1 = new AuthenticationConfig("conf", true, 100L, 200L, "uri");
        AuthenticationConfig config2 = new AuthenticationConfig("conf", true, 100L, 200L, "uri");
        AuthenticationConfig config3 = new AuthenticationConfig("other", true, 100L, 200L, "uri");

        // Then
        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertEquals(config1.hashCode(), config2.hashCode());
        assertNotEquals(config1.hashCode(), config3.hashCode());
    }
}
