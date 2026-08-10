/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.security.jwt;

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
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void issuesAndVerifiesAnAccessTokenUsingAnRsaKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        KeySet keySet = KeySet.withActiveKey(
                "active",
                List.of(new KeyEntry("active", keyPair.getPrivate(), keyPair.getPublic()))
        );
        JwtAccessTokenAdapter adapter = adapter(keySet);

        Authentication authentication = new Authentication(
                new UserSubject(42L),
                AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE)))
        );

        VerifiedAccessToken token = adapter.verify(
                adapter.issue(authentication).require(AccessToken.KIND).getToken()
        );

        assertEquals("42", token.getSubjectId());
    }

    @Test
    void rejectsATokenWhoseKeyIdIsNotTrusted() {
        Fixture fixture = new Fixture();
        SecretKey untrustedKey = key("abcdefghijklmnopqrstuvwxyz012345");
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
            SecretKey signingKey,
            String keyId,
            String purpose,
            List<String> scopes
    ) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("42")
                .claim("subject_type", "user")
                .claim("scopes", scopes)
                .audience(AUDIENCE)
                .claim("token_purpose", purpose)
                .jwtID("token")
                .issueTime(Date.from(ISSUED_AT))
                .expirationTime(Date.from(ISSUED_AT.plus(Duration.ofMinutes(5))))
                .issuer(ISSUER)
                .build();
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(keyId).build(),
                claims
        );
        try {
            signedJwt.sign(new MACSigner(signingKey));
            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to create test token", exception);
        }
    }

    private static SecretKey key(String value) {
        return new SecretKeySpec(value.getBytes(StandardCharsets.US_ASCII), "HmacSHA256");
    }

    private static final class Fixture {
        private final SecretKey activeKey = key("01234567890123456789012345678901");
        private final KeySet keySet;
        private final JwtAccessTokenAdapter adapter;

        private Fixture() {
            keySet = KeySet.withActiveKey(
                    "active",
                    List.of(new KeyEntry("active", activeKey, activeKey))
            );
            adapter = adapter(keySet);
        }
    }

    private static JwtAccessTokenAdapter adapter(KeySet keySet) {
        return new JwtAccessTokenAdapter(
                keySetName -> keySet,
                "access",
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC),
                new JwtAccessTokenOptions(
                        Duration.ofMinutes(5),
                        ISSUER,
                        Set.of(AUDIENCE),
                        Duration.ZERO
                )
        );
    }
}
