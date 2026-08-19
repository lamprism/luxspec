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

package com.lamprism.luxspec.security.crypto.config;

import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigParameter;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.ConfigValueValidator;
import com.lamprism.luxspec.config.value.ConfigValidators;
import com.lamprism.luxspec.message.LocalizedText;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Defines standard Config specifications for named cryptographic key sets.
 *
 * @author RollW
 */
public final class ConfigKeySetSpecs {
    private static final List<ConfigParameter> KEY_SET_PARAMETERS = List.of(
            new ConfigParameter("key-set", Set.of())
    );
    private static final List<ConfigParameter> KEY_PARAMETERS = List.of(
            new ConfigParameter("key-set", Set.of()),
            new ConfigParameter("key-id", Set.of())
    );
    private static final ConfigValueValidator<String> NON_BLANK_TEXT = ConfigValidators.nonBlankText();
    private static final ConfigValueValidator<List<String>> KEY_IDS_VALIDATOR = ConfigValueValidator.<List<String>>of(
            values -> !values.isEmpty(),
            "Key IDs must contain at least one value"
    ).and(ConfigValidators.<String>elements(NON_BLANK_TEXT));

    /**
     * Identifies the optional active signing key in one named key set.
     */
    public static final ConfigSpec<String> ACTIVE_KEY_ID = ConfigSpec.builder(
                    "security.crypto.key-sets.{key-set}.active-key-id",
                    ConfigCodecs.string()
            )
            .parameters(KEY_SET_PARAMETERS)
            .localizedDescription(localized(
                    "Optional active signing key ID for the named key set.",
                    "命名密钥集的可选活动签名密钥 ID。"
            ))
            .validator(NON_BLANK_TEXT)
            .build();
    /**
     * Lists all key IDs accepted by one named key set.
     */
    public static final ConfigSpec<List<String>> KEY_IDS = ConfigSpec.builder(
                    "security.crypto.key-sets.{key-set}.key-ids",
                    ConfigCodecs.list(ConfigCodecs.string())
            )
            .parameters(KEY_SET_PARAMETERS)
            .localizedDescription(localized(
                    "Key IDs accepted by the named key set.",
                    "命名密钥集接受的密钥 ID。"
            ))
            .validator(KEY_IDS_VALIDATOR)
            .build();
    /**
     * Defines the material type for one named key entry.
     */
    public static final ConfigSpec<ConfigKeyMaterialType> TYPE = ConfigSpec.builder(
                    "security.crypto.key-sets.{key-set}.keys.{key-id}.type",
                    ConfigCodecs.enumValue(ConfigKeyMaterialType.values())
            )
            .parameters(KEY_PARAMETERS)
            .localizedDescription(localized(
                    "Cryptographic material type for the named key entry.",
                    "命名密钥条目的加密材料类型。"
            ))
            .build();
    /**
     * Defines the JCA algorithm for one named key entry.
     */
    public static final ConfigSpec<String> ALGORITHM = ConfigSpec.builder(
                    "security.crypto.key-sets.{key-set}.keys.{key-id}.algorithm",
                    ConfigCodecs.string()
            )
            .parameters(KEY_PARAMETERS)
            .localizedDescription(localized(
                    "JCA algorithm used by the named key entry.",
                    "命名密钥条目使用的 JCA 算法。"
            ))
            .validator(NON_BLANK_TEXT)
            .build();
    /**
     * Defines the Base64-encoded secret material for one named key entry.
     */
    public static final ConfigSpec<String> SECRET = ConfigSpec.builder(
                    "security.crypto.key-sets.{key-set}.keys.{key-id}.secret",
                    ConfigCodecs.string()
            )
            .parameters(KEY_PARAMETERS)
            .localizedDescription(localized(
                    "Base64-encoded secret material for the named key entry.",
                    "命名密钥条目的 Base64 编码密钥材料。"
            ))
            .sensitive()
            .validator(NON_BLANK_TEXT)
            .build();
    /**
     * Defines the Base64-encoded PKCS#8 private key for one named key entry.
     */
    public static final ConfigSpec<String> PRIVATE_KEY = ConfigSpec.builder(
                    "security.crypto.key-sets.{key-set}.keys.{key-id}.private-key",
                    ConfigCodecs.string()
            )
            .parameters(KEY_PARAMETERS)
            .localizedDescription(localized(
                    "Base64-encoded PKCS#8 private key for the named key entry.",
                    "命名密钥条目的 Base64 编码 PKCS#8 私钥。"
            ))
            .sensitive()
            .validator(NON_BLANK_TEXT)
            .build();
    /**
     * Defines the optional Base64-encoded X.509 public key for one named key entry.
     *
     * <p>A {@code key-pair} entry derives this value from its private key when it is absent. A
     * {@code public-key} entry still requires this value.</p>
     */
    public static final ConfigSpec<String> PUBLIC_KEY = ConfigSpec.builder(
                    "security.crypto.key-sets.{key-set}.keys.{key-id}.public-key",
                    ConfigCodecs.string()
            )
            .parameters(KEY_PARAMETERS)
            .localizedDescription(localized(
                    "Optional Base64-encoded X.509 public key for the named key entry.",
                    "命名密钥条目的可选 Base64 编码 X.509 公钥。"
            ))
            .validator(NON_BLANK_TEXT)
            .build();

    private ConfigKeySetSpecs() {
    }

    /**
     * Returns every configuration definition owned by named cryptographic key sets.
     *
     * @return the immutable definitions in declaration order
     */
    public static List<ConfigSpec<?>> all() {
        return List.of(
                ACTIVE_KEY_ID,
                KEY_IDS,
                TYPE,
                ALGORITHM,
                SECRET,
                PRIVATE_KEY,
                PUBLIC_KEY
        );
    }

    private static LocalizedText localized(String defaultText, String simplifiedChineseText) {
        return LocalizedText.of(defaultText, Locale.SIMPLIFIED_CHINESE, simplifiedChineseText);
    }
}
