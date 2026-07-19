package com.lamprism.luxspec.security.authentication;

import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.NoOpAccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.security.token.TokenVerifier;
import java.util.Objects;

/**
 * Converts a verified access token into the unified Authentication model.
 *
 * @author RollW
 */
public final class AccessTokenAuthenticator implements Authenticator<AccessTokenCredentials> {
    private static final CredentialType<AccessTokenCredentials> CREDENTIAL_TYPE = CredentialType.of(
            "access-token",
            AccessTokenCredentials.class
    );
    private final TokenVerifier<AccessToken, VerifiedAccessToken> tokenVerifier;
    private final SubjectResolver subjectResolver;
    private final AccessTokenRevocationStore revocationStore;

    /**
     * Creates an authenticator with Token verification and current-subject resolution roles.
     *
     * @param tokenVerifier the authoritative Access Token verifier
     * @param subjectResolver the current subject and state resolver
     */
    public AccessTokenAuthenticator(
            TokenVerifier<AccessToken, VerifiedAccessToken> tokenVerifier,
            SubjectResolver subjectResolver
    ) {
        this(tokenVerifier, subjectResolver, NoOpAccessTokenRevocationStore.getInstance());
    }

    /**
     * Creates an authenticator with an explicit optional access-token revocation store.
     *
     * @param tokenVerifier the authoritative Access Token verifier
     * @param subjectResolver the current subject and state resolver
     * @param revocationStore the optional verified-token revocation store
     */
    public AccessTokenAuthenticator(
            TokenVerifier<AccessToken, VerifiedAccessToken> tokenVerifier,
            SubjectResolver subjectResolver,
            AccessTokenRevocationStore revocationStore
    ) {
        this.tokenVerifier = Objects.requireNonNull(tokenVerifier, "tokenVerifier");
        this.subjectResolver = Objects.requireNonNull(subjectResolver, "subjectResolver");
        this.revocationStore = Objects.requireNonNull(revocationStore, "revocationStore");
    }

    /**
     * Returns the credential type handled by this authenticator.
     *
     * @return the access-token credential type
     */
    @Override
    public CredentialType<AccessTokenCredentials> getCredentialType() {
        return CREDENTIAL_TYPE;
    }

    /**
     * Verifies the token, applies optional revocation state, and resolves the current subject.
     *
     * @param credentials the opaque access-token credentials
     * @return the reconstructed authentication
     * @throws AuthenticationException when a verified token has been revoked
     */
    @Override
    public Authentication authenticate(AccessTokenCredentials credentials) {
        VerifiedAccessToken token = tokenVerifier.verify(
                Objects.requireNonNull(credentials, "credentials").getAccessToken()
        );
        if (revocationStore.isRevoked(token)) {
            throw new AuthenticationException(AuthErrorCode.ACCESS_TOKEN_REVOKED, "Access token was rejected");
        }
        Subject subject = subjectResolver.resolve(token.getSubjectType(), token.getSubjectId());
        return new Authentication(subject, token.getGrants());
    }
}
