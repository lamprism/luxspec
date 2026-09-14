package com.lamprism.luxspec.config.source.process;

import com.lamprism.luxspec.config.ConfigKey;

import java.util.Objects;
import java.util.function.Function;

final class ProcessConfigName {
    private ProcessConfigName() {
    }

    static Function<ConfigKey, String> affixed(
            Function<? super ConfigKey, String> mapper,
            String prefix,
            String suffix
    ) {
        Function<? super ConfigKey, String> nonNullMapper = Objects.requireNonNull(mapper, "mapper");
        String nonNullPrefix = Objects.requireNonNull(prefix, "prefix");
        String nonNullSuffix = Objects.requireNonNull(suffix, "suffix");
        return key -> nonNullPrefix + map(nonNullMapper, key) + nonNullSuffix;
    }

    static String map(Function<? super ConfigKey, String> mapper, ConfigKey key) {
        String name = Objects.requireNonNull(
                Objects.requireNonNull(mapper, "mapper").apply(Objects.requireNonNull(key, "key")),
                "mapped configuration name"
        );
        if (name.isBlank()) {
            throw new IllegalArgumentException("Mapped configuration name must not be blank");
        }
        return name;
    }

    static String requireName(String value, String label) {
        String name = Objects.requireNonNull(value, label);
        if (name.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        if (name.startsWith("--")) {
            throw new IllegalArgumentException(label + " must not start with dashes");
        }
        return name;
    }
}
