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
package ch.lin.platform.web.config;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(properties = "cors.allowed-origins=http://localhost:3000")
@ContextConfiguration(classes = {BaseWebConfigTest.TestApplication.class, BaseWebConfigTest.TestController.class})
@Import(BaseWebConfig.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test MVC config in isolation
class BaseWebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestApplication {

        @Bean
        public WebMvcConfigurer contentNegotiationConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void configureContentNegotiation(@NonNull ContentNegotiationConfigurer configurer) {
                    configurer.favorParameter(true)
                            .parameterName("mediaType")
                            .mediaType("json", Objects.requireNonNull(MediaType.APPLICATION_JSON));
                }
            };
        }
    }

    @Test
    void contentNegotiation_ShouldFavorParameterOverAcceptHeader() throws Exception {
        // Request with mediaType=json should return JSON even if Accept header is XML
        // This verifies favorParameter(true) and the parameter name configuration
        mockMvc.perform(get("/test/negotiation")
                .param("mediaType", "json")
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(Objects.requireNonNull(MediaType.APPLICATION_JSON)));
    }

    @Test
    void cors_ShouldAllowConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/test/cors")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PATCH,DELETE,PUT,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @RestController
    static class TestController {

        @GetMapping(value = "/test/negotiation", produces = MediaType.APPLICATION_JSON_VALUE)
        public String negotiation() {
            return "{\"message\": \"hello\"}";
        }

        @GetMapping("/test/cors")
        public String cors() {
            return "cors";
        }
    }
}
