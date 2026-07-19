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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Encodes and verifies short-lived JWT access tokens through JJWT.
 *
 * @author RollW
 */
public final class JwtAccessTokenAdapter
        implements TokenIssuer, TokenVerifier<AccessToken, VerifiedAccessToken> {
    private static final String SUBJECT_TYPE = "subject_type";
    private static final String SCOPES = "scopes";
    private static final String AUDIENCE = "aud";
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
     * @param keySetName the provider-owned key-set name
     * @param clock the token issuance clock
     * @param options trusted JWT format and validation options
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
        String token = Jwts.builder()
                .header().keyId(activeKey.getId()).and()
                .subject(nonNullAuthentication.subject().getId())
                .claim(SUBJECT_TYPE, nonNullAuthentication.subject().getType())
                .claim(SCOPES, scopes)
                .claim(AUDIENCE, options.getAudiences())
                .claim(TOKEN_PURPOSE, AccessToken.KIND.getName())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .issuer(options.getIssuer())
                .signWith(signingKey)
                .compact();
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
            Claims claims = Jwts.parser()
                    .clock(() -> Date.from(clock.instant()))
                    .clockSkewSeconds(options.getClockSkew().toSeconds())
                    .requireIssuer(options.getIssuer())
                    .require(TOKEN_PURPOSE, AccessToken.KIND.getName())
                    .keyLocator(header -> verificationKey(keySet, requireKeyId(header.get("kid"))))
                    .build()
                    .parseSignedClaims(Objects.requireNonNull(accessToken, "accessToken").getValue())
                    .getPayload();
            return verifiedToken(claims);
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Access token is invalid", exception);
        }
    }

    private KeySet keySet() {
        return Objects.requireNonNull(
                keySetProvider.get(keySetName),
                "keySetProvider.get(keySetName)"
        );
    }

    private VerifiedAccessToken verifiedToken(Claims claims) {
        requireAudience(claims.get(AUDIENCE));
        List<?> rawScopes = claims.get(SCOPES, List.class);
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
        return VerifiedAccessToken.of(
                claims.get(SUBJECT_TYPE, String.class),
                claims.getSubject(),
                AuthorizationGrantSet.of(scopes),
                claims.getId(),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
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

    private String requireKeyId(Object value) {
        if (!(value instanceof String keyId) || keyId.isBlank()) {
            throw new IllegalArgumentException("JWT header is missing a key ID");
        }
        return keyId;
    }

    private Key verificationKey(KeySet keySet, String keyId) {
        return keySet.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown key ID"))
                .getVerificationKey();
    }

    private void requireAudience(Object rawAudience) {
        if (options.getAudiences().isEmpty()) {
            return;
        }
        if (rawAudience instanceof String audience && options.getAudiences().contains(audience)) {
            return;
        }
        if (rawAudience instanceof Iterable<?> audiences) {
            for (Object audience : audiences) {
                if (audience instanceof String value && options.getAudiences().contains(value)) {
                    return;
                }
            }
        }
        throw new IllegalArgumentException("JWT audience is not accepted");
    }
}
