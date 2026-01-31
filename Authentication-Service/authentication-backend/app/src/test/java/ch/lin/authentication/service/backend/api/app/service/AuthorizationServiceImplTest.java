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
package ch.lin.authentication.service.backend.api.app.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import ch.lin.authentication.service.backend.api.app.repository.ClientRepository;
import ch.lin.authentication.service.backend.api.app.repository.UserRepository;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;
import ch.lin.authentication.service.backend.api.domain.model.Client;
import ch.lin.authentication.service.backend.api.domain.model.Role;
import ch.lin.authentication.service.backend.api.domain.model.User;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock
    private JwtEncoder jwtEncoder;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private ConfigsService configsService;

    @InjectMocks
    private AuthorizationServiceImpl authorizationService;

    private AuthenticationConfig authConfig;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        authConfig = new AuthenticationConfig();
        authConfig.setJwtExpiration(3600000L); // 1 hour
        authConfig.setJwtRefreshExpiration(7200000L); // 2 hours
        authConfig.setJwtIssuerUri("http://test-issuer");
    }

    @Test
    void cleanup_ShouldClearRepositories() {
        authorizationService.cleanup();
        verify(userRepository).cleanTable();
        verify(userRepository).resetSequence();
        verify(clientRepository).cleanTable();
        verify(clientRepository).resetSequence();
    }

    @Test
    @SuppressWarnings("null")
    void register_ShouldSaveUserAndReturnToken() {
        // Arrange
        String firstname = "John";
        String lastname = "Doe";
        String email = "john.doe@example.com";
        String password = "password";
        Role role = Role.USER;

        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);

        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("mockToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.register(firstname, lastname, email, password, role);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getFirstname()).isEqualTo(firstname);
        assertThat(savedUser.getLastname()).isEqualTo(lastname);
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRole()).isEqualTo(role);

        // Verify the parameters passed to the JWT encoder for both tokens
        ArgumentCaptor<JwtEncoderParameters> jwtParamsCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder, times(2)).encode(jwtParamsCaptor.capture());
        List<JwtEncoderParameters> allJwtParams = jwtParamsCaptor.getAllValues();

        // Assert on Access Token parameters (assuming it's the first call)
        JwtEncoderParameters accessTokenParams = allJwtParams.get(0);
        assertThat(accessTokenParams.getClaims().getSubject()).isEqualTo(email);
        assertThat(accessTokenParams.getClaims().getIssuer().toString()).isEqualTo(authConfig.getJwtIssuerUri());

        // Assert on Refresh Token parameters (assuming it's the second call)
        JwtEncoderParameters refreshTokenParams = allJwtParams.get(1);
        assertThat(refreshTokenParams.getClaims().getExpiresAt()).isAfter(accessTokenParams.getClaims().getExpiresAt());

        assertThat(token.token()).isEqualTo("mockToken");
        assertThat(token.refreshToken()).isEqualTo("mockToken");
    }

    @Test
    @SuppressWarnings("null")
    void registerClient_ShouldSaveClientAndReturnRawSecret() {
        // Arrange
        String clientName = "TestClient";
        Role role = Role.ADMIN;
        when(passwordEncoder.encode(anyString())).thenReturn("encodedSecret");

        // Act
        Client result = authorizationService.registerClient(clientName, role);

        // Assert
        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());
        Client savedClient = clientCaptor.getValue();

        assertThat(savedClient.getClientName()).isEqualTo(clientName);
        assertThat(savedClient.getClientSecret()).isEqualTo("encodedSecret"); // Verify DB gets encoded secret
        assertThat(savedClient.getRole()).isEqualTo(role);

        assertThat(result.getClientName()).isEqualTo(clientName);
        assertThat(result.getClientSecret()).isNotEqualTo("encodedSecret");
        assertThat(result.getRole()).isEqualTo(role);
    }

    @Test
    void authenticate_ShouldAuthenticateAndReturnToken() {
        // Arrange
        String email = "test@example.com";
        String password = "password";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).when(auth).getAuthorities();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);

        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("mockToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.authenticate(email, password);

        // Assert
        ArgumentCaptor<JwtEncoderParameters> jwtParamsCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        // Expecting 2 calls (Access Token + Refresh Token)
        verify(jwtEncoder, times(2)).encode(jwtParamsCaptor.capture());
        List<JwtEncoderParameters> allJwtParams = jwtParamsCaptor.getAllValues();

        JwtEncoderParameters accessTokenParams = allJwtParams.get(0);
        assertThat(accessTokenParams.getClaims().getSubject()).isEqualTo(email);
        assertThat(accessTokenParams.getClaims().getIssuer().toString()).isEqualTo(authConfig.getJwtIssuerUri());

        assertThat(token.token()).isEqualTo("mockToken");
    }

    @Test
    void authenticateClient_ShouldReturnToken_WhenCredentialsValid() {
        // Arrange
        String clientId = "clientId";
        String clientSecret = "secret";
        String encodedSecret = "encodedSecret";
        Client client = Client.builder()
                .clientId(clientId)
                .clientSecret(encodedSecret)
                .role(Role.USER)
                .build();

        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(clientSecret, encodedSecret)).thenReturn(true);
        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);

        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("mockToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.authenticateClient(clientId, clientSecret);

        // Assert
        assertThat(token.token()).isEqualTo("mockToken");
    }

    @Test
    void authenticateClient_ShouldThrow_WhenClientNotFound() {
        when(clientRepository.findByClientId("id")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authorizationService.authenticateClient("id", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid client ID or secret");
    }

    @Test
    void authenticateClient_ShouldThrow_WhenSecretInvalid() {
        Client client = Client.builder().clientId("id").clientSecret("encoded").build();
        when(clientRepository.findByClientId("id")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authorizationService.authenticateClient("id", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid client ID or secret");
    }

    @Test
    void refreshUserToken_ShouldReturnNewToken() {
        // Arrange
        String refreshToken = "validRefresh";
        String username = "user";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(username);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);
        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("newToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.refreshUserToken(refreshToken);

        // Assert
        assertThat(token.token()).isEqualTo("newToken");
    }

    @Test
    void refreshUserToken_ShouldThrow_WhenTokenInvalid() {
        String refreshToken = "invalid";
        when(jwtDecoder.decode(refreshToken)).thenThrow(new JwtException("Invalid token"));

        assertThatThrownBy(() -> authorizationService.refreshUserToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void refreshClientToken_ShouldReturnNewToken() {
        // Arrange
        String refreshToken = "validRefresh";
        String clientId = "client";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(clientId);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        Client client = Client.builder().clientId(clientId).role(Role.USER).build();
        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));

        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);
        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("newToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.refreshClientToken(refreshToken);

        // Assert
        assertThat(token.token()).isEqualTo("newToken");
    }

    @Test
    void refreshClientToken_ShouldThrow_WhenClientNotFound() {
        String refreshToken = "validRefresh";
        String clientId = "client";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(clientId);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.refreshClientToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid client ID in refresh token");
    }

    @Test
    void refreshClientToken_ShouldThrow_WhenTokenInvalid() {
        String refreshToken = "invalid";
        when(jwtDecoder.decode(refreshToken)).thenThrow(new JwtException("Invalid token"));

        assertThatThrownBy(() -> authorizationService.refreshClientToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }
}
