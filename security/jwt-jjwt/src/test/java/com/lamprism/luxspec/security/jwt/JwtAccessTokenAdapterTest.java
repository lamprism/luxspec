package com.lamprism.luxspec.security.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import com.lamprism.luxspec.security.token.TokenIssuance;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class JwtAccessTokenAdapterTest {
    private static final Instant ISSUED_AT = Instant.parse("2026-07-12T00:00:00Z");
    private static final String ISSUER = "luxspec-test";
    private static final String AUDIENCE = "luxspec-client";
    private static final String SCOPE = "resource:document:read";

    @Test
    void issuesAndVerifiesAnAccessTokenUsingTheActiveKey() {
        Fixture fixture = new Fixture();
        Authentication authentication = new Authentication(
                new UserSubject(42L),
                AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE)))
        );

        TokenIssuance issuance = fixture.adapter.issue(authentication);
        VerifiedAccessToken token = fixture.adapter.verify(
                issuance.require(AccessToken.KIND).getToken()
        );

        assertEquals("user", token.getSubjectType());
        assertEquals("42", token.getSubjectId());
        assertEquals(AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE))), token.getGrants());
        assertEquals(ISSUED_AT, issuance.require(AccessToken.KIND).getIssuedAt());
        assertEquals(ISSUED_AT.plus(Duration.ofMinutes(5)), token.getExpiresAt());
    }

    @Test
    void rejectsATokenWhoseKeyIdIsNotTrusted() {
        Fixture fixture = new Fixture();
        Key untrustedKey = key("abcdefghijklmnopqrstuvwxyz012345");
        String encoded = signedToken(
                untrustedKey,
                "unknown",
                AccessToken.KIND.getName(),
                List.of(SCOPE)
        );

        assertInvalidToken(fixture, encoded);
    }

    @Test
    void rejectsATokenWhosePurposeIsNotAccess() {
        Fixture fixture = new Fixture();
        String encoded = signedToken(
                fixture.activeKey,
                "active",
                "refresh",
                List.of(SCOPE)
        );

        assertInvalidToken(fixture, encoded);
    }

    @Test
    void rejectsAnExcessiveScopeClaim() {
        Fixture fixture = new Fixture();
        List<String> scopes = IntStream.range(0, 257)
                .mapToObj(index -> "resource:item-" + index + ":read")
                .toList();
        String encoded = signedToken(
                fixture.activeKey,
                "active",
                AccessToken.KIND.getName(),
                scopes
        );

        assertInvalidToken(fixture, encoded);
    }

    private static void assertInvalidToken(Fixture fixture, String encoded) {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> fixture.adapter.verify(new AccessToken(encoded))
        );

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    private static String signedToken(
            Key signingKey,
            String keyId,
            String purpose,
            List<String> scopes
    ) {
        return Jwts.builder()
                .header().keyId(keyId).and()
                .subject("42")
                .claim("subject_type", "user")
                .claim("scopes", scopes)
                .claim("aud", List.of(AUDIENCE))
                .claim("token_purpose", purpose)
                .id("token")
                .issuedAt(Date.from(ISSUED_AT))
                .expiration(Date.from(ISSUED_AT.plus(Duration.ofMinutes(5))))
                .issuer(ISSUER)
                .signWith(signingKey)
                .compact();
    }

    private static Key key(String value) {
        return Keys.hmacShaKeyFor(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static final class Fixture {
        private final Key activeKey = key("01234567890123456789012345678901");
        private final JwtAccessTokenAdapter adapter;

        private Fixture() {
            KeySet keySet = KeySet.withActiveKey(
                    "active",
                    List.of(new KeyEntry("active", activeKey, activeKey))
            );
            Clock clock = Clock.fixed(ISSUED_AT, ZoneOffset.UTC);
            adapter = new JwtAccessTokenAdapter(
                    keySetName -> keySet,
                    "access",
                    clock,
                    new JwtAccessTokenOptions(
                            Duration.ofMinutes(5),
                            ISSUER,
                            Set.of(AUDIENCE),
                            Duration.ZERO
                    )
            );
        }
    }
}
