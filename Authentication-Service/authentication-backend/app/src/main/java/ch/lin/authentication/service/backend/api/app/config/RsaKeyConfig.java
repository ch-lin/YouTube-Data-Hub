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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import lombok.RequiredArgsConstructor;

/**
 * Configuration class responsible for creating RSA key beans for JWT signing
 * and validation.
 * <p>
 * This class reads the PEM-encoded RSA key pair from the locations specified in
 * {@link JwtKeyProperties} and provides them as {@link RSAPublicKey} and
 * {@link RSAPrivateKey} beans in the Spring application context.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtKeyProperties.class)
public class RsaKeyConfig {

    private final JwtKeyProperties jwtKeyProperties;
    private static final String RSA_ALGORITHM = "RSA";

    /**
     * Creates the {@link RSAPublicKey} bean from the configured location.
     *
     * @return The RSA public key.
     * @throws IOException if the key file cannot be read.
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available.
     * @throws InvalidKeySpecException if the provided key data is invalid.
     */
    @Bean
    public RSAPublicKey publicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = readKeyBytes(
                jwtKeyProperties.getPublicKeyLocation(),
                "-----BEGIN PUBLIC KEY-----",
                "-----END PUBLIC KEY-----");

        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPublicKey) kf.generatePublic(keySpecX509);
    }

    /**
     * Creates the {@link RSAPrivateKey} bean from the configured location.
     *
     * @return The RSA private key.
     * @throws IOException if the key file cannot be read.
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available.
     * @throws InvalidKeySpecException if the provided key data is invalid.
     */
    @Bean
    public RSAPrivateKey privateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = readKeyBytes(
                jwtKeyProperties.getPrivateKeyLocation(),
                "-----BEGIN PRIVATE KEY-----",
                "-----END PRIVATE KEY-----");

        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
        return (RSAPrivateKey) kf.generatePrivate(keySpecPKCS8);
    }

    /**
     * Reads a PEM-encoded key from a resource, strips the header/footer, and
     * returns the Base64-decoded bytes.
     *
     * @param keyLocation The resource pointing to the key file.
     * @param header The PEM header to remove (e.g., "-----BEGIN PUBLIC
     * KEY-----").
     * @param footer The PEM footer to remove (e.g., "-----END PUBLIC
     * KEY-----").
     * @return The raw, decoded key bytes.
     * @throws IOException if the resource cannot be read.
     */
    private byte[] readKeyBytes(Resource keyLocation, String header, String footer) throws IOException {
        try (InputStream inputStream = keyLocation.getInputStream()) {
            String keyContent = new String(inputStream.readAllBytes());
            keyContent = keyContent.replaceAll("\\n", "")
                    .replace(header, "")
                    .replace(footer, "");
            return Base64.getDecoder().decode(keyContent);
        }
    }
}
