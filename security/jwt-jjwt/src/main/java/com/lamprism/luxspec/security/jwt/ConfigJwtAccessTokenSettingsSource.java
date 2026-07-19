package com.lamprism.luxspec.security.jwt;

import com.lamprism.luxspec.config.ConfigReadOption;
import com.lamprism.luxspec.config.ConfigReader;
import com.lamprism.luxspec.config.ConfigSpec;
import java.util.Objects;
import java.util.Set;

/**
 * Reads trusted JWT access-token settings from typed Luxspec configuration.
 *
 * @author RollW
 */
public final class ConfigJwtAccessTokenSettingsSource {
    private final ConfigReader reader;

    /**
     * Creates a source backed by typed Luxspec configuration.
     *
     * @param reader the typed configuration reader
     */
    public ConfigJwtAccessTokenSettingsSource(ConfigReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    /**
     * Returns the current trusted JWT access-token options.
     *
     * @return the JWT access-token options
     */
    public JwtAccessTokenOptions getOptions() {
        return new JwtAccessTokenOptions(
                read(JwtAccessTokenConfigSpecs.ACCESS_TTL),
                read(JwtAccessTokenConfigSpecs.ACCESS_ISSUER),
                Set.copyOf(read(JwtAccessTokenConfigSpecs.ACCESS_AUDIENCES)),
                read(JwtAccessTokenConfigSpecs.ACCESS_CLOCK_SKEW)
        );
    }

    /**
     * Returns the provider-owned key-set name referenced by JWT configuration.
     *
     * @return the configured key-set name
     */
    public String getKeySetName() {
        return requireText(read(JwtAccessTokenConfigSpecs.KEY_SET_NAME), "keySetName");
    }

    private <T> T read(ConfigSpec<T> spec) {
        return reader.get(spec, ConfigReadOption.FRESH).getValue().orElseThrow(
                () -> new IllegalStateException("Required JWT configuration is not available: " + spec.getKey().getValue())
        );
    }

    private String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
