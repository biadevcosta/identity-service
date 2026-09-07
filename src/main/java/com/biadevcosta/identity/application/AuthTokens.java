package com.biadevcosta.identity.application;

/** Result of a successful login or token refresh: a signed access token plus an opaque refresh token. */
public record AuthTokens(String accessToken, String refreshToken, long expiresInSeconds) {
}
