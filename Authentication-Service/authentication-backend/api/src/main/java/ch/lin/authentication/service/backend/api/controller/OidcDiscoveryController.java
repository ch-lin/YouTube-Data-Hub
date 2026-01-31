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

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.lin.authentication.service.backend.api.app.service.ConfigsService;
import ch.lin.authentication.service.backend.api.app.service.JwkSetService;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;

/**
 * REST controller for implementing the OIDC Discovery specification.
 * <p>
 * This controller provides standard endpoints that allow clients to
 * automatically discover the configuration of this OpenID Provider.
 */
@RestController
public class OidcDiscoveryController {

    private final JwkSetService jwkSetService;
    private final ConfigsService configsService;

    public OidcDiscoveryController(JwkSetService jwkSetService, ConfigsService configsService) {
        this.jwkSetService = jwkSetService;
        this.configsService = configsService;
    }

    /**
     * Serves the OIDC Provider Configuration Information document.
     * <p>
     * This endpoint returns essential metadata about the provider, such as the
     * issuer URI and the location of the JSON Web Key Set (JWKS).
     *
     * @return A map representing the JSON discovery document.
     */
    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> openidConfiguration() {
        AuthenticationConfig activeConfig = configsService.getResolvedConfig(null);
        String issuerUri = activeConfig.getJwtIssuerUri();
        return Map.of(
                "issuer", issuerUri,
                "jwks_uri", issuerUri + "/oauth2/jwks"
        // TODO: Add other OIDC metadata here as needed, e.g.,
        // "authorization_endpoint", issuerUri + "/api/v1/auth/authorize",
        // "token_endpoint", issuerUri + "/api/v1/auth/authenticate"
        );
    }

    /**
     * Serves the JSON Web Key Set (JWKS) for this provider.
     * <p>
     * The JWKS contains the public key(s) that clients can use to verify the
     * signature of JWTs issued by this service.
     *
     * @return A map representing the JWK Set in JSON format.
     */
    @GetMapping(value = "/oauth2/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return jwkSetService.getJwkSet();
    }
}
