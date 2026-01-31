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

import ch.lin.authentication.service.backend.api.domain.model.Client;
import ch.lin.authentication.service.backend.api.domain.model.Role;

/**
 * Defines the contract for authorization services, including user and client
 * registration, authentication, and token management.
 */
public interface AuthorizationService {

    /**
     * Deletes all users and clients from the database. This is typically used
     * for testing or cleanup purposes.
     */
    void cleanup();

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
    JwtToken register(String firstname, String lastname, String email, String password, Role role);

    /**
     * Registers a new client application. The client secret is returned in its
     * raw form for initial setup.
     *
     * @param clientName The name of the client application.
     * @param role The {@link Role} to assign to the client.
     * @return The registered {@link Client} with its raw, un-hashed client
     * secret.
     */
    Client registerClient(String clientName, Role role);

    /**
     * Authenticates a user with their email and password.
     *
     * @param email The user's email.
     * @param password The user's password.
     * @return A {@link JwtToken} containing the access and refresh tokens upon
     * successful authentication.
     */
    JwtToken authenticate(String email, String password);

    /**
     * Authenticates a client application with its client ID and client secret.
     *
     * @param clientId The client's unique identifier.
     * @param clientSecret The client's secret.
     * @return A {@link JwtToken} containing the access and refresh tokens upon
     * successful authentication.
     * @throws IllegalArgumentException if the client ID or secret is invalid.
     */
    JwtToken authenticateClient(String clientId, String clientSecret);

    /**
     * Refreshes a user's access token using a valid refresh token.
     *
     * @param refreshToken The user's refresh token.
     * @return A new {@link JwtToken} containing a new access token.
     * @throws IllegalArgumentException if the refresh token is invalid or
     * expired.
     */
    JwtToken refreshUserToken(String refreshToken);

    /**
     * Refreshes a client's access token using a valid refresh token.
     *
     * @param refreshToken The client's refresh token.
     * @return A new {@link JwtToken} containing a new access token.
     * @throws IllegalArgumentException if the refresh token is invalid or
     * expired.
     */
    JwtToken refreshClientToken(String refreshToken);
}
