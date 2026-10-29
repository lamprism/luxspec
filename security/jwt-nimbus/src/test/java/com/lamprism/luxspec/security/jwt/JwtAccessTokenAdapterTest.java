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
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import com.lamprism.luxspec.security.token.IssuedToken;

import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.InMemoryAccessTokenRevocationStore;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.jspecify.annotations.Nullable;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
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

        IssuedToken<AccessToken> issuance = fixture.adapter.issue(authentication);
        VerifiedAccessToken token = fixture.adapter.verify(
                issuance.getToken()
        );

        assertEquals("user", token.getSubjectType());
        assertEquals("42", token.getSubjectId());
        assertEquals(AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE))), token.getGrants());
        assertEquals(ISSUED_AT, issuance.getIssuedAt());
        assertEquals(ISSUED_AT.plus(Duration.ofMinutes(5)), token.getExpiresAt());
    }

    @Test
    void appliesClaimPoliciesOnlyToVerifiedTokens() {
        Fixture fixture = new Fixture();
        AtomicInteger securityVersion = new AtomicInteger(1);
        AtomicInteger validationCount = new AtomicInteger();
        JwtAccessTokenClaimsPolicy policy = new JwtAccessTokenClaimsPolicy() {
            @Override
            public Map<String, ?> contribute(Authentication authentication, ExecutionContext attributes,
                                             String tokenId, Instant issuedAt, Instant expiresAt, String issuer, Set<String> audiences) {
                return Map.of("security_version", securityVersion.get());
            }

            @Override
            public void validate(VerifiedAccessToken token, Map<String, Object> claims) {
                validationCount.incrementAndGet();
                if (!(claims.get("security_version") instanceof Number version)
                        || version.intValue() != securityVersion.get()) {
                    throw new IllegalArgumentException("Security version changed");
                }
            }
        };
        JwtAccessTokenAdapter adapter = new JwtAccessTokenAdapter(
                name -> fixture.keySet,
                "access",
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC),
                new JwtAccessTokenConfiguration(Duration.ofMinutes(5), ISSUER, Set.of(AUDIENCE), Duration.ZERO),
                List.of(policy)
        );
        Authentication authentication = new Authentication(
                new UserSubject(42L), AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE)))
        );
        AccessToken token = adapter.issue(authentication).getToken();

        adapter.verify(token);
        assertEquals(1, validationCount.get());
        String value = token.getValue();
        int signatureStart = value.lastIndexOf('.') + 1;
        char replacement = value.charAt(signatureStart) == 'A' ? 'B' : 'A';
        AccessToken tampered = new AccessToken(
                value.substring(0, signatureStart) + replacement + value.substring(signatureStart + 1)
        );
        assertThrows(AuthenticationException.class, () -> adapter.verify(tampered));
        assertEquals(1, validationCount.get());
        securityVersion.incrementAndGet();
        assertThrows(AuthenticationException.class, () -> adapter.verify(token));
        assertEquals(2, validationCount.get());
    }

    @Test
    void rejectsReservedAndDuplicateContributedClaims() {
        Fixture fixture = new Fixture();
        Authentication authentication = new Authentication(
                new UserSubject(42L), AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE)))
        );
        JwtAccessTokenClaimsPolicy reserved = claimPolicy("sub", "another-user");
        JwtAccessTokenClaimsPolicy first = claimPolicy("application_version", 1);
        JwtAccessTokenClaimsPolicy second = claimPolicy("application_version", 2);

        assertThrows(IllegalArgumentException.class, () -> adapter(fixture.keySet, List.of(reserved)).issue(authentication));
        assertThrows(IllegalArgumentException.class, () -> adapter(fixture.keySet, List.of(first, second)).issue(authentication));
    }

    @Test
    void includesClockSkewInTheRevocationAcceptanceWindow() {
        SecretKey activeKey = key("01234567890123456789012345678901");
        KeySet keySet = KeySet.withActiveKey(
                "active",
                List.of(new KeyEntry("active", activeKey, activeKey))
        );
        JwtAccessTokenAdapter adapter = new JwtAccessTokenAdapter(
                keySetName -> keySet,
                "access",
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC),
                new JwtAccessTokenConfiguration(
                        Duration.ofMinutes(5),
                        ISSUER,
                        Set.of(AUDIENCE),
                        Duration.ofSeconds(30)
                )
        );
        Authentication authentication = new Authentication(
                new UserSubject(42L),
                AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE)))
        );

        VerifiedAccessToken verified = adapter.verify(
                adapter.issue(authentication).getToken()
        );

        Instant expiresAt = verified.getExpiresAt();
        Instant acceptanceDeadline = adapter.getAcceptanceDeadline(verified);
        InMemoryAccessTokenRevocationStore acceptedDuringClockSkew = new InMemoryAccessTokenRevocationStore(
                Clock.fixed(expiresAt.plusSeconds(29L), ZoneOffset.UTC)
        );
        InMemoryAccessTokenRevocationStore rejectedAfterClockSkew = new InMemoryAccessTokenRevocationStore(
                Clock.fixed(expiresAt.plusSeconds(30L), ZoneOffset.UTC)
        );

        acceptedDuringClockSkew.revoke(verified, acceptanceDeadline);
        rejectedAfterClockSkew.revoke(verified, acceptanceDeadline);

        assertEquals(1, acceptedDuringClockSkew.size());
        assertEquals(0, rejectedAfterClockSkew.size());
    }

    @Test
    void rejectsSubsecondTokenLifetimesThatJwtNumericDatesCannotRepresent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtAccessTokenConfiguration(
                        Duration.ofMillis(500L),
                        ISSUER,
                        Set.of(),
                        Duration.ZERO
                )
        );
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
                adapter.issue(authentication).getToken()
        );

        assertEquals("42", token.getSubjectId());
    }

    @Test
    void selectsAndRestrictsTrustedAlgorithmsWithoutWeakeningKeyChecks() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        KeySet keys = KeySet.withActiveKey("active", List.of(new KeyEntry("active", pair.getPrivate(), pair.getPublic())));
        JwtAccessTokenConfiguration configuration = new JwtAccessTokenConfiguration(
                Duration.ofMinutes(5), ISSUER, Set.of(AUDIENCE), Duration.ZERO
        );
        JwtAccessTokenAdapter issuer = new JwtAccessTokenAdapter(
                name -> keys, "access", Clock.fixed(ISSUED_AT, ZoneOffset.UTC), configuration, List.of(),
                JwtAccessTokenMissingScopesPolicy.reject(), JwtAlgorithmPolicy.fixed(JWSAlgorithm.RS512, Set.of(JWSAlgorithm.RS512))
        );
        JwtAccessTokenAdapter restricted = new JwtAccessTokenAdapter(
                name -> keys, "access", Clock.fixed(ISSUED_AT, ZoneOffset.UTC), configuration, List.of(),
                JwtAccessTokenMissingScopesPolicy.reject(), JwtAlgorithmPolicy.fixed(JWSAlgorithm.RS256, Set.of(JWSAlgorithm.RS256))
        );
        Authentication authentication = new Authentication(new UserSubject(42L), AuthorizationGrantSet.of(List.of()));

        AccessToken token = issuer.issue(authentication).getToken();

        assertEquals(JWSAlgorithm.RS512, SignedJWT.parse(token.getValue()).getHeader().getAlgorithm());
        assertEquals("42", issuer.verify(token).getSubjectId());
        assertThrows(AuthenticationException.class, () -> restricted.verify(token));
        JwtAccessTokenAdapter incompatible = new JwtAccessTokenAdapter(
                name -> keys, "access", Clock.fixed(ISSUED_AT, ZoneOffset.UTC), configuration, List.of(),
                JwtAccessTokenMissingScopesPolicy.reject(), JwtAlgorithmPolicy.fixed(JWSAlgorithm.HS256, Set.of(JWSAlgorithm.HS256))
        );
        assertThrows(IllegalArgumentException.class, () -> incompatible.issue(authentication));
    }

    @Test
    void issuesAndVerifiesAnAccessTokenUsingAnEd25519KeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        assertIssuesAndVerifies(generator.generateKeyPair());
    }

    @Test
    void issuesAndVerifiesAnAccessTokenUsingAnEd448KeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed448");
        assertIssuesAndVerifies(generator.generateKeyPair());
    }

    @Test
    void rejectsAnHmacTokenForAnEd25519VerificationKey() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = generator.generateKeyPair();
        JwtAccessTokenAdapter adapter = adapter(KeySet.forVerification(
                List.of(new KeyEntry("active", keyPair.getPublic()))
        ));
        String encoded = signedToken(
                key("abcdefghijklmnopqrstuvwxyz012345"),
                "active",
                AccessToken.KIND.getName(),
                List.of(SCOPE)
        );

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> adapter.verify(new AccessToken(encoded))
        );

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.getErrorCode());
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

    @Test
    void appliesTheMissingScopesPolicyOnlyAfterStandardVerification() {
        Fixture fixture = new Fixture();
        String unscoped = signedToken(fixture.activeKey, "active", AccessToken.KIND.getName(), List.of(), false);
        assertInvalidToken(fixture, unscoped);

        AtomicInteger resolutions = new AtomicInteger();
        JwtAccessTokenMissingScopesPolicy policy = subject -> {
            assertEquals("user", subject.getType());
            assertEquals("42", subject.getId());
            resolutions.incrementAndGet();
            return AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE)));
        };
        JwtAccessTokenConfiguration configuration = new JwtAccessTokenConfiguration(
                Duration.ofMinutes(5), ISSUER, Set.of(AUDIENCE), Duration.ZERO
        );
        JwtAccessTokenAdapter adapter = new JwtAccessTokenAdapter(
                name -> fixture.keySet, "access", Clock.fixed(ISSUED_AT, ZoneOffset.UTC),
                configuration, List.of(), policy
        );
        assertEquals(AuthorizationGrantSet.of(List.of(AuthorizationScope.of(SCOPE))),
                adapter.verify(new AccessToken(unscoped)).getGrants());
        assertEquals(1, resolutions.get());

        String emptyScopes = signedToken(fixture.activeKey, "active", AccessToken.KIND.getName(), List.of());
        assertEquals(AuthorizationGrantSet.of(List.of()), adapter.verify(new AccessToken(emptyScopes)).getGrants());
        String malformedScopes = signedToken(fixture.activeKey, "active", AccessToken.KIND.getName(), "invalid", true);
        assertThrows(AuthenticationException.class, () -> adapter.verify(new AccessToken(malformedScopes)));
        String nullScopes = signedToken(fixture.activeKey, "active", AccessToken.KIND.getName(), null, true);
        assertThrows(AuthenticationException.class, () -> adapter.verify(new AccessToken(nullScopes)));
        String badSignature = signedToken(
                key("abcdefghijklmnopqrstuvwxyz012345"), "active", AccessToken.KIND.getName(), List.of(), false
        );
        assertThrows(AuthenticationException.class, () -> adapter.verify(new AccessToken(badSignature)));
        JwtAccessTokenAdapter expiredAdapter = new JwtAccessTokenAdapter(
                name -> fixture.keySet, "access", Clock.fixed(ISSUED_AT.plusSeconds(301), ZoneOffset.UTC),
                configuration, List.of(), policy
        );
        assertThrows(AuthenticationException.class, () -> expiredAdapter.verify(new AccessToken(unscoped)));
        assertEquals(1, resolutions.get());

        JwtAccessTokenAdapter noGrantsAdapter = new JwtAccessTokenAdapter(
                name -> fixture.keySet, "access", Clock.fixed(ISSUED_AT, ZoneOffset.UTC),
                configuration, List.of(), JwtAccessTokenMissingScopesPolicy.noGrants()
        );
        assertEquals(AuthorizationGrantSet.of(List.of()), noGrantsAdapter.verify(new AccessToken(unscoped)).getGrants());
    }

    private static void assertInvalidToken(Fixture fixture, String encoded) {
        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> fixture.adapter.verify(new AccessToken(encoded))
        );

        assertEquals(AuthErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    private static void assertIssuesAndVerifies(KeyPair keyPair) {
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
                adapter.issue(authentication).getToken()
        );

        assertEquals("42", token.getSubjectId());
    }

    private static String signedToken(
            SecretKey signingKey,
            String keyId,
            String purpose,
            List<String> scopes
    ) {
        return signedToken(signingKey, keyId, purpose, scopes, true);
    }

    private static String signedToken(
            SecretKey signingKey,
            String keyId,
            String purpose,
            @Nullable Object scopes,
            boolean includeScopes
    ) {
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder().serializeNullClaims(true)
                .subject("42")
                .claim("subject_type", "user")
                .audience(AUDIENCE)
                .claim("token_purpose", purpose)
                .jwtID("token")
                .issueTime(Date.from(ISSUED_AT))
                .expirationTime(Date.from(ISSUED_AT.plus(Duration.ofMinutes(5))))
                .issuer(ISSUER);
        if (includeScopes) {
            claimsBuilder.claim("scopes", scopes);
        }
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(keyId).build(),
                claimsBuilder.build()
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
        return adapter(keySet, List.of());
    }

    private static JwtAccessTokenAdapter adapter(KeySet keySet, List<JwtAccessTokenClaimsPolicy> policies) {
        return new JwtAccessTokenAdapter(
                keySetName -> keySet,
                "access",
                Clock.fixed(ISSUED_AT, ZoneOffset.UTC),
                new JwtAccessTokenConfiguration(
                        Duration.ofMinutes(5),
                        ISSUER,
                        Set.of(AUDIENCE),
                        Duration.ZERO
                ),
                policies
        );
    }

    private static JwtAccessTokenClaimsPolicy claimPolicy(String name, Object value) {
        return new JwtAccessTokenClaimsPolicy() {
            @Override
            public Map<String, ?> contribute(Authentication authentication, ExecutionContext attributes,
                                             String tokenId, Instant issuedAt, Instant expiresAt, String issuer, Set<String> audiences) {
                return Map.of(name, value);
            }
        };
    }
}
