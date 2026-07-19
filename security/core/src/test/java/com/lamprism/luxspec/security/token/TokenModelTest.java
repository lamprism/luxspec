package com.lamprism.luxspec.security.token;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.refresh.RefreshToken;
import com.lamprism.luxspec.security.token.support.Sha256TokenHasher;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenModelTest {
    private static final Instant ISSUED_AT = Instant.parse("2026-07-15T00:00:00Z");
    private static final Instant EXPIRES_AT = ISSUED_AT.plusSeconds(300);

    @Test
    void providesTypedLookupForImmutableIssuedTokens() {
        AccessToken accessToken = new AccessToken("sensitive-access-token");
        RefreshToken refreshToken = new RefreshToken("sensitive-refresh-token");
        TokenIssuance issuance = TokenIssuance.of(List.of(
                IssuedToken.of(accessToken, ISSUED_AT, EXPIRES_AT),
                IssuedToken.of(refreshToken, ISSUED_AT, EXPIRES_AT)
        ));

        assertSame(accessToken, issuance.require(AccessToken.KIND).getToken());
        assertSame(refreshToken, issuance.find(RefreshToken.KIND).orElseThrow().getToken());
        assertEquals(2, issuance.getTokens().size());
        assertEquals(2, issuance.getKinds().size());
        assertThrows(UnsupportedOperationException.class, issuance.getTokens()::clear);
        assertThrows(UnsupportedOperationException.class, issuance.getKinds()::clear);
    }

    @Test
    void rejectsInvalidIssuanceShapesAndLifetimes() {
        IssuedToken<AccessToken> accessToken = IssuedToken.of(
                new AccessToken("access-token"),
                ISSUED_AT,
                EXPIRES_AT
        );

        assertThrows(IllegalArgumentException.class, () -> TokenIssuance.of(List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> TokenIssuance.of(List.of(accessToken, accessToken))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> IssuedToken.of(new AccessToken("access-token"), ISSUED_AT, ISSUED_AT)
        );
    }

    @Test
    void keepsSensitiveValuesOutOfStringRepresentations() {
        String accessValue = "sensitive-access-token";
        String refreshValue = "sensitive-refresh-token";
        AccessToken accessToken = new AccessToken(accessValue);
        RefreshToken refreshToken = new RefreshToken(refreshValue);
        IssuedToken<AccessToken> issuedAccessToken = IssuedToken.of(accessToken, ISSUED_AT, EXPIRES_AT);
        TokenIssuance issuance = TokenIssuance.of(List.of(
                issuedAccessToken,
                IssuedToken.of(refreshToken, ISSUED_AT, EXPIRES_AT)
        ));
        TokenDigest<AccessToken> digest = new Sha256TokenHasher<>(AccessToken.KIND).hash(accessToken);

        assertFalse(accessToken.toString().contains(accessValue));
        assertFalse(refreshToken.toString().contains(refreshValue));
        assertFalse(issuedAccessToken.toString().contains(accessValue));
        assertFalse(issuance.toString().contains(accessValue));
        assertFalse(issuance.toString().contains(refreshValue));
        assertFalse(digest.toString().contains(accessValue));
    }

    @Test
    void protectsDigestBytesAndPreservesTokenKindIdentity() {
        byte[] source = new byte[] {1, 2, 3};
        TokenDigest<AccessToken> accessDigest = new TokenDigest<>(AccessToken.KIND, source);
        source[0] = 9;
        byte[] exposed = accessDigest.getValue();
        exposed[1] = 9;
        TokenDigest<RefreshToken> refreshDigest = new TokenDigest<>(RefreshToken.KIND, new byte[] {1, 2, 3});

        assertArrayEquals(new byte[] {1, 2, 3}, accessDigest.getValue());
        assertNotEquals(accessDigest, refreshDigest);
        assertEquals(
                new Sha256TokenHasher<>(AccessToken.KIND).hash(new AccessToken("same-value")),
                new Sha256TokenHasher<>(AccessToken.KIND).hash(new AccessToken("same-value"))
        );
    }

    @Test
    void validatesTokenKindsAndRotationCommands() {
        TokenKind<AccessToken> equivalentKind = TokenKind.of("access", AccessToken.class);
        TokenDigest<AccessToken> presented = new TokenDigest<>(AccessToken.KIND, new byte[] {1});
        TokenDigest<AccessToken> successor = new TokenDigest<>(AccessToken.KIND, new byte[] {2});
        TokenRotation<AccessToken> rotation = new TokenRotation<>(presented, successor, ISSUED_AT);
        TokenRotationResult<AccessToken> succeeded = TokenRotationResult.succeeded(new AccessToken("successor"));

        assertEquals(AccessToken.KIND, equivalentKind);
        assertTrue(AccessToken.KIND.matches(new AccessToken("access-token")));
        assertEquals(presented, rotation.getPresentedDigest());
        assertEquals(successor, rotation.getSuccessorDigest());
        assertTrue(succeeded instanceof TokenRotationResult.Succeeded<AccessToken>);
        assertThrows(
                IllegalArgumentException.class,
                () -> new TokenRotation<>(presented, presented, ISSUED_AT)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> TokenKind.of("Access_Token", AccessToken.class)
        );
    }
}
