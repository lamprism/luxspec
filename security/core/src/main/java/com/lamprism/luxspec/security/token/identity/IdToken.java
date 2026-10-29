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

package com.lamprism.luxspec.security.token.identity;

import com.lamprism.luxspec.security.token.Token;
import com.lamprism.luxspec.security.token.TokenKind;

import java.util.Objects;

/**
 * Holds an identity-token value without prescribing its protocol or format.
 *
 * <p>Issuers and verifiers own their identity claims, audience, and protocol validation.</p>
 *
 * @author RollW
 */
public final class IdToken implements Token {
    /**
     * The standard identity-token kind.
     */
    public static final TokenKind<IdToken> KIND = TokenKind.of("id", IdToken.class);

    private final String value;

    /**
     * Creates an identity-token value.
     *
     * @param value the non-blank sensitive token value
     */
    public IdToken(String value) {
        this.value = Objects.requireNonNull(value, "value");
        if (this.value.isBlank()) {
            throw new IllegalArgumentException("ID Token value must not be blank");
        }
    }

    @Override
    public TokenKind<IdToken> getKind() {
        return KIND;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "IdToken[REDACTED]";
    }
}
