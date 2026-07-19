package com.lamprism.luxspec.security.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.token.TokenVerifier;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.AccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class AccessTokenAuthenticatorTest {
    private static final VerifiedAccessToken VERIFIED_TOKEN = VerifiedAccessToken.of(
            "user",
            "42",
            AuthorizationGrantSet.of(List.of(AuthorizationScope.of("account:read"))),
            "access-token-id",
            Instant.parse("2026-07-12T00:00:00Z"),
            Instant.parse("2026-07-12T00:05:00Z")
    );

    @Test
    void authenticatesWithoutAnOptionalRevocationStore() {
        TestSubjectResolver subjectResolver = new TestSubjectResolver();
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                subjectResolver
        );

        Authentication authentication = authenticator.authenticate(credentials());

        assertEquals(new UserSubject(42L).getId(), authentication.subject().getId());
        assertEquals(VERIFIED_TOKEN.getGrants(), authentication.grants());
        assertEquals(1, subjectResolver.getInvocationCount());
    }

    @Test
    void rejectsARevokedTokenBeforeResolvingTheSubject() {
        TestSubjectResolver subjectResolver = new TestSubjectResolver();
        TrackingRevocationStore revocationStore = new TrackingRevocationStore();
        revocationStore.revoke(VERIFIED_TOKEN);
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(
                new TestTokenVerifier(),
                subjectResolver,
                revocationStore
        );

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> authenticator.authenticate(credentials())
        );

        assertEquals(AuthErrorCode.ACCESS_TOKEN_REVOKED, exception.getErrorCode());
        assertEquals(1, revocationStore.getCheckCount());
        assertEquals(0, subjectResolver.getInvocationCount());
    }

    private static AccessTokenCredentials credentials() {
        return new AccessTokenCredentials(new AccessToken("encoded-access-token"));
    }

    private static final class TestTokenVerifier
            implements TokenVerifier<AccessToken, VerifiedAccessToken> {
        @Override
        public VerifiedAccessToken verify(AccessToken accessToken) {
            Objects.requireNonNull(accessToken, "accessToken");
            return VERIFIED_TOKEN;
        }
    }

    private static final class TestSubjectResolver implements SubjectResolver {
        private int invocationCount;

        @Override
        public Subject resolve(String subjectType, String subjectId) {
            if (!"user".equals(subjectType)) {
                throw new IllegalArgumentException("Unexpected subject type");
            }
            invocationCount++;
            return new UserSubject(Long.parseLong(subjectId));
        }

        private int getInvocationCount() {
            return invocationCount;
        }
    }

    private static final class TrackingRevocationStore implements AccessTokenRevocationStore {
        private String revokedTokenId;
        private int checkCount;

        @Override
        public void revoke(VerifiedAccessToken accessToken) {
            revokedTokenId = Objects.requireNonNull(accessToken, "accessToken").getTokenId();
        }

        @Override
        public boolean isRevoked(VerifiedAccessToken accessToken) {
            checkCount++;
            return Objects.requireNonNull(accessToken, "accessToken").getTokenId().equals(revokedTokenId);
        }

        private int getCheckCount() {
            return checkCount;
        }
    }
}
