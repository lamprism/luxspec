package com.lamprism.luxspec.security.crypto.config;

import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigParameter;
import com.lamprism.luxspec.config.ConfigSourceCapability;
import com.lamprism.luxspec.config.TemplateConfigSpec;
import java.util.List;
import java.util.Set;

/**
 * Defines standard Config specifications for named cryptographic key sets.
 *
 * @author RollW
 */
public final class ConfigKeySetSpecs {
    /**
     * Identifies the optional active signing key in one named key set.
     */
    public static final TemplateConfigSpec<String> ACTIVE_KEY_ID = TemplateConfigSpec.of(
            "security.crypto.key-sets.{key-set}.active-key-id",
            List.of(new ConfigParameter("key-set", Set.of())),
            ConfigCodecs.string(),
            null,
            false
    );
    /**
     * Lists all key IDs accepted by one named key set.
     */
    public static final TemplateConfigSpec<List<String>> KEY_IDS = TemplateConfigSpec.of(
            "security.crypto.key-sets.{key-set}.key-ids",
            List.of(new ConfigParameter("key-set", Set.of())),
            ConfigCodecs.list(ConfigCodecs.string()),
            null,
            false
    );
    /**
     * Defines the material type for one named key entry.
     */
    public static final TemplateConfigSpec<String> TYPE = TemplateConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.type",
            List.of(
                    new ConfigParameter("key-set", Set.of()),
                    new ConfigParameter("key-id", Set.of())
            ),
            ConfigCodecs.string(),
            null,
            false
    );
    /**
     * Defines the JCA algorithm for one named key entry.
     */
    public static final TemplateConfigSpec<String> ALGORITHM = TemplateConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.algorithm",
            List.of(
                    new ConfigParameter("key-set", Set.of()),
                    new ConfigParameter("key-id", Set.of())
            ),
            ConfigCodecs.string(),
            null,
            false
    );
    /**
     * Defines the Base64-encoded secret material for one named key entry.
     */
    public static final TemplateConfigSpec<String> SECRET = TemplateConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.secret",
            List.of(
                    new ConfigParameter("key-set", Set.of()),
                    new ConfigParameter("key-id", Set.of())
            ),
            ConfigCodecs.string(),
            null,
            true,
            Set.of(ConfigSourceCapability.READ, ConfigSourceCapability.SECURE)
    );
    /**
     * Defines the Base64-encoded PKCS#8 private key for one named key entry.
     */
    public static final TemplateConfigSpec<String> PRIVATE_KEY = TemplateConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.private-key",
            List.of(
                    new ConfigParameter("key-set", Set.of()),
                    new ConfigParameter("key-id", Set.of())
            ),
            ConfigCodecs.string(),
            null,
            true,
            Set.of(ConfigSourceCapability.READ, ConfigSourceCapability.SECURE)
    );
    /**
     * Defines the Base64-encoded X.509 public key for one named key entry.
     */
    public static final TemplateConfigSpec<String> PUBLIC_KEY = TemplateConfigSpec.of(
            "security.crypto.key-sets.{key-set}.keys.{key-id}.public-key",
            List.of(
                    new ConfigParameter("key-set", Set.of()),
                    new ConfigParameter("key-id", Set.of())
            ),
            ConfigCodecs.string(),
            null,
            false
    );

    private ConfigKeySetSpecs() {
    }
}
