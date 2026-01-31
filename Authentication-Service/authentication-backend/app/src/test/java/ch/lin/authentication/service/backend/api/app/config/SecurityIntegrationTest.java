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
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration tests for {@link SecurityConfig}.
 * <p>
 * This test verifies the security filter chain behavior using MockMvc and
 * Spring Security Test support, avoiding the need to mock internal Spring
 * Security classes like HttpSecurity.
 */
@WebMvcTest(controllers = SecurityIntegrationTest.TestConfig.TestController.class)
@ContextConfiguration(classes = {SecurityIntegrationTest.TestApplication.class, SecurityIntegrationTest.TestConfig.class})
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnauthenticated_thenReturns401() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAuthenticatedWithJwt_thenReturns200() throws Exception {
        // Simulates a valid JWT authentication
        mockMvc.perform(get("/test/protected")
                .with(Objects.requireNonNull(jwt())))
                .andExpect(status().isOk());
    }

    @Configuration
    static class TestApplication {
    }

    /**
     * Test configuration to provide necessary beans for SecurityConfig and a
     * dummy controller.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        public RSAPublicKey publicKey() throws Exception {
            return (RSAPublicKey) generateKeyPair().getPublic();
        }

        @Bean
        public RSAPrivateKey privateKey() throws Exception {
            return (RSAPrivateKey) generateKeyPair().getPrivate();
        }

        private KeyPair generateKeyPair() throws Exception {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        }

        @RestController
        static class TestController {

            @GetMapping("/test/protected")
            public String protectedEndpoint() {
                return "secret";
            }
        }
    }
}
