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
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import com.lamprism.luxspec.security.crypto.KeySetProvider;
import com.lamprism.luxspec.security.token.IssuedToken;
import com.lamprism.luxspec.security.token.TokenIssuance;
import com.lamprism.luxspec.security.token.TokenIssuer;
import com.lamprism.luxspec.security.token.TokenVerifier;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.VerifiedAccessToken;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import javax.crypto.SecretKey;
import java.security.Key;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Encodes and verifies short-lived JWT access tokens through Nimbus JOSE+JWT.
 *
 * @author RollW
 */
public final class JwtAccessTokenAdapter
        implements TokenIssuer, TokenVerifier<AccessToken, VerifiedAccessToken> {
    private static final String SUBJECT_TYPE = "subject_type";
    private static final String SCOPES = "scopes";
    private static final String TOKEN_PURPOSE = "token_purpose";
    private static final int MAX_SCOPE_COUNT = 256;
    private static final int MAX_SCOPE_NAME_LENGTH = 256;
    private final KeySetProvider keySetProvider;
    private final String keySetName;
    private final Clock clock;
    private final JwtAccessTokenOptions options;

    /**
     * Creates a JWT access-token adapter with trusted key and format collaborators.
     *
     * @param keySetProvider the provider of named atomic key-set snapshots
     * @param keySetName     the provider-owned key-set name
     * @param clock          the token issuance clock
     * @param options        trusted JWT format and validation options
     */
    public JwtAccessTokenAdapter(
            KeySetProvider keySetProvider,
            String keySetName,
            Clock clock,
            JwtAccessTokenOptions options
    ) {
        this.keySetProvider = Objects.requireNonNull(keySetProvider, "keySetProvider");
        this.keySetName = Objects.requireNonNull(keySetName, "keySetName");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.options = Objects.requireNonNull(options, "options");
    }

    @Override
    public TokenIssuance issue(Authentication authentication) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        KeySet keySet = keySet();
        KeyEntry activeKey = keySet.getActiveKey().orElseThrow(
                () -> new IllegalStateException("No active signing key is available")
        );
        Key signingKey = activeKey.getSigningKey().orElseThrow(
                () -> new IllegalStateException("Active key does not contain signing material")
        );
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(options.getLifetime());
        List<String> scopes = scopeNames(nonNullAuthentication);
        JWSAlgorithm algorithm = signingAlgorithm(signingKey);
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(nonNullAuthentication.subject().getId())
                .claim(SUBJECT_TYPE, nonNullAuthentication.subject().getType())
                .claim(SCOPES, scopes)
                .claim(TOKEN_PURPOSE, AccessToken.KIND.getName())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .issuer(options.getIssuer());
        if (!options.getAudiences().isEmpty()) {
            claims.audience(options.getAudiences().stream().toList());
        }
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(algorithm).keyID(activeKey.getId()).build(),
                claims.build()
        );
        try {
            signedJwt.sign(signer(signingKey, algorithm));
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to sign access token", exception);
        }
        String token = signedJwt.serialize();
        return TokenIssuance.of(List.of(IssuedToken.of(
                new AccessToken(token),
                issuedAt,
                expiresAt
        )));
    }

    @Override
    public VerifiedAccessToken verify(AccessToken accessToken) {
        KeySet keySet = keySet();
        try {
            SignedJWT signedJwt = SignedJWT.parse(
                    Objects.requireNonNull(accessToken, "accessToken").getValue()
            );
            String keyId = requireKeyId(signedJwt.getHeader().getKeyID());
            Key verificationKey = verificationKey(keySet, keyId);
            JWSAlgorithm algorithm = signedJwt.getHeader().getAlgorithm();
            if (!isCompatibleVerificationAlgorithm(verificationKey, algorithm)) {
                throw new IllegalArgumentException("JWT algorithm is not compatible with the verification key");
            }
            if (!signedJwt.verify(verifier(verificationKey, algorithm))) {
                throw new IllegalArgumentException("JWT signature is invalid");
            }
            return verifiedToken(signedJwt.getJWTClaimsSet());
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (ParseException | JOSEException | RuntimeException exception) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Access token is invalid", exception);
        }
    }

    private KeySet keySet() {
        return Objects.requireNonNull(
                keySetProvider.get(keySetName),
                "keySetProvider.get(keySetName)"
        );
    }

    private VerifiedAccessToken verifiedToken(JWTClaimsSet claims) throws ParseException {
        if (!options.getIssuer().equals(claims.getIssuer())) {
            throw new IllegalArgumentException("JWT issuer is not accepted");
        }
        if (!AccessToken.KIND.getName().equals(claims.getStringClaim(TOKEN_PURPOSE))) {
            throw new IllegalArgumentException("JWT token purpose is not accepted");
        }
        requireAudience(claims.getAudience());
        List<?> rawScopes = claims.getListClaim(SCOPES);
        if (rawScopes == null) {
            throw new IllegalArgumentException("JWT claims are missing scopes");
        }
        requireScopeCount(rawScopes.size());
        LinkedHashSet<AuthorizationScope> scopes = new LinkedHashSet<>();
        for (Object rawScope : rawScopes) {
            if (!(rawScope instanceof String scopeName)) {
                throw new IllegalArgumentException("JWT scope claim contains an invalid value");
            }
            requireScopeNameLength(scopeName);
            if (!scopes.add(AuthorizationScope.of(scopeName))) {
                throw new IllegalArgumentException("JWT scope claim contains a duplicate value");
            }
        }
        String subjectType = requireText(claims.getStringClaim(SUBJECT_TYPE), SUBJECT_TYPE);
        String subjectId = requireText(claims.getSubject(), "sub");
        String tokenId = requireText(claims.getJWTID(), "jti");
        Instant issuedAt = requireInstant(claims.getIssueTime(), "iat");
        Instant expiresAt = requireInstant(claims.getExpirationTime(), "exp");
        Instant now = clock.instant();
        Instant skewedExpiration = expiresAt.plus(options.getClockSkew());
        if (!skewedExpiration.isAfter(now)) {
            throw new IllegalArgumentException("JWT has expired");
        }
        if (issuedAt.minus(options.getClockSkew()).isAfter(now)) {
            throw new IllegalArgumentException("JWT was issued in the future");
        }
        Date notBeforeClaim = claims.getNotBeforeTime();
        Instant notBefore = notBeforeClaim == null
                ? null
                : notBeforeClaim.toInstant();
        if (notBefore != null && notBefore.minus(options.getClockSkew()).isAfter(now)) {
            throw new IllegalArgumentException("JWT is not active yet");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("JWT expiration must be after issuance");
        }
        return VerifiedAccessToken.of(
                subjectType,
                subjectId,
                AuthorizationGrantSet.of(scopes),
                tokenId,
                issuedAt,
                expiresAt
        );
    }

    private List<String> scopeNames(Authentication authentication) {
        List<String> scopes = authentication.grants().getScopes().stream()
                .map(AuthorizationScope::name)
                .toList();
        requireScopeCount(scopes.size());
        for (String scope : scopes) {
            requireScopeNameLength(scope);
        }
        return scopes;
    }

    private static void requireScopeCount(int count) {
        if (count > MAX_SCOPE_COUNT) {
            throw new IllegalArgumentException("JWT scope claim contains too many values");
        }
    }

    private static void requireScopeNameLength(String scopeName) {
        if (scopeName.length() > MAX_SCOPE_NAME_LENGTH) {
            throw new IllegalArgumentException("JWT scope claim value is too long");
        }
    }

    private String requireKeyId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT header is missing a key ID");
        }
        return value;
    }

    private Key verificationKey(KeySet keySet, String keyId) {
        return keySet.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown key ID"))
                .getVerificationKey();
    }

    private void requireAudience(List<String> audiences) {
        if (options.getAudiences().isEmpty()) {
            return;
        }
        for (String audience : audiences) {
            if (options.getAudiences().contains(audience)) {
                return;
            }
        }
        throw new IllegalArgumentException("JWT audience is not accepted");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT claim is missing or blank: " + name);
        }
        return value;
    }

    private static Instant requireInstant(Date value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("JWT claim is missing or invalid: " + name);
        }
        return value.toInstant();
    }

    private static JWSAlgorithm signingAlgorithm(Key key) {
        if (key instanceof SecretKey) {
            return hmacAlgorithm(key);
        }
        if (key instanceof RSAPrivateKey) {
            return JWSAlgorithm.RS256;
        }
        if (key instanceof ECPrivateKey privateKey) {
            return ecdsaAlgorithm(privateKey.getParams());
        }
        throw new IllegalArgumentException("JWT signing key type is unsupported: " + key.getClass().getName());
    }

    private static JWSSigner signer(Key key, JWSAlgorithm algorithm) throws JOSEException {
        if (key instanceof SecretKey secretKey) {
            requireAlgorithm(algorithm, hmacAlgorithm(key));
            return new MACSigner(secretKey);
        }
        if (key instanceof RSAPrivateKey privateKey) {
            requireRsaAlgorithm(algorithm);
            return new RSASSASigner(privateKey);
        }
        if (key instanceof ECPrivateKey privateKey) {
            requireAlgorithm(algorithm, ecdsaAlgorithm(privateKey.getParams()));
            return new ECDSASigner(privateKey);
        }
        throw new IllegalArgumentException("JWT signing key type is unsupported: " + key.getClass().getName());
    }

    private static JWSVerifier verifier(Key key, JWSAlgorithm algorithm) throws JOSEException {
        if (key instanceof SecretKey secretKey) {
            requireAlgorithm(algorithm, hmacAlgorithm(key));
            return new MACVerifier(secretKey);
        }
        if (key instanceof RSAPublicKey publicKey) {
            requireRsaAlgorithm(algorithm);
            return new RSASSAVerifier(publicKey);
        }
        if (key instanceof ECPublicKey publicKey) {
            requireAlgorithm(algorithm, ecdsaAlgorithm(publicKey.getParams()));
            return new ECDSAVerifier(publicKey);
        }
        throw new IllegalArgumentException("JWT verification key type is unsupported: " + key.getClass().getName());
    }

    private static boolean isCompatibleVerificationAlgorithm(Key key, JWSAlgorithm algorithm) {
        if (key instanceof SecretKey) {
            return hmacAlgorithm(key).equals(algorithm);
        }
        if (key instanceof RSAPublicKey) {
            return isRsaAlgorithm(algorithm);
        }
        if (key instanceof ECPublicKey publicKey) {
            return ecdsaAlgorithm(publicKey.getParams()).equals(algorithm);
        }
        return false;
    }

    private static JWSAlgorithm hmacAlgorithm(Key key) {
        String algorithm = key.getAlgorithm().replace("-", "").toUpperCase(Locale.ROOT);
        return switch (algorithm) {
            case "HMACSHA256" -> JWSAlgorithm.HS256;
            case "HMACSHA384" -> JWSAlgorithm.HS384;
            case "HMACSHA512" -> JWSAlgorithm.HS512;
            default -> throw new IllegalArgumentException("JWT HMAC algorithm is unsupported: " + key.getAlgorithm());
        };
    }

    private static JWSAlgorithm ecdsaAlgorithm(ECParameterSpec parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("EC key parameters are missing");
        }
        return switch (parameters.getCurve().getField().getFieldSize()) {
            case 256 -> JWSAlgorithm.ES256;
            case 384 -> JWSAlgorithm.ES384;
            case 521 -> JWSAlgorithm.ES512;
            default -> throw new IllegalArgumentException("EC key curve is unsupported");
        };
    }

    private static void requireRsaAlgorithm(JWSAlgorithm algorithm) {
        if (!isRsaAlgorithm(algorithm)) {
            throw new IllegalArgumentException("JWT algorithm is not compatible with an RSA key");
        }
    }

    private static boolean isRsaAlgorithm(JWSAlgorithm algorithm) {
        return JWSAlgorithm.RS256.equals(algorithm)
                || JWSAlgorithm.RS384.equals(algorithm)
                || JWSAlgorithm.RS512.equals(algorithm);
    }

    private static void requireAlgorithm(JWSAlgorithm actual, JWSAlgorithm expected) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("JWT algorithm is not compatible with the key");
        }
    }
}
