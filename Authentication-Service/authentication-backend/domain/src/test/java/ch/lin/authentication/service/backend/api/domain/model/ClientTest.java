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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientTest {

    @Test
    @DisplayName("Should build client using Builder pattern")
    void testBuilder() {
        // Given
        Integer id = 1;
        String clientName = "Test App";
        String clientId = "client-id-123";
        String clientSecret = "secret-hash";
        Role role = (Role.values().length > 0) ? Role.values()[0] : null;

        // When
        Client client = Client.builder()
                .id(id)
                .clientName(clientName)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .role(role)
                .build();

        // Then
        assertEquals(id, client.getId());
        assertEquals(clientName, client.getClientName());
        assertEquals(clientId, client.getClientId());
        assertEquals(clientSecret, client.getClientSecret());
        assertEquals(role, client.getRole());
    }

    @Test
    @DisplayName("Should create a copy with modified fields using toBuilder")
    void testToBuilder() {
        // Given
        Client original = Client.builder()
                .clientName("Original Name")
                .clientId("id-1")
                .build();

        // When
        Client modified = original.toBuilder()
                .clientName("Modified Name")
                .build();

        // Then
        assertEquals("id-1", modified.getClientId()); // Should remain same
        assertEquals("Modified Name", modified.getClientName()); // Should change
        assertNotEquals(original, modified);
    }

    @Test
    @DisplayName("Should verify Lombok @Data generated methods (Equals, HashCode, ToString)")
    void testDataMethods() {
        // Given
        Client client1 = Client.builder().clientId("id").build();
        Client client2 = Client.builder().clientId("id").build();
        Client client3 = Client.builder().clientId("other").build();

        // Then
        // Equals
        assertEquals(client1, client2);
        assertNotEquals(client1, client3);

        // HashCode
        assertEquals(client1.hashCode(), client2.hashCode());
        assertNotEquals(client1.hashCode(), client3.hashCode());

        // ToString
        assertNotNull(client1.toString());
        assertTrue(client1.toString().contains("clientId=id"));
    }
}
