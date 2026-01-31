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
package ch.lin.youtube.hub.backend.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the response body for a successful user authentication.
 * <p>
 * This DTO is returned by the
 * {@link ch.lin.youtube.hub.backend.api.controller.YoutubeHubController#login(LoginRequest)}
 * endpoint upon successful login. It contains an authentication token for
 * subsequent API calls.
 * <p>
 * Example JSON response:
 *
 * <pre>
 * {@code
 * {
 *   "token": "dummy-token"
 * }
 * }
 * </pre>
 */
@Getter
@Setter
public class LoginResponse {

    /**
     * The authentication token (e.g., a JWT) to be used in the 'Authorization'
     * header for subsequent requests.
     */
    private String token;

    /**
     * Constructs a new LoginResponse.
     *
     * @param token The authentication token.
     */
    public LoginResponse(String token) {
        this.token = token;
    }
}
