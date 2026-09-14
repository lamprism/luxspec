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

package com.lamprism.luxspec.user.spring;

import com.lamprism.luxspec.user.security.password.EncodedPassword;
import com.lamprism.luxspec.user.security.password.PasswordScheme;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

/**
 * Adapts the provider-independent Luxspec password scheme to Spring Security.
 *
 * @author RollW
 */
public class LuxspecPasswordEncoder implements PasswordEncoder {
    private final PasswordScheme passwordScheme;

    /**
     * Creates a Spring password encoder backed by one Luxspec password scheme.
     *
     * @param passwordScheme the authoritative Luxspec password scheme
     */
    public LuxspecPasswordEncoder(PasswordScheme passwordScheme) {
        this.passwordScheme = Objects.requireNonNull(passwordScheme, "passwordScheme");
    }

    /**
     * Encodes one raw password through the configured Luxspec scheme.
     *
     * @param rawPassword the raw password
     * @return the opaque encoded password value
     */
    @Override
    public String encode(@Nullable CharSequence rawPassword) {
        return passwordScheme.encode(rawPassword).getValue();
    }

    /**
     * Verifies one raw password through the configured Luxspec scheme.
     *
     * @param rawPassword     the raw password
     * @param encodedPassword the stored encoded password value
     * @return whether the password matches
     */
    @Override
    public boolean matches(@Nullable CharSequence rawPassword, @Nullable String encodedPassword) {
        return passwordScheme.verify(rawPassword, new EncodedPassword(encodedPassword));
    }

    /**
     * Reports whether one stored password should be re-encoded.
     *
     * @param encodedPassword the stored encoded password value
     * @return whether the configured scheme considers it outdated
     */
    @Override
    public boolean upgradeEncoding(@Nullable String encodedPassword) {
        return passwordScheme.needsUpgrade(new EncodedPassword(encodedPassword));
    }
}
