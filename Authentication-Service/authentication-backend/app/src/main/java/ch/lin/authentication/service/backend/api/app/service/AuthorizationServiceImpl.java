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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import ch.lin.authentication.service.backend.api.app.repository.ClientRepository;
import ch.lin.authentication.service.backend.api.app.repository.UserRepository;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;
import ch.lin.authentication.service.backend.api.domain.model.Client;
import ch.lin.authentication.service.backend.api.domain.model.Role;
import ch.lin.authentication.service.backend.api.domain.model.User;

/**
 * Service implementation for handling user and client authorization, including
 * registration, authentication, and token management (generation and refresh).
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final ClientRepository clientRepository;
    private final UserDetailsService userDetailsService;
    private final ConfigsService configsService;

    /**
     * Constructs a new AuthorizationServiceImpl with the necessary
     * dependencies.
     *
     * @param jwtEncoder The encoder for creating JWTs.
     * @param jwtDecoder The decoder for validating and parsing JWTs.
     * @param userRepository The repository for user data access.
     * @param authenticationManager The manager for handling authentication
     * requests.
     * @param passwordEncoder The encoder for hashing passwords and client
     * secrets.
     * @param clientRepository The repository for client application data
     * access.
     * @param userDetailsService The service for loading user-specific data.
     * @param configsService The service to fetch dynamic authentication
     * configuration.
     */
    public AuthorizationServiceImpl(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder,
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            ClientRepository clientRepository, UserDetailsService userDetailsService, ConfigsService configsService) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.clientRepository = clientRepository;
        this.userDetailsService = userDetailsService;
        this.configsService = configsService;
    }

    /**
     * Deletes all users and clients from the database. This is typically used
     * for testing or cleanup purposes.
     */
    @Override
    public void cleanup() {
        userRepository.cleanTable();
        userRepository.resetSequence();
        clientRepository.cleanTable();
        clientRepository.resetSequence();
    }

    /**
     * Registers a new user in the system.
     *
     * @param firstname The user's first name.
     * @param lastname The user's last name.
     * @param email The user's email, used as the username.
     * @param password The user's raw password.
     * @param role The {@link Role} to assign to the user.
     * @return A {@link JwtToken} containing the access and refresh tokens for
     * the newly registered user.
     */
    @Override
    public JwtToken register(String firstname, String lastname, String email, String password, Role role) {
        User user = new User(null, firstname, lastname, email, passwordEncoder.encode(password), role);
        userRepository.save(user);
        JwtToken jwtToken = generateToken(user);
        return jwtToken;
    }

    /**
     * Registers a new client application. The client secret is hashed before
     * being stored, but the raw secret is returned once for initial setup.
     *
     * @param clientName The name of the client application.
     * @param role The {@link Role} to assign to the client.
     * @return The registered {@link Client} with its raw, un-hashed client
     * secret.
     */
    @Override
    public Client registerClient(String clientName, Role role) {
        String clientId = UUID.randomUUID().toString();
        String clientSecret = UUID.randomUUID().toString();

        Client client = Client.builder()
                .clientName(clientName)
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret)) // Store the hashed secret
                .role(role)
                .build();

        clientRepository.save(Objects.requireNonNull(client));

        // Return the client with the raw secret for one-time display
        return client.toBuilder().clientSecret(clientSecret).build();
    }

    /**
     * Authenticates a user with their email and password.
     *
     * @param email The user's email.
     * @param password The user's password.
     * @return A {@link JwtToken} containing the access and refresh tokens upon
     * successful authentication.
     */
    @Override
    public JwtToken authenticate(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        password));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        JwtToken jwtToken = generateToken(authentication);
        return jwtToken;
    }

    /**
     * Authenticates a client application with its client ID and client secret.
     *
     * @param clientId The client's unique identifier.
     * @param clientSecret The client's secret.
     * @return A {@link JwtToken} containing the access and refresh tokens upon
     * successful authentication.
     * @throws IllegalArgumentException if the client ID or secret is invalid.
     */
    @Override
    public JwtToken authenticateClient(String clientId, String clientSecret) {
        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client ID or secret"));

        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            throw new IllegalArgumentException("Invalid client ID or secret");
        }

        // Generate a token for the client
        return generateToken(client.getClientId(), List.of(client.getRole().name()));
    }

    /**
     * Refreshes a user's access token using a valid refresh token.
     *
     * @param refreshToken The user's refresh token.
     * @return A new {@link JwtToken} containing a new access token and a new
     * refresh token.
     * @throws IllegalArgumentException if the refresh token is invalid or
     * expired.
     */
    @Override
    public JwtToken refreshUserToken(String refreshToken) {
        try {
            var jwt = jwtDecoder.decode(refreshToken);
            String username = jwt.getSubject();

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return generateToken(userDetails);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid refresh token", e);
        }
    }

    /**
     * Refreshes a client's access token using a valid refresh token.
     *
     * @param refreshToken The client's refresh token.
     * @return A new {@link JwtToken} containing a new access token and a new
     * refresh token.
     * @throws IllegalArgumentException if the refresh token is invalid,
     * expired, or belongs to a non-existent client.
     */
    @Override
    public JwtToken refreshClientToken(String refreshToken) {
        try {
            var jwt = jwtDecoder.decode(refreshToken);
            String clientId = jwt.getSubject();

            // Verify the client exists
            Client client = clientRepository.findByClientId(clientId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid client ID in refresh token"));

            return generateToken(client.getClientId(), List.of(client.getRole().name()));
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid refresh token", e);
        }
    }

    /**
     * Generates a JWT token pair based on a successful authentication object.
     *
     * @param authentication The successful {@link Authentication} object.
     * @return A {@link JwtToken} containing the access and refresh tokens.
     */
    private JwtToken generateToken(Authentication authentication) {
        return generateToken(authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));
    }

    /**
     * Generates a JWT token pair for a given user details object.
     *
     * @param userDetails The {@link UserDetails} object for the user.
     * @return A {@link JwtToken} containing the access and refresh tokens.
     */
    private JwtToken generateToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));
    }

    /**
     * Generates a JWT token pair (access and refresh) for a subject with
     * specified roles.
     *
     * @param username The subject of the token (e.g., user's email or client's
     * ID).
     * @param roles The roles/authorities to include in the access token's
     * claims.
     * @return A {@link JwtToken} containing the newly created access and
     * refresh tokens.
     */
    private JwtToken generateToken(String username, Iterable<String> roles) {
        AuthenticationConfig config = configsService.getResolvedConfig(null);
        Instant now = Instant.now();

        Instant accessTokenExpiry = now.plus(config.getJwtExpiration(), ChronoUnit.MILLIS);

        Instant refreshTokenExpiry = now.plus(config.getJwtRefreshExpiration(), ChronoUnit.MILLIS);

        logger.info(
                "Generating token for subject '{}' with access token expiration: {}ms, refresh token expiration: {}ms",
                username, config.getJwtExpiration(), config.getJwtRefreshExpiration());

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        String accessToken = createToken(username, now, accessTokenExpiry, claims, config.getJwtIssuerUri());

        String refreshToken = createToken(username, now, refreshTokenExpiry, new HashMap<>(), config.getJwtIssuerUri());

        logger.debug("Access token {}, refreah token {}", accessToken, refreshToken);

        return new JwtToken(accessToken, refreshToken, Date.from(accessTokenExpiry), config.getJwtExpiration() / 1000);
    }

    /**
     * Creates a single JWT string with the given parameters.
     *
     * @param subject The subject of the token.
     * @param issuedAt The time the token is issued.
     * @param expiresAt The time the token expires.
     * @param claims A map of custom claims to include in the token.
     * @param issuerUri The issuer URI for the 'iss' claim.
     * @return The encoded JWT as a string.
     */
    private String createToken(String subject, Instant issuedAt, Instant expiresAt, Map<String, Object> claims, String issuerUri) {
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .subject(subject)
                .issuer(issuerUri)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt);

        claims.forEach(claimsBuilder::claim);

        return jwtEncoder.encode(JwtEncoderParameters.from(claimsBuilder.build())).getTokenValue();
    }
}
