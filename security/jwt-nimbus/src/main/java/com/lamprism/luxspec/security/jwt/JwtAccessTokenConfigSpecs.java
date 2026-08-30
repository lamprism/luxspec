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
import com.lamprism.luxspec.message.LocalizedText;
import com.lamprism.luxspec.validation.Validator;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Defines standard Config specifications for JWT access-token settings.
 *
 * @author RollW
 */
public final class JwtAccessTokenConfigSpecs {
    private static final Validator<Duration> POSITIVE_DURATION = Validator.of(
            value -> !value.isZero() && !value.isNegative(),
            "Duration must be positive"
    );
    private static final Validator<Duration> NON_NEGATIVE_DURATION = Validator.of(
            value -> !value.isNegative(),
            "Duration must not be negative"
    );
    private static final Validator<String> NON_BLANK_TEXT = Validator.of(
            value -> !value.isBlank(),
            "Text value must not be blank"
    );
    private static final Validator<List<String>> NON_BLANK_AUDIENCES = Validator.of(
            values -> values.stream().noneMatch(String::isBlank),
            "Audience values must not be blank"
    );

    /**
     * Controls the lifetime of newly issued access tokens.
     */
    public static final ConfigSpec<Duration> ACCESS_TTL = ConfigSpec.builder(
                    "security.token.access.ttl",
                    ConfigCodecs.duration()
            )
            .localizedDescription(localized(
                    "Lifetime of newly issued access tokens. Defaults to 15 minutes.",
                    "新签发访问令牌的有效期。默认值为 15 分钟。"
            ))
            .defaultValue(Duration.ofMinutes(15))
            .validator(POSITIVE_DURATION)
            .build();
    /**
     * Identifies the trusted issuer of newly issued and parsed access tokens.
     */
    public static final ConfigSpec<String> ACCESS_ISSUER = ConfigSpec.builder(
                    "security.token.access.issuer",
                    ConfigCodecs.string()
            )
            .localizedDescription(localized(
                    "Trusted issuer required on newly issued and parsed access tokens.",
                    "新签发和已解析访问令牌必须信任的签发者。"
            ))
            .validator(NON_BLANK_TEXT)
            .build();
    /**
     * Defines accepted audience values for access tokens.
     */
    public static final ConfigSpec<List<String>> ACCESS_AUDIENCES = ConfigSpec.builder(
                    "security.token.access.audiences",
                    ConfigCodecs.list(ConfigCodecs.string())
            )
            .localizedDescription(localized(
                    "Audience values accepted by access token consumers. An empty list disables audience validation. Applications sharing one issuer across multiple resource servers should configure explicit audiences.",
                    "访问令牌使用者接受的受众值。空列表会停用受众校验。多个资源服务器共享同一签发方时应配置明确的受众值。"
            ))
            .defaultValue(List.of())
            .validator(NON_BLANK_AUDIENCES)
            .build();
    /**
     * Defines whole-second clock tolerance when verifying access tokens.
     */
    public static final ConfigSpec<Duration> ACCESS_CLOCK_SKEW = ConfigSpec.builder(
                    "security.token.access.clock-skew",
                    ConfigCodecs.duration()
            )
            .localizedDescription(localized(
                    "Clock tolerance used when verifying access tokens. Defaults to zero.",
                    "验证访问令牌时允许的时钟偏差。默认值为零。"
            ))
            .defaultValue(Duration.ZERO)
            .validator(NON_NEGATIVE_DURATION)
            .build();
    /**
     * References the provider-owned key set used to sign and verify access tokens.
     */
    public static final ConfigSpec<String> KEY_SET_NAME = ConfigSpec.builder(
                    "security.token.access.key-set",
                    ConfigCodecs.string()
            )
            .localizedDescription(localized(
                    "Name of the configured key set used to sign and verify access tokens.",
                    "用于签发和验证访问令牌的已配置密钥集名称。"
            ))
            .validator(NON_BLANK_TEXT)
            .build();

    private JwtAccessTokenConfigSpecs() {
    }

    /**
     * Returns every configuration definition owned by JWT access-token settings.
     *
     * @return the immutable definitions in declaration order
     */
    public static List<ConfigSpec<?>> all() {
        return List.of(
                ACCESS_TTL,
                ACCESS_ISSUER,
                ACCESS_AUDIENCES,
                ACCESS_CLOCK_SKEW,
                KEY_SET_NAME
        );
    }

    private static LocalizedText localized(String defaultText, String simplifiedChineseText) {
        return LocalizedText.of(defaultText, Locale.SIMPLIFIED_CHINESE, simplifiedChineseText);
    }
}
