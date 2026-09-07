package com.biadevcosta.identity.application.port;

import com.biadevcosta.identity.domain.Role;

/**
 * Access-token issuing port. The infrastructure adapter signs an RS256 JWT with the private key
 * and fills {@code iss}, {@code aud}, {@code iat}, {@code exp} from configuration.
 */
public interface TokenIssuer {

    /**
     * Issues a signed access token for the given user.
     *
     * @param patientId value of the {@code patientId} claim; non-null only for patient users
     */
    AccessToken issueAccessToken(String userId, Role role, String patientId);

    /** A signed access token plus its lifetime, so the caller can report {@code expiresIn}. */
    record AccessToken(String value, long expiresInSeconds) {
    }
}
