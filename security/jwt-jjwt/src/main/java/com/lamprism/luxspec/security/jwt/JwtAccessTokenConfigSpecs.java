package com.lamprism.luxspec.security.jwt;

import com.lamprism.luxspec.config.ConfigCodecs;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;
import com.lamprism.luxspec.config.FixedConfigSpec;
import java.time.Duration;
import java.util.List;

/**
 * Defines standard Config specifications for JJWT access-token settings.
 *
 * @author RollW
 */
public final class JwtAccessTokenConfigSpecs {
    /**
     * Controls the lifetime of newly issued access tokens.
     */
    public static final ConfigSpec<Duration> ACCESS_TTL = FixedConfigSpec.of(
            ConfigKey.of("security.token.access.ttl"),
            ConfigCodecs.duration(),
            Duration.ofMinutes(15),
            false
    );
    /**
     * Identifies the trusted issuer of newly issued and parsed access tokens.
     */
    public static final ConfigSpec<String> ACCESS_ISSUER = FixedConfigSpec.of(
            ConfigKey.of("security.token.access.issuer"),
            ConfigCodecs.string(),
            null,
            false
    );
    /**
     * Defines accepted audience values for access tokens.
     */
    public static final ConfigSpec<List<String>> ACCESS_AUDIENCES = FixedConfigSpec.of(
            ConfigKey.of("security.token.access.audiences"),
            ConfigCodecs.list(ConfigCodecs.string()),
            List.of(),
            false
    );
    /**
     * Defines whole-second clock tolerance when verifying access tokens.
     */
    public static final ConfigSpec<Duration> ACCESS_CLOCK_SKEW = FixedConfigSpec.of(
            ConfigKey.of("security.token.access.clock-skew"),
            ConfigCodecs.duration(),
            Duration.ZERO,
            false
    );
    /**
     * References the provider-owned key set used to sign and verify access tokens.
     */
    public static final ConfigSpec<String> KEY_SET_NAME = FixedConfigSpec.of(
            ConfigKey.of("security.token.access.key-set"),
            ConfigCodecs.string(),
            null,
            false
    );

    private JwtAccessTokenConfigSpecs() {
    }
}
