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

import java.util.List;
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
    public static final ConfigSpec<String> ACTIVE_KEY_ID = ConfigSpec.of(
            "security.crypto.key-sets.{key-set}.active-key-id",
            KEY_SET_PARAMETERS,
            ConfigCodecs.string(),
            null,
            false,
            NON_BLANK_TEXT
    );
    /**
     * Lists all key IDs accepted by one named key set.
     */
    public static final ConfigSpec<List<String>> KEY_IDS = ConfigSpec.of(
            "security.crypto.key-sets.{key-set}.key-ids",
            KEY_SET_PARAMETERS,
            ConfigCodecs.list(ConfigCodecs.string()),
            null,
            false,
            KEY_IDS_VALIDATOR
    );
    /**
     * Defines the material type for one named key entry.
     */
    public static final ConfigSpec<ConfigKeyMaterialType> TYPE = ConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.type",
            KEY_PARAMETERS,
            ConfigCodecs.enumValue(ConfigKeyMaterialType.values()),
            null,
            false
    );
    /**
     * Defines the JCA algorithm for one named key entry.
     */
    public static final ConfigSpec<String> ALGORITHM = ConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.algorithm",
            KEY_PARAMETERS,
            ConfigCodecs.string(),
            null,
            false,
            NON_BLANK_TEXT
    );
    /**
     * Defines the Base64-encoded secret material for one named key entry.
     */
    public static final ConfigSpec<String> SECRET = ConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.secret",
            KEY_PARAMETERS,
            ConfigCodecs.string(),
            null,
            true,
            NON_BLANK_TEXT
    );
    /**
     * Defines the Base64-encoded PKCS#8 private key for one named key entry.
     */
    public static final ConfigSpec<String> PRIVATE_KEY = ConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.private-key",
            KEY_PARAMETERS,
            ConfigCodecs.string(),
            null,
            true,
            NON_BLANK_TEXT
    );
    /**
     * Defines the optional Base64-encoded X.509 public key for one named key entry.
     *
     * <p>A {@code key-pair} entry derives this value from its private key when it is absent. A
     * {@code public-key} entry still requires this value.</p>
     */
    public static final ConfigSpec<String> PUBLIC_KEY = ConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.public-key",
            KEY_PARAMETERS,
            ConfigCodecs.string(),
            null,
            false,
            NON_BLANK_TEXT
    );

    private ConfigKeySetSpecs() {
    }
}
