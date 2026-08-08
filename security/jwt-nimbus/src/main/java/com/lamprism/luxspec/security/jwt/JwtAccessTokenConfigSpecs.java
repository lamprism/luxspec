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

import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValueValidator;

import java.time.Duration;
import java.util.List;

/**
 * Defines standard Config specifications for JWT access-token settings.
 *
 * @author RollW
 */
public final class JwtAccessTokenConfigSpecs {
    private static final ConfigValueValidator<Duration> POSITIVE_DURATION = ConfigValueValidator.of(
            value -> !value.isZero() && !value.isNegative(),
            "Duration must be positive"
    );
    private static final ConfigValueValidator<Duration> NON_NEGATIVE_DURATION = ConfigValueValidator.of(
            value -> !value.isNegative(),
            "Duration must not be negative"
    );
    private static final ConfigValueValidator<String> NON_BLANK_TEXT = ConfigValueValidator.of(
            value -> !value.isBlank(),
            "Text value must not be blank"
    );
    private static final ConfigValueValidator<List<String>> NON_BLANK_AUDIENCES = ConfigValueValidator.of(
            values -> values.stream().noneMatch(String::isBlank),
            "Audience values must not be blank"
    );

    /**
     * Controls the lifetime of newly issued access tokens.
     */
    public static final ConfigSpec<Duration> ACCESS_TTL = ConfigSpec.of(
            "security.token.access.ttl",
            ConfigCodecs.duration(),
            Duration.ofMinutes(15),
            false,
            POSITIVE_DURATION
    );
    /**
     * Identifies the trusted issuer of newly issued and parsed access tokens.
     */
    public static final ConfigSpec<String> ACCESS_ISSUER = ConfigSpec.of(
            "security.token.access.issuer",
            ConfigCodecs.string(),
            null,
            false,
            NON_BLANK_TEXT
    );
    /**
     * Defines accepted audience values for access tokens.
     */
    public static final ConfigSpec<List<String>> ACCESS_AUDIENCES = ConfigSpec.of(
            "security.token.access.audiences",
            ConfigCodecs.list(ConfigCodecs.string()),
            List.of(),
            false,
            NON_BLANK_AUDIENCES
    );
    /**
     * Defines whole-second clock tolerance when verifying access tokens.
     */
    public static final ConfigSpec<Duration> ACCESS_CLOCK_SKEW = ConfigSpec.of(
            "security.token.access.clock-skew",
            ConfigCodecs.duration(),
            Duration.ZERO,
            false,
            NON_NEGATIVE_DURATION
    );
    /**
     * References the provider-owned key set used to sign and verify access tokens.
     */
    public static final ConfigSpec<String> KEY_SET_NAME = ConfigSpec.of(
            "security.token.access.key-set",
            ConfigCodecs.string(),
            null,
            false,
            NON_BLANK_TEXT
    );

    private JwtAccessTokenConfigSpecs() {
    }
}
