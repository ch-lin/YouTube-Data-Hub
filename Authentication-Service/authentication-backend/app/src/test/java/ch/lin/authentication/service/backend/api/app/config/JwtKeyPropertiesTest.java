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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

class JwtKeyPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void testPropertiesBinding() {
        runner.withPropertyValues(
                "jwt.key.public-key-location=classpath:keys/public.pem",
                "jwt.key.private-key-location=classpath:keys/private.pem"
        ).run(context -> {
            assertThat(context).hasSingleBean(JwtKeyProperties.class);
            JwtKeyProperties properties = context.getBean(JwtKeyProperties.class);

            assertThat(properties.getPublicKeyLocation())
                    .isNotNull()
                    .isInstanceOf(ClassPathResource.class);
            assertThat(properties.getPublicKeyLocation().getFilename())
                    .isEqualTo("public.pem");

            assertThat(properties.getPrivateKeyLocation())
                    .isNotNull()
                    .isInstanceOf(ClassPathResource.class);
            assertThat(properties.getPrivateKeyLocation().getFilename())
                    .isEqualTo("private.pem");
        });
    }

    @Test
    void testLombokMethods() {
        // Verify Lombok @Data generation (equals, hashCode, toString, getters, setters)
        JwtKeyProperties p1 = new JwtKeyProperties();
        p1.setPublicKeyLocation(new ClassPathResource("test.pem"));

        JwtKeyProperties p2 = new JwtKeyProperties();
        p2.setPublicKeyLocation(new ClassPathResource("test.pem"));

        assertThat(p1)
                .isEqualTo(p2)
                .hasSameHashCodeAs(p2);

        assertThat(p1.toString())
                .contains("JwtKeyProperties")
                .contains("publicKeyLocation");
    }

    @EnableConfigurationProperties(JwtKeyProperties.class)
    static class TestConfig {
        // Configuration class to enable the properties processing for the test slice
    }
}
