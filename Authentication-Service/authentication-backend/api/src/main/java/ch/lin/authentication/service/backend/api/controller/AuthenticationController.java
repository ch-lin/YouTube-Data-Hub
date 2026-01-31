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
package ch.lin.authentication.service.backend.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.lin.authentication.service.backend.api.app.service.AuthorizationService;
import ch.lin.authentication.service.backend.api.app.service.JwtToken;
import ch.lin.authentication.service.backend.api.domain.model.Client;
import ch.lin.authentication.service.backend.api.dto.AuthenticationRequest;
import ch.lin.authentication.service.backend.api.dto.AuthenticationResponse;
import ch.lin.authentication.service.backend.api.dto.ClientAuthenticationRequest;
import ch.lin.authentication.service.backend.api.dto.ClientRegisterRequest;
import ch.lin.authentication.service.backend.api.dto.ClientRegisterResponse;
import ch.lin.authentication.service.backend.api.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for managing authentication processes for both users and
 * machine clients.
 * <p>
 * This controller provides endpoints for registration, authentication, and
 * token refreshing. It delegates the core business logic to the
 * {@link AuthorizationService}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    public static final String BEARER_PREFIX = "Bearer ";

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthorizationService authorizationService;

    public AuthenticationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * Registers a new user and returns authentication tokens.
     *
     * @param request The registration request containing user details.
     * @return A {@link ResponseEntity} with an {@link AuthenticationResponse}
     * containing the JWT and refresh token.
     * <p>
     * Example cURL request:
     *
     * <pre>{@code
     * curl -X POST http://localhost:8082/api/v1/auth/register \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "firstname": "John",
     *   "lastname": "Doe",
     *   "email": "john.doe@example.com",
     *   "password": "password123",
     *   "role": "USER"
     * }'
     * }</pre>
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) {
        logger.info("Processing registration for user: {}", request.email());
        JwtToken jwtToken = authorizationService.register(request.firstname(), request.lastname(), request.email(),
                request.password(), request.role());
        AuthenticationResponse response = new AuthenticationResponse(jwtToken.token(), jwtToken.refreshToken(),
                BEARER_PREFIX.trim(), jwtToken.expiresIn());
        logger.info("User registered successfully: {}", request.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates a user and returns new authentication tokens.
     *
     * @param request The authentication request containing the user's
     * credentials.
     * @return A {@link ResponseEntity} with an {@link AuthenticationResponse}
     * containing a new JWT and refresh token.
     * <p>
     * Example cURL request:
     *
     * <pre>{@code
     * curl -X POST http://localhost:8082/api/v1/auth/authenticate \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "email": "john.doe@example.com",
     *   "password": "password123"
     * }'
     * }</pre>
     */
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) {
        logger.info("Processing authentication for user: {}", request.email());
        JwtToken jwtToken = authorizationService.authenticate(request.email(), request.password());
        AuthenticationResponse response = new AuthenticationResponse(jwtToken.token(), jwtToken.refreshToken(),
                BEARER_PREFIX.trim(), jwtToken.expiresIn());
        logger.info("User authenticated successfully: {}", request.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new machine client and returns its generated credentials. The
     * client secret is only returned on registration and is not stored in plain
     * text.
     *
     * @param request The registration request containing the client's name and
     * role.
     * @return A {@link ResponseEntity} with a {@link ClientRegisterResponse}
     * containing the generated client ID and client secret.
     * <p>
     * Example cURL request:
     *
     * <pre>{@code
     * curl -X POST http://localhost:8082/api/v1/auth/client-register \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "clientName": "downloader-app",
     *   "role": "SERVICE"
     * }'
     * }</pre>
     */
    @PostMapping("/client-register")
    public ResponseEntity<ClientRegisterResponse> registerClient(@RequestBody ClientRegisterRequest request) {
        logger.info("Processing registration for client: {}", request.clientName());
        Client client = authorizationService.registerClient(request.clientName(), request.role());
        ClientRegisterResponse response = new ClientRegisterResponse(client.getClientId(), client.getClientSecret());
        logger.info("Client registered successfully: {}", request.clientName());
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticates a machine client and returns an authentication token.
     *
     * @param request The authentication request containing the client's
     * credentials.
     * @return A {@link ResponseEntity} with an {@link AuthenticationResponse}
     * containing a new JWT and refresh token.
     * <p>
     * Example cURL request:
     *
     * <pre>{@code
     * curl -X POST http://localhost:8082/api/v1/auth/client-authenticate \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "clientId": "generated-client-id",
     *   "clientSecret": "generated-client-secret"
     * }'
     * }</pre>
     */
    @PostMapping("/client-authenticate")
    public ResponseEntity<AuthenticationResponse> authenticateClient(@RequestBody ClientAuthenticationRequest request) {
        logger.info("Processing authentication for client ID: {}", request.clientId());
        JwtToken jwtToken = authorizationService.authenticateClient(request.clientId(), request.clientSecret());
        AuthenticationResponse response = new AuthenticationResponse(jwtToken.token(), jwtToken.refreshToken(),
                BEARER_PREFIX.trim(), jwtToken.expiresIn());
        logger.info("Client authenticated successfully: {}", request.clientId());
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes an authentication token.
     * <p>
     * This endpoint takes a valid refresh token and returns a new access token,
     * allowing the user to extend their session without re-authenticating.
     *
     * @param request The HTTP request containing the refresh token in the
     * Authorization header.
     * @return A {@link ResponseEntity} with an {@link AuthenticationResponse}
     * containing a new access token and a new refresh token.
     * <p>
     * Example cURL request:
     *
     * <pre>{@code
     * curl -X POST http://localhost:8082/api/v1/auth/refresh \
     * -H "Authorization: Bearer <your-refresh-token>"
     * }</pre>
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletRequest request) {
        logger.info("Processing token refresh request.");
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Token refresh failed: Missing or invalid Authorization header.");
            return ResponseEntity.badRequest().build();
        }

        try {
            String refreshToken = authHeader.substring(BEARER_PREFIX.length());
            JwtToken newJwtToken = authorizationService.refreshUserToken(refreshToken);
            logger.info("Token refreshed successfully for user.");
            AuthenticationResponse response = new AuthenticationResponse(newJwtToken.token(), newJwtToken.refreshToken(),
                    BEARER_PREFIX.trim(), newJwtToken.expiresIn());
            return ResponseEntity.ok(response);
        } catch (JwtException | BadCredentialsException e) {
            logger.warn("Refresh token failed: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Refreshes an authentication token for a machine client.
     * <p>
     * This endpoint takes a valid client refresh token and returns a new access
     * token.
     *
     * @param request The HTTP request containing the refresh token in the
     * Authorization header.
     * @return A {@link ResponseEntity} with an {@link AuthenticationResponse}
     * containing a new access token and a new refresh token.
     * <p>
     * Example cURL request:
     *
     * <pre>{@code
     * curl -X POST http://localhost:8082/api/v1/auth/client-refresh \
     * -H "Authorization: Bearer <your-client-refresh-token>"
     * }</pre>
     */
    @PostMapping("/client-refresh")
    public ResponseEntity<AuthenticationResponse> refreshClientToken(HttpServletRequest request) {
        logger.info("Processing client token refresh request.");
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Client token refresh failed: Missing or invalid Authorization header.");
            return ResponseEntity.badRequest().build();
        }

        try {
            String refreshToken = authHeader.substring(BEARER_PREFIX.length());
            JwtToken newJwtToken = authorizationService.refreshClientToken(refreshToken);
            logger.info("Token refreshed successfully for client.");
            // Implement refresh token rotation by returning the new refresh token
            AuthenticationResponse response = new AuthenticationResponse(newJwtToken.token(), newJwtToken.refreshToken(),
                    BEARER_PREFIX.trim(), newJwtToken.expiresIn());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Client refresh token failed: {}", e.getMessage());
            // Return 401 Unauthorized for invalid tokens
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Deletes all users and clients, and resets their respective database
     * sequences.
     * <p>
     * This is a destructive operation and should be used with caution,
     * typically in development or testing environments.
     *
     * @return A {@link ResponseEntity} with a confirmation message.
     * <p>
     * Example cURL request:
     *
     * <pre>
     * {@code
     * curl -X DELETE http://localhost:8082/api/v1/auth/cleanup
     * }
     * </pre>
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanup() {
        logger.warn("Processing cleanup request. All users and clients will be deleted.");
        authorizationService.cleanup();
        logger.warn("Cleanup successful. All users and clients have been deleted and sequences have been reset.");
        return ResponseEntity.ok("All users and clients have been deleted and sequences have been reset.");
    }
}
