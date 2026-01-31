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
import java.util.Base64;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

class RsaKeyConfigTest {

    private JwtKeyProperties jwtKeyProperties;
    private RsaKeyConfig rsaKeyConfig;
    private KeyPair keyPair;

    void setUp() throws Exception {
        // Generate a real RSA key pair for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        jwtKeyProperties = new JwtKeyProperties();
        rsaKeyConfig = new RsaKeyConfig(jwtKeyProperties);
    }

    @Test
    void publicKey_ShouldLoadAndDecodePublicKey() throws Exception {
        // Arrange
        setUp();
        String pem = createPem("PUBLIC KEY", keyPair.getPublic().getEncoded());
        Resource resource = new ByteArrayResource(Objects.requireNonNull(pem.getBytes()));
        ReflectionTestUtils.setField(Objects.requireNonNull(jwtKeyProperties), "publicKeyLocation", resource);

        // Act
        RSAPublicKey result = rsaKeyConfig.publicKey();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModulus()).isEqualTo(((RSAPublicKey) keyPair.getPublic()).getModulus());
    }

    @Test
    void privateKey_ShouldLoadAndDecodePrivateKey() throws Exception {
        // Arrange
        setUp();
        String pem = createPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        Resource resource = new ByteArrayResource(Objects.requireNonNull(pem.getBytes()));
        ReflectionTestUtils.setField(Objects.requireNonNull(jwtKeyProperties), "privateKeyLocation", resource);

        // Act
        RSAPrivateKey result = rsaKeyConfig.privateKey();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getModulus()).isEqualTo(((RSAPrivateKey) keyPair.getPrivate()).getModulus());
    }

    private String createPem(String type, byte[] content) {
        String base64 = Base64.getEncoder().encodeToString(content);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(type).append("-----\n");
        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length())).append("\n");
        }
        pem.append("-----END ").append(type).append("-----");
        return pem.toString();
    }
}
