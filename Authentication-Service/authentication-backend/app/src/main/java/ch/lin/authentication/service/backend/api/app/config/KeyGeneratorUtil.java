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

/**
 * A utility class for generating RSA key pairs.
 * <p>
 * This is a non-instantiable utility class that provides static methods to
 * create cryptographic keys.
 */
public final class KeyGeneratorUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private KeyGeneratorUtil() {
    }

    /**
     * Generates a 2048-bit RSA key pair.
     *
     * @return A new {@link KeyPair} instance containing the public and private
     * keys.
     * @throws NoSuchAlgorithmException if the RSA algorithm is not available in
     * the environment.
     */
    public static KeyPair generateRsaKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * A record to hold a generated RSA public and private key pair.
     *
     * @param publicKey The RSA public key.
     * @param privateKey The RSA private key.
     */
    public record RsaKeyProperties(RSAPublicKey publicKey, RSAPrivateKey privateKey) {

        /**
         * A static factory method to generate a new {@link RsaKeyProperties}
         * instance.
         * <p>
         * This method handles the checked {@link NoSuchAlgorithmException} by
         * wrapping it in a {@link RuntimeException}, making it convenient for
         * use in contexts where a checked exception is not desired.
         *
         * @return A new instance of {@link RsaKeyProperties} with a freshly
         * generated key pair.
         * @throws IllegalStateException if the RSA algorithm is not available.
         */
        public static RsaKeyProperties generate() {
            try {
                KeyPair keyPair = generateRsaKey();
                RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
                RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
                return new RsaKeyProperties(publicKey, privateKey);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("Cannot generate RSA key pair: RSA algorithm not available.", e);
            }
        }
    }
}
