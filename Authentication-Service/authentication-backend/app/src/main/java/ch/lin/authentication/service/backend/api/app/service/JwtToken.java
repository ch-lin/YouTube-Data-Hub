package ch.lin.authentication.service.backend.api.app.service;

import java.util.Date;

/**
 * Represents a JWT, including the access token, a refresh token, and the access
 * token's expiration date.
 *
 * @param token The JWT access token string.
 * @param refreshToken The JWT refresh token string.
 * @param expiresAt The expiration {@link Date} of the access token.
 * @param expiresIn The expiration duration of the access token in seconds.
 */
public record JwtToken(String token, String refreshToken, Date expiresAt, long expiresIn) {

}
