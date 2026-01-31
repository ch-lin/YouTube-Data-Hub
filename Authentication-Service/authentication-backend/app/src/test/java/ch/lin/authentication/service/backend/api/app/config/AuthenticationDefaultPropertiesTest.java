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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthenticationDefaultPropertiesTest {

    @Test
    @DisplayName("Should have correct default values")
    void testDefaultValues() {
        AuthenticationDefaultProperties properties = new AuthenticationDefaultProperties();

        assertEquals("default", properties.getName());
        assertEquals(true, properties.getEnabled());
        assertNull(properties.getJwtExpiration());
        assertNull(properties.getJwtRefreshExpiration());
        assertNull(properties.getJwtIssuerUri());
    }

    @Test
    @DisplayName("Should verify Getters and Setters")
    void testGettersAndSetters() {
        // Given
        AuthenticationDefaultProperties properties = new AuthenticationDefaultProperties();
        String name = "custom-config";
        Boolean enabled = false;
        Long jwtExpiration = 3600L;
        Long jwtRefreshExpiration = 7200L;
        String issuerUri = "https://api.example.com";

        // When
        properties.setName(name);
        properties.setEnabled(enabled);
        properties.setJwtExpiration(jwtExpiration);
        properties.setJwtRefreshExpiration(jwtRefreshExpiration);
        properties.setJwtIssuerUri(issuerUri);

        // Then
        assertEquals(name, properties.getName());
        assertEquals(enabled, properties.getEnabled());
        assertEquals(jwtExpiration, properties.getJwtExpiration());
        assertEquals(jwtRefreshExpiration, properties.getJwtRefreshExpiration());
        assertEquals(issuerUri, properties.getJwtIssuerUri());
    }

    @Test
    @DisplayName("Should verify Equals and HashCode")
    void testEqualsAndHashCode() {
        // Given
        AuthenticationDefaultProperties p1 = new AuthenticationDefaultProperties();
        p1.setName("test");

        AuthenticationDefaultProperties p2 = new AuthenticationDefaultProperties();
        p2.setName("test");

        AuthenticationDefaultProperties p3 = new AuthenticationDefaultProperties();
        p3.setName("other");

        // Then
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1.hashCode(), p3.hashCode());
    }

    @Test
    @DisplayName("Should verify ToString")
    void testToString() {
        AuthenticationDefaultProperties properties = new AuthenticationDefaultProperties();
        assertNotNull(properties.toString());
    }
}
