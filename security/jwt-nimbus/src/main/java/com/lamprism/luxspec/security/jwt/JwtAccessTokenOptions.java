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

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Defines trusted JWT access-token format and validation options.
 *
 * @author RollW
 */
public final class JwtAccessTokenOptions {
    private final Duration lifetime;
    private final String issuer;
    private final Set<String> audiences;
    private final Duration clockSkew;

    /**
     * Creates immutable JWT access-token options.
     *
     * @param lifetime  the positive token lifetime
     * @param issuer    the non-blank expected issuer
     * @param audiences accepted audiences, or an empty set to omit audience validation
     * @param clockSkew the non-negative whole-second verification tolerance
     */
    public JwtAccessTokenOptions(
            Duration lifetime,
            String issuer,
            Set<String> audiences,
            Duration clockSkew
    ) {
        this.lifetime = requirePositive(lifetime, "lifetime");
        this.issuer = requireText(issuer, "issuer");
        this.audiences = copyAudiences(audiences);
        this.clockSkew = requireWholeSeconds(clockSkew);
    }

    /**
     * Returns the positive access-token lifetime.
     *
     * @return the token lifetime
     */
    public Duration getLifetime() {
        return lifetime;
    }

    /**
     * Returns the required JWT issuer.
     *
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns immutable accepted audiences.
     *
     * @return the accepted audiences
     */
    public Set<String> getAudiences() {
        return audiences;
    }

    /**
     * Returns the whole-second verification tolerance.
     *
     * @return the clock-skew duration
     */
    public Duration getClockSkew() {
        return clockSkew;
    }

    private static Duration requirePositive(Duration value, String name) {
        Duration duration = Objects.requireNonNull(value, name);
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name);
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }

    private static Set<String> copyAudiences(Set<String> values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : Objects.requireNonNull(values, "audiences")) {
            result.add(requireText(value, "audience"));
        }
        return Set.copyOf(result);
    }

    private static Duration requireWholeSeconds(Duration value) {
        Duration duration = Objects.requireNonNull(value, "clockSkew");
        if (duration.isNegative() || duration.toNanosPart() != 0) {
            throw new IllegalArgumentException("clockSkew must be a non-negative whole-second duration");
        }
        return duration;
    }
}
