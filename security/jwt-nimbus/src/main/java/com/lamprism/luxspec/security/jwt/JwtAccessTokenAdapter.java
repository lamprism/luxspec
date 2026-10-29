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
import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.crypto.KeyEntry;
import com.lamprism.luxspec.security.crypto.KeySet;
import com.lamprism.luxspec.security.crypto.KeySetProvider;
import com.lamprism.luxspec.security.token.IssuedToken;
import com.lamprism.luxspec.security.token.TokenIssuer;
import com.lamprism.luxspec.security.token.access.AccessToken;
import com.lamprism.luxspec.security.token.access.AccessTokenVerifier;
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
import com.nimbusds.jose.crypto.impl.BaseJWSProvider;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.jspecify.annotations.Nullable;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.Provider;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Encodes and verifies short-lived JWT access tokens through Nimbus JOSE+JWT.
 *
 * @author RollW
 */
public class JwtAccessTokenAdapter implements TokenIssuer<AccessToken>, AccessTokenVerifier {
    private static final String SUBJECT_TYPE = "subject_type";
    private static final String SCOPES = "scopes";
    private static final String TOKEN_PURPOSE = "token_purpose";
    private static final Set<String> RESERVED_CLAIMS = Set.of(
            "iss", "sub", "aud", "exp", "nbf", "iat", "jti", SUBJECT_TYPE, SCOPES, TOKEN_PURPOSE
    );
    private static final int MAX_SCOPE_COUNT = 256;
    private static final int MAX_SCOPE_NAME_LENGTH = 256;
    private final KeySetProvider keySetProvider;
    private final String keySetName;
    private final Clock clock;
    private final JwtAccessTokenConfiguration configuration;
    private final List<JwtAccessTokenClaimsPolicy> claimsPolicies;
    private final JwtAccessTokenMissingScopesPolicy missingScopesPolicy;
    private final JwtAlgorithmPolicy algorithmPolicy;

    /**
     * Creates a JWT access-token adapter with trusted key and format collaborators.
     *
     * @param keySetProvider the provider of named atomic key-set snapshots
     * @param keySetName     the provider-owned key-set name
     * @param clock          the token issuance clock
     * @param configuration the trusted JWT format and validation configuration
     */
    public JwtAccessTokenAdapter(
            KeySetProvider keySetProvider,
            String keySetName,
            Clock clock,
            JwtAccessTokenConfiguration configuration
    ) {
        this(keySetProvider, keySetName, clock, configuration, List.of());
    }

    /**
     * Creates an adapter with application-selected additional claim policies.
     *
     * @param keySetProvider the provider of named atomic key-set snapshots
     * @param keySetName     the provider-owned key-set name
     * @param clock          the token issuance clock
     * @param configuration  the trusted JWT format and validation configuration
     * @param claimsPolicies the policies applied in iteration order
     */
    public JwtAccessTokenAdapter(
            KeySetProvider keySetProvider,
            String keySetName,
            Clock clock,
            JwtAccessTokenConfiguration configuration,
            Collection<? extends JwtAccessTokenClaimsPolicy> claimsPolicies
    ) {
        this(keySetProvider, keySetName, clock, configuration, claimsPolicies, JwtAccessTokenMissingScopesPolicy.reject());
    }

    /**
     * Creates an adapter with additional claim and missing-scope policies.
     *
     * @param keySetProvider      the provider of named atomic key-set snapshots
     * @param keySetName          the provider-owned key-set name
     * @param clock               the token issuance clock
     * @param configuration       the trusted JWT format and validation configuration
     * @param claimsPolicies      the policies applied in iteration order
     * @param missingScopesPolicy the policy for a missing scopes claim
     */
    public JwtAccessTokenAdapter(
            KeySetProvider keySetProvider,
            String keySetName,
            Clock clock,
            JwtAccessTokenConfiguration configuration,
            Collection<? extends JwtAccessTokenClaimsPolicy> claimsPolicies,
            JwtAccessTokenMissingScopesPolicy missingScopesPolicy
    ) {
        this(keySetProvider, keySetName, clock, configuration, claimsPolicies, missingScopesPolicy,
                JwtAlgorithmPolicy.standard());
    }

    /**
     * Creates an adapter with explicit claim, scope, and algorithm policies.
     *
     * @param keySetProvider      the named key-set provider
     * @param keySetName          the provider-owned key-set name
     * @param clock               the token issuance clock
     * @param configuration       the trusted JWT configuration
     * @param claimsPolicies      the ordered additional claim policies
     * @param missingScopesPolicy the missing scopes policy
     * @param algorithmPolicy     the signing and trusted verification algorithm policy
     */
    public JwtAccessTokenAdapter(
            KeySetProvider keySetProvider,
            String keySetName,
            Clock clock,
            JwtAccessTokenConfiguration configuration,
            Collection<? extends JwtAccessTokenClaimsPolicy> claimsPolicies,
            JwtAccessTokenMissingScopesPolicy missingScopesPolicy,
            JwtAlgorithmPolicy algorithmPolicy
    ) {
        this.keySetProvider = Objects.requireNonNull(keySetProvider, "keySetProvider");
        this.keySetName = Objects.requireNonNull(keySetName, "keySetName");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.claimsPolicies = List.copyOf(claimsPolicies);
        this.missingScopesPolicy = Objects.requireNonNull(missingScopesPolicy, "missingScopesPolicy");
        this.algorithmPolicy = Objects.requireNonNull(algorithmPolicy, "algorithmPolicy");
    }

    @Override
    public IssuedToken<AccessToken> issue(Authentication authentication, ExecutionContext attributes) {
        Authentication nonNullAuthentication = Objects.requireNonNull(authentication, "authentication");
        ExecutionContext nonNullAttributes = Objects.requireNonNull(attributes, "attributes");
        KeySet keySet = keySet();
        KeyEntry activeKey = keySet.getActiveKey();
        if (activeKey == null) {
            throw new IllegalStateException("No active signing key is available");
        }
        Key signingKey = activeKey.getSigningKey();
        if (signingKey == null) {
            throw new IllegalStateException("Active key does not contain signing material");
        }
        Instant issuedAt = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(configuration.getLifetime());
        List<String> scopes = scopeNames(nonNullAuthentication);
        JWSAlgorithm algorithm = Objects.requireNonNull(
                algorithmPolicy.signingAlgorithm(signingKey, signingAlgorithm(signingKey)), "signing algorithm"
        );
        Key activeVerificationKey = activeKey.getVerificationKey();
        if (!isCompatibleVerificationAlgorithm(activeVerificationKey, algorithm)
                || !algorithmPolicy.permitsVerification(activeVerificationKey, algorithm)) {
            throw new IllegalArgumentException("Signing algorithm is not trusted for the active verification key");
        }
        Subject subject = nonNullAuthentication.subject();
        String tokenId = UUID.randomUUID().toString();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(subject.getId())
                .claim(SUBJECT_TYPE, subject.getType())
                .claim(SCOPES, scopes)
                .claim(TOKEN_PURPOSE, AccessToken.KIND.getName())
                .jwtID(tokenId)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .issuer(configuration.getIssuer());
        if (!configuration.getAudiences().isEmpty()) {
            claims.audience(configuration.getAudiences().stream().toList());
        }
        contributeClaims(claims, nonNullAuthentication, nonNullAttributes, tokenId, issuedAt, expiresAt);
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
        return IssuedToken.of(
                new AccessToken(token),
                issuedAt,
                expiresAt
        );
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
            if (!isCompatibleVerificationAlgorithm(verificationKey, algorithm)
                    || !algorithmPolicy.permitsVerification(verificationKey, algorithm)) {
                throw new IllegalArgumentException("JWT algorithm is not compatible with the verification key");
            }
            if (!signedJwt.verify(verifier(verificationKey, algorithm))) {
                throw new IllegalArgumentException("JWT signature is invalid");
            }
            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            VerifiedAccessToken verified = verifiedToken(claims);
            for (JwtAccessTokenClaimsPolicy policy : claimsPolicies) {
                policy.validate(verified, copyClaims(claims.getClaims()));
            }
            return verified;
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (ParseException | JOSEException | RuntimeException exception) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Access token is invalid", exception);
        }
    }

    @Override
    public Instant getAcceptanceDeadline(VerifiedAccessToken accessToken) {
        VerifiedAccessToken nonNullAccessToken = Objects.requireNonNull(accessToken, "accessToken");
        return acceptanceDeadline(nonNullAccessToken.getExpiresAt());
    }

    private void contributeClaims(
            JWTClaimsSet.Builder builder,
            Authentication authentication,
            ExecutionContext attributes,
            String tokenId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        Map<String, Object> contributed = new LinkedHashMap<>();
        for (JwtAccessTokenClaimsPolicy policy : claimsPolicies) {
            Map<String, ?> claims = Objects.requireNonNull(policy.contribute(
                    authentication, attributes, tokenId, issuedAt, expiresAt,
                    configuration.getIssuer(), configuration.getAudiences()
            ), "contributed claims");
            for (Map.Entry<String, ?> entry : claims.entrySet()) {
                String name = Objects.requireNonNull(entry.getKey(), "claim name");
                if (name.isBlank() || RESERVED_CLAIMS.contains(name) || contributed.containsKey(name)) {
                    throw new IllegalArgumentException("Claim name is reserved, empty, or duplicated: " + name);
                }
                contributed.put(name, entry.getValue());
            }
        }
        contributed.forEach(builder::claim);
    }

    private static Map<String, @Nullable Object> copyClaims(Map<String, Object> claims) {
        Map<String, @Nullable Object> copy = new LinkedHashMap<>();
        claims.forEach((name, value) -> copy.put(name, copyClaimValue(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static @Nullable Object copyClaimValue(@Nullable Object value) {
        if (value instanceof Date date) {
            return new Date(date.getTime());
        }
        if (value instanceof List<?> list) {
            List<@Nullable Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(copyClaimValue(element));
            }
            return Collections.unmodifiableList(copy);
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, @Nullable Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> copy.put(key, copyClaimValue(nested)));
            return Collections.unmodifiableMap(copy);
        }
        return value;
    }

    private KeySet keySet() {
        return Objects.requireNonNull(
                keySetProvider.get(keySetName),
                "keySetProvider.get(keySetName)"
        );
    }

    private VerifiedAccessToken verifiedToken(JWTClaimsSet claims) throws ParseException {
        if (!configuration.getIssuer().equals(claims.getIssuer())) {
            throw new IllegalArgumentException("JWT issuer is not accepted");
        }
        if (!AccessToken.KIND.getName().equals(claims.getStringClaim(TOKEN_PURPOSE))) {
            throw new IllegalArgumentException("JWT token purpose is not accepted");
        }
        requireAudience(claims.getAudience());
        String subjectType = requireText(claims.getStringClaim(SUBJECT_TYPE), SUBJECT_TYPE);
        String subjectId = requireText(claims.getSubject(), "sub");
        String tokenId = requireText(claims.getJWTID(), "jti");
        Instant issuedAt = requireInstant(claims.getIssueTime(), "iat");
        Instant expiresAt = requireInstant(claims.getExpirationTime(), "exp");
        Instant now = clock.instant();
        Instant acceptanceDeadline = acceptanceDeadline(expiresAt);
        if (!acceptanceDeadline.isAfter(now)) {
            throw new IllegalArgumentException("JWT has expired");
        }
        if (issuedAt.minus(configuration.getClockSkew()).isAfter(now)) {
            throw new IllegalArgumentException("JWT was issued in the future");
        }
        Date notBeforeClaim = claims.getNotBeforeTime();
        Instant notBefore = notBeforeClaim == null
                ? null
                : notBeforeClaim.toInstant();
        if (notBefore != null && notBefore.minus(configuration.getClockSkew()).isAfter(now)) {
            throw new IllegalArgumentException("JWT is not active yet");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("JWT expiration must be after issuance");
        }
        return VerifiedAccessToken.of(
                subjectType,
                subjectId,
                resolveGrants(claims, new SimpleSubject(subjectType, subjectId)),
                tokenId,
                issuedAt,
                expiresAt
        );
    }

    private AuthorizationGrantSet resolveGrants(JWTClaimsSet claims, Subject subject) throws ParseException {
        if (!claims.getClaims().containsKey(SCOPES)) {
            return Objects.requireNonNull(missingScopesPolicy.resolve(subject), "missing scopes grants");
        }
        List<?> rawScopes = claims.getListClaim(SCOPES);
        if (rawScopes == null) {
            throw new IllegalArgumentException("JWT scope claim is invalid");
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
        return AuthorizationGrantSet.of(scopes);
    }

    private Instant acceptanceDeadline(Instant expiresAt) {
        return Objects.requireNonNull(expiresAt, "expiresAt").plus(configuration.getClockSkew());
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

    private String requireKeyId(@Nullable String value) {
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
        if (configuration.getAudiences().isEmpty()) {
            return;
        }
        for (String audience : audiences) {
            if (configuration.getAudiences().contains(audience)) {
                return;
            }
        }
        throw new IllegalArgumentException("JWT audience is not accepted");
    }

    private static String requireText(@Nullable String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT claim is missing or blank: " + name);
        }
        return value;
    }

    private static Instant requireInstant(@Nullable Date value, String name) {
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
        if (key instanceof EdECPrivateKey privateKey) {
            edDsaSignatureAlgorithm(privateKey);
            return JWSAlgorithm.EdDSA;
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
        if (key instanceof EdECPrivateKey privateKey) {
            requireAlgorithm(algorithm, JWSAlgorithm.EdDSA);
            return new JcaEdDsaSigner(privateKey);
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
        if (key instanceof EdECPublicKey publicKey) {
            requireEdDsaAlgorithm(algorithm, publicKey);
            return new JcaEdDsaVerifier(publicKey);
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
        if (key instanceof EdECPublicKey publicKey) {
            return isEdDsaAlgorithm(publicKey, algorithm);
        }
        return false;
    }

    private static Set<JWSAlgorithm> edDsaAlgorithms(EdECKey key) {
        return switch (edDsaSignatureAlgorithm(key)) {
            case "Ed25519" -> Set.of(JWSAlgorithm.EdDSA, JWSAlgorithm.Ed25519);
            case "Ed448" -> Set.of(JWSAlgorithm.EdDSA, JWSAlgorithm.Ed448);
            default -> throw new IllegalArgumentException("JWT Edwards curve is unsupported");
        };
    }

    private static String edDsaSignatureAlgorithm(EdECKey key) {
        NamedParameterSpec parameters = key.getParams();
        if (parameters == null) {
            throw new IllegalArgumentException("JWT Edwards key parameters are missing");
        }
        return switch (parameters.getName()) {
            case "Ed25519", "Ed448" -> parameters.getName();
            default -> throw new IllegalArgumentException(
                    "JWT Edwards curve is unsupported: " + parameters.getName()
            );
        };
    }

    private static boolean isEdDsaAlgorithm(EdECKey key, JWSAlgorithm algorithm) {
        return edDsaAlgorithms(key).contains(algorithm);
    }

    private static void requireEdDsaAlgorithm(JWSAlgorithm algorithm, EdECKey key) {
        if (!isEdDsaAlgorithm(key, algorithm)) {
            throw new IllegalArgumentException("JWT algorithm is not compatible with an Edwards key");
        }
    }

    private static Signature createSignature(EdECKey key, JCAContext context)
            throws GeneralSecurityException {
        String algorithm = edDsaSignatureAlgorithm(key);
        Provider provider = context.getProvider();
        if (provider == null) {
            return Signature.getInstance(algorithm);
        }
        return Signature.getInstance(algorithm, provider);
    }

    private static final class JcaEdDsaSigner extends BaseJWSProvider implements JWSSigner {
        private final EdECPrivateKey privateKey;

        private JcaEdDsaSigner(EdECPrivateKey privateKey) {
            super(edDsaAlgorithms(privateKey));
            this.privateKey = privateKey;
        }

        @Override
        public Base64URL sign(JWSHeader header, byte[] signingInput) throws JOSEException {
            if (!supportedJWSAlgorithms().contains(header.getAlgorithm())) {
                throw new JOSEException("JWT algorithm is not supported: " + header.getAlgorithm());
            }
            try {
                Signature signature = createSignature(privateKey, getJCAContext());
                signature.initSign(privateKey, getJCAContext().getSecureRandom());
                signature.update(signingInput);
                return Base64URL.encode(signature.sign());
            } catch (GeneralSecurityException exception) {
                throw new JOSEException("Unable to sign EdDSA JWT", exception);
            }
        }
    }

    private static final class JcaEdDsaVerifier extends BaseJWSProvider implements JWSVerifier {
        private final EdECPublicKey publicKey;

        private JcaEdDsaVerifier(EdECPublicKey publicKey) {
            super(edDsaAlgorithms(publicKey));
            this.publicKey = publicKey;
        }

        @Override
        public boolean verify(JWSHeader header, byte[] signingInput, Base64URL signatureValue)
                throws JOSEException {
            if (!supportedJWSAlgorithms().contains(header.getAlgorithm())) {
                throw new JOSEException("JWT algorithm is not supported: " + header.getAlgorithm());
            }
            try {
                Signature verifier = createSignature(publicKey, getJCAContext());
                verifier.initVerify(publicKey);
                verifier.update(signingInput);
                return verifier.verify(signatureValue.decode());
            } catch (GeneralSecurityException exception) {
                throw new JOSEException("Unable to verify EdDSA JWT", exception);
            }
        }
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

    private static JWSAlgorithm ecdsaAlgorithm(@Nullable ECParameterSpec parameters) {
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
