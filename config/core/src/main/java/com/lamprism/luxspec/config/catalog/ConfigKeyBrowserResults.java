package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class ConfigKeyBrowserResults {

    private ConfigKeyBrowserResults() {
    }

    static List<ConfigKey> validate(ConfigSpec<?> spec, Iterable<ConfigKey> keys) {
        ConfigSpec<?> nonNullSpec = Objects.requireNonNull(spec, "spec");
        Set<ConfigKey> result = new LinkedHashSet<>();
        for (ConfigKey key : Objects.requireNonNull(keys, "browser result")) {
            ConfigKey nonNullKey = Objects.requireNonNull(key, "browser key");
            if (!nonNullSpec.matches(nonNullKey)) {
                throw new IllegalArgumentException(
                        "Browser returned a key that does not match the configuration definition"
                );
            }
            result.add(nonNullKey);
        }
        return List.copyOf(new ArrayList<>(result));
    }
}
