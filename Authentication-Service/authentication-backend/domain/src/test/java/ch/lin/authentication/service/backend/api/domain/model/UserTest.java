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

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UserTest {

    private final User user = new User();

    @Test
    @DisplayName("Should return correct authorities based on role")
    void getAuthorities() {
        // Given
        // We assume Role is an enum in the same package and has at least one value.
        if (Role.values().length > 0) {
            Role testRole = Role.values()[0];
            user.setRole(testRole);

            // When
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

            // Then
            assertNotNull(authorities);
            assertEquals(1, authorities.size());
            assertTrue(authorities.contains(new SimpleGrantedAuthority(testRole.name())));
        }
    }

    @Test
    @DisplayName("Should return email as username")
    void getUsername() {
        // Given
        String email = "test@example.com";
        user.setEmail(email);

        // When
        String username = user.getUsername();

        // Then
        assertEquals(email, username);
    }

    @Test
    @DisplayName("Should return true for all boolean status flags")
    void accountStatusFlags() {
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("Should verify Lombok generated methods (Constructors, Getters, Setters, Equals, HashCode)")
    void lombokMethods() {
        // Given
        Integer id = 1;
        String firstname = "John";
        String lastname = "Doe";
        String email = "john.doe@example.com";
        String password = "password123";
        Role role = (Role.values().length > 0) ? Role.values()[0] : null;

        // When
        User fullUser = new User(id, firstname, lastname, email, password, role);

        // Then
        assertEquals(id, fullUser.getId());
        assertEquals(firstname, fullUser.getFirstname());
        assertEquals(lastname, fullUser.getLastname());
        assertEquals(email, fullUser.getEmail());
        assertEquals(password, fullUser.getPassword());
        assertEquals(role, fullUser.getRole());

        // Test Equals and HashCode
        User anotherUser = new User(id, firstname, lastname, email, password, role);
        assertEquals(fullUser, anotherUser);
        assertEquals(fullUser.hashCode(), anotherUser.hashCode());
    }
}
