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
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class KeyGeneratorUtilTest {

    @Test
    void generateRsaKey_ShouldReturnValid2048BitKeyPair() throws Exception {
        // When
        KeyPair keyPair = KeyGeneratorUtil.generateRsaKey();

        // Then
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPublic()).isInstanceOf(RSAPublicKey.class);
        assertThat(keyPair.getPrivate()).isInstanceOf(RSAPrivateKey.class);

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        // Verify key size is 2048 bits
        assertThat(publicKey.getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void rsaKeyProperties_Generate_ShouldReturnValidRecord() {
        // When
        KeyGeneratorUtil.RsaKeyProperties properties = KeyGeneratorUtil.RsaKeyProperties.generate();

        // Then
        assertThat(properties).isNotNull();
        assertThat(properties.publicKey()).isNotNull();
        assertThat(properties.privateKey()).isNotNull();
        assertThat(properties.publicKey().getModulus().bitLength()).isEqualTo(2048);
    }

    @Test
    void rsaKeyProperties_Generate_ShouldThrowIllegalStateException_WhenNoSuchAlgorithmExceptionOccurs() {
        try (MockedStatic<KeyPairGenerator> mockedKeyPairGenerator = Mockito.mockStatic(KeyPairGenerator.class)) {
            mockedKeyPairGenerator.when(() -> KeyPairGenerator.getInstance("RSA"))
                    .thenThrow(new NoSuchAlgorithmException("Simulated RSA missing"));

            assertThatThrownBy(KeyGeneratorUtil.RsaKeyProperties::generate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot generate RSA key pair: RSA algorithm not available.")
                    .hasCauseInstanceOf(NoSuchAlgorithmException.class);
        }
    }
}
