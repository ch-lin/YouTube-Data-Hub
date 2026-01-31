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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class SecurityConfigTest {

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private SecurityConfig securityConfig;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() throws Exception {
        // Generate real RSA keys to ensure JWT libraries (Nimbus) accept them during bean creation
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        publicKey = (RSAPublicKey) keyPair.getPublic();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        securityConfig = new SecurityConfig(publicKey, privateKey);
    }

    @Test
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void jwtAuthenticationConverter_ShouldExtractAuthoritiesCorrectly() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        // Create a dummy JWT with "roles" claim
        Map<String, Object> claims = Map.of("roles", List.of("USER", "ADMIN"));
        Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"), claims);

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void jwtDecoder_ShouldBeCreatedWithPublicKey() {
        JwtDecoder decoder = securityConfig.jwtDecoder();
        assertThat(decoder).isNotNull();
    }

    @Test
    void jwtEncoder_ShouldBeCreatedWithKeyPair() {
        JwtEncoder encoder = securityConfig.jwtEncoder();
        assertThat(encoder).isNotNull();
    }
}
