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

package com.lamprism.luxspec.security.token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class ImmutableTokenIssuance implements TokenIssuance {
    private final List<IssuedToken<? extends Token>> tokens;
    private final Map<String, IssuedToken<?>> tokensByKindName;
    private final Set<TokenKind<?>> kinds;

    ImmutableTokenIssuance(Collection<? extends IssuedToken<? extends Token>> tokens) {
        Objects.requireNonNull(tokens, "tokens");
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Token issuance must not be empty");
        }
        LinkedHashMap<String, IssuedToken<?>> byKindName = new LinkedHashMap<>();
        LinkedHashSet<TokenKind<?>> tokenKinds = new LinkedHashSet<>();
        ArrayList<IssuedToken<? extends Token>> tokenSnapshots = new ArrayList<>();
        for (IssuedToken<? extends Token> token : tokens) {
            IssuedToken<?> snapshot = snapshot(Objects.requireNonNull(token, "token"));
            TokenKind<?> kind = snapshot.getToken().getKind();
            if (!kind.matches(snapshot.getToken())) {
                throw new IllegalArgumentException("Issued token does not match its token kind");
            }
            if (byKindName.putIfAbsent(kind.getName(), snapshot) != null) {
                throw new IllegalArgumentException("Duplicate issued token kind: " + kind.getName());
            }
            tokenKinds.add(kind);
            tokenSnapshots.add(snapshot);
        }
        this.tokens = List.copyOf(tokenSnapshots);
        this.tokensByKindName = Map.copyOf(byKindName);
        this.kinds = Set.copyOf(tokenKinds);
    }

    @Override
    public Collection<IssuedToken<? extends Token>> getTokens() {
        return tokens;
    }

    @Override
    public <T extends Token> Optional<IssuedToken<T>> find(TokenKind<T> kind) {
        TokenKind<T> nonNullKind = Objects.requireNonNull(kind, "kind");
        IssuedToken<?> token = tokensByKindName.get(nonNullKind.getName());
        if (token == null) {
            return Optional.empty();
        }
        if (!nonNullKind.matches(token.getToken())) {
            throw new IllegalArgumentException("Token kind name is associated with a different Java type");
        }
        return Optional.of(cast(token, nonNullKind));
    }

    @Override
    public <T extends Token> IssuedToken<T> require(TokenKind<T> kind) {
        return find(kind).orElseThrow(
                () -> new IllegalArgumentException("Required token kind was not issued: " + kind.getName())
        );
    }

    @Override
    public Set<TokenKind<?>> getKinds() {
        return kinds;
    }

    @Override
    public String toString() {
        return "TokenIssuance[kinds=" + kinds + "]";
    }

    private static <T extends Token> IssuedToken<T> snapshot(IssuedToken<T> token) {
        return IssuedToken.of(token.getToken(), token.getIssuedAt(), token.getExpiresAt());
    }

    private static <T extends Token> IssuedToken<T> cast(IssuedToken<?> token, TokenKind<T> kind) {
        T typedToken = kind.getTokenClass().cast(token.getToken());
        return IssuedToken.of(typedToken, token.getIssuedAt(), token.getExpiresAt());
    }
}
