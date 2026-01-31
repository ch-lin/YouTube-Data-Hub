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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import ch.lin.authentication.service.backend.api.app.repository.AuthenticationConfigRepository;
import ch.lin.authentication.service.backend.api.app.repository.ClientRepository;
import ch.lin.authentication.service.backend.api.app.repository.UserRepository;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;
import ch.lin.authentication.service.backend.api.domain.model.Client;
import ch.lin.authentication.service.backend.api.domain.model.Role;
import ch.lin.authentication.service.backend.api.domain.model.User;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AuthenticationConfigRepository authConfigRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    @SuppressWarnings({"null", "unused"})
    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(userRepository, clientRepository, authConfigRepository, passwordEncoder);

        // Set default values for Admin User (as they are usually present in properties)
        ReflectionTestUtils.setField(dataInitializer, "adminFirstname", "admin");
        ReflectionTestUtils.setField(dataInitializer, "adminLastname", "admin");
        ReflectionTestUtils.setField(dataInitializer, "adminEmail", "admin@example.com");

        // Set default values for JWT Config
        ReflectionTestUtils.setField(dataInitializer, "jwtExpiration", 900000L);
        ReflectionTestUtils.setField(dataInitializer, "jwtRefreshExpiration", 604800000L);
        ReflectionTestUtils.setField(dataInitializer, "jwtIssuerUri", "http://test-issuer");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should create Admin User when password is provided and user does not exist")
    void initAdminUser_Success() throws Exception {
        // Given
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "admin123");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encodedPass");

        // When
        dataInitializer.initData().run();

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("admin@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPass");
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should skip Admin User creation when password is missing")
    void initAdminUser_SkipWhenNoPassword() throws Exception {
        // Given
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "");

        // When
        dataInitializer.initData().run();

        // Then
        verify(userRepository, never()).save(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should skip Admin User creation when user already exists")
    void initAdminUser_SkipWhenExists() throws Exception {
        // Given
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "admin123");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(new User()));

        // When
        dataInitializer.initData().run();

        // Then
        verify(userRepository, never()).save(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should create Downloader Client when ID provided and not exists")
    void initDownloaderClient_Success() throws Exception {
        // Given
        String clientId = "downloader-id";
        String clientSecret = "secret";
        ReflectionTestUtils.setField(dataInitializer, "downloaderClientId", clientId);
        ReflectionTestUtils.setField(dataInitializer, "downloaderClientSecret", clientSecret);

        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(clientSecret)).thenReturn("encodedSecret");

        // When
        dataInitializer.initData().run();

        // Then
        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());

        Client savedClient = clientCaptor.getValue();
        assertThat(savedClient.getClientId()).isEqualTo(clientId);
        assertThat(savedClient.getClientName()).isEqualTo("Downloader Service");
        assertThat(savedClient.getRole()).isEqualTo(Role.SERVICE);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should create Hub Client when ID provided and not exists")
    void initHubClient_Success() throws Exception {
        // Given
        String clientId = "hub-id";
        String clientSecret = "secret";
        ReflectionTestUtils.setField(dataInitializer, "hubClientId", clientId);
        ReflectionTestUtils.setField(dataInitializer, "hubClientSecret", clientSecret);

        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(clientSecret)).thenReturn("encodedSecret");

        // When
        dataInitializer.initData().run();

        // Then
        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());

        Client savedClient = clientCaptor.getValue();
        assertThat(savedClient.getClientId()).isEqualTo(clientId);
        assertThat(savedClient.getClientName()).isEqualTo("YouTube Hub Web");
        assertThat(savedClient.getRole()).isEqualTo(Role.SERVICE);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should create Postman Client when ID provided and not exists")
    void initPostmanClient_Success() throws Exception {
        // Given
        String clientId = "postman-id";
        String clientSecret = "secret";
        ReflectionTestUtils.setField(dataInitializer, "postmanClientId", clientId);
        ReflectionTestUtils.setField(dataInitializer, "postmanClientSecret", clientSecret);

        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(clientSecret)).thenReturn("encodedSecret");

        // When
        dataInitializer.initData().run();

        // Then
        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());

        Client savedClient = clientCaptor.getValue();
        assertThat(savedClient.getClientId()).isEqualTo(clientId);
        assertThat(savedClient.getClientName()).isEqualTo("Postman Test Client");
        assertThat(savedClient.getRole()).isEqualTo(Role.ADMIN);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should skip Client creation when ID is missing")
    void initClients_SkipWhenNoId() throws Exception {
        // Given - no client IDs set in ReflectionTestUtils

        // When
        dataInitializer.initData().run();

        // Then
        verify(clientRepository, never()).save(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should skip Client creation when ID is blank")
    void initClients_SkipWhenBlankId() throws Exception {
        // Given
        ReflectionTestUtils.setField(dataInitializer, "downloaderClientId", "");
        ReflectionTestUtils.setField(dataInitializer, "hubClientId", "   ");
        ReflectionTestUtils.setField(dataInitializer, "postmanClientId", "\t");

        // When
        dataInitializer.initData().run();

        // Then
        verify(clientRepository, never()).save(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should skip Client creation when Client already exists")
    void initClients_SkipWhenExists() throws Exception {
        // Given
        String clientId = "existing-id";
        ReflectionTestUtils.setField(dataInitializer, "downloaderClientId", clientId);
        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(new Client()));

        // When
        dataInitializer.initData().run();

        // Then
        verify(clientRepository, never()).save(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should skip Hub Client creation when Client already exists")
    void initHubClient_SkipWhenExists() throws Exception {
        // Given
        String clientId = "existing-hub-id";
        ReflectionTestUtils.setField(dataInitializer, "hubClientId", clientId);
        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(new Client()));

        // When
        dataInitializer.initData().run();

        // Then
        verify(clientRepository, never()).save(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should skip Postman Client creation when Client already exists")
    void initPostmanClient_SkipWhenExists() throws Exception {
        // Given
        String clientId = "existing-postman-id";
        ReflectionTestUtils.setField(dataInitializer, "postmanClientId", clientId);
        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(new Client()));

        // When
        dataInitializer.initData().run();

        // Then
        verify(clientRepository, never()).save(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should create Default Config when not exists")
    void initDefaultConfig_Success() throws Exception {
        // Given
        when(authConfigRepository.findById("default")).thenReturn(Optional.empty());

        // When
        dataInitializer.initData().run();

        // Then
        ArgumentCaptor<AuthenticationConfig> configCaptor = ArgumentCaptor.forClass(AuthenticationConfig.class);
        verify(authConfigRepository).save(configCaptor.capture());

        AuthenticationConfig savedConfig = configCaptor.getValue();
        assertThat(savedConfig.getName()).isEqualTo("default");
        assertThat(savedConfig.getEnabled()).isTrue();
        assertThat(savedConfig.getJwtIssuerUri()).isEqualTo("http://test-issuer");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Should skip Default Config creation when exists")
    void initDefaultConfig_SkipWhenExists() throws Exception {
        // Given
        when(authConfigRepository.findById("default")).thenReturn(Optional.of(new AuthenticationConfig()));

        // When
        dataInitializer.initData().run();

        // Then
        verify(authConfigRepository, never()).save(any());
    }
}
