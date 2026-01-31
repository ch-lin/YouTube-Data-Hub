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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ch.lin.authentication.service.backend.api.app.repository.UserRepository;
import ch.lin.authentication.service.backend.api.domain.model.User;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private UserRepository repository;

    @Mock
    private AuthenticationConfiguration authConfiguration;

    @InjectMocks
    private ApplicationConfig applicationConfig;

    @Test
    @DisplayName("Should return UserDetailsService that loads user from repository")
    void userDetailsService_Success() {
        // Given
        String email = "test@example.com";
        User mockUser = mock(User.class);
        when(repository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // When
        UserDetailsService service = applicationConfig.userDetailsService();
        UserDetails result = service.loadUserByUsername(email);

        // Then
        assertNotNull(result);
        assertEquals(mockUser, result);
        verify(repository).findByEmail(email);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void userDetailsService_NotFound() {
        // Given
        String email = "nonexistent@example.com";
        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        UserDetailsService service = applicationConfig.userDetailsService();

        // Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(email));
        assertEquals("User not found", exception.getMessage());
        verify(repository).findByEmail(email);
    }

    @Test
    @DisplayName("Should return AuthenticationManager from config")
    void authenticationManager() throws Exception {
        // Given
        AuthenticationManager mockAuthManager = mock(AuthenticationManager.class);
        when(authConfiguration.getAuthenticationManager()).thenReturn(mockAuthManager);

        // When
        AuthenticationManager result = applicationConfig.authenticationManager(authConfiguration);

        // Then
        assertNotNull(result);
        assertEquals(mockAuthManager, result);
        verify(authConfiguration).getAuthenticationManager();
    }
}
