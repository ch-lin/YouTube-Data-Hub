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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ch.lin.authentication.service.backend.api.app.service.ConfigsService;
import ch.lin.authentication.service.backend.api.app.service.JwkSetService;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;

@ExtendWith(MockitoExtension.class)
class OidcDiscoveryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JwkSetService jwkSetService;

    @Mock
    private ConfigsService configsService;

    @InjectMocks
    private OidcDiscoveryController oidcDiscoveryController;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(oidcDiscoveryController).build();
    }

    @Test
    @DisplayName("Should return OIDC configuration with correct issuer and jwks_uri")
    void openidConfiguration_ShouldReturnConfig() throws Exception {
        AuthenticationConfig config = new AuthenticationConfig();
        config.setJwtIssuerUri("http://localhost:8080");

        when(configsService.getResolvedConfig(null)).thenReturn(config);

        mockMvc.perform(get("/.well-known/openid-configuration")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8080/oauth2/jwks"));
    }

    @Test
    @DisplayName("Should return JWK Set")
    void jwks_ShouldReturnKeys() throws Exception {
        Map<String, Object> jwks = Map.of("keys", "some-key-data");
        when(jwkSetService.getJwkSet()).thenReturn(jwks);

        mockMvc.perform(get("/oauth2/jwks")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").value("some-key-data"));
    }
}
