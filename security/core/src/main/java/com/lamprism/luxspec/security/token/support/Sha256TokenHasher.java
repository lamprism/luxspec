package com.lamprism.luxspec.security.token.support;

import com.lamprism.luxspec.security.token.Token;
import com.lamprism.luxspec.security.token.TokenDigest;
import com.lamprism.luxspec.security.token.TokenHasher;
import com.lamprism.luxspec.security.token.TokenKind;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Produces SHA-256 Digests for one configured Token Kind.
 *
 * @param <T> the Token value type
 * @author RollW
 */
public final class Sha256TokenHasher<T extends Token> implements TokenHasher<T> {
    private static final String ALGORITHM = "SHA-256";

    private final TokenKind<T> tokenKind;

    /**
     * Creates a hasher for one exact Token Kind.
     *
     * @param tokenKind the accepted Token Kind
     */
    public Sha256TokenHasher(TokenKind<T> tokenKind) {
        this.tokenKind = Objects.requireNonNull(tokenKind, "tokenKind");
    }

    @Override
    public TokenDigest<T> hash(T token) {
        T nonNullToken = Objects.requireNonNull(token, "token");
        if (!tokenKind.matches(nonNullToken)) {
            throw new IllegalArgumentException("Token does not match the configured Token Kind");
        }
        return new TokenDigest<>(tokenKind, digest(nonNullToken.getValue()));
    }

    private static byte[] digest(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
            return messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
