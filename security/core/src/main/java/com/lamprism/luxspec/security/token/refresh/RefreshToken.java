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

package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.token.Token;
import com.lamprism.luxspec.security.token.TokenKind;

import java.util.Objects;

/**
 * Holds one opaque client-presented Refresh Token value.
 *
 * @author RollW
 */
public final class RefreshToken implements Token {
    /**
     * The standard Refresh Token kind.
     */
    public static final TokenKind<RefreshToken> KIND = TokenKind.of("refresh", RefreshToken.class);

    private final String value;

    /**
     * Creates a Refresh Token from an opaque sensitive value.
     *
     * @param value the non-blank token value
     */
    public RefreshToken(String value) {
        this.value = Objects.requireNonNull(value, "value");
        if (this.value.isBlank()) {
            throw new IllegalArgumentException("Refresh Token value must not be blank");
        }
    }

    @Override
    public TokenKind<RefreshToken> getKind() {
        return KIND;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "RefreshToken[REDACTED]";
    }
}
