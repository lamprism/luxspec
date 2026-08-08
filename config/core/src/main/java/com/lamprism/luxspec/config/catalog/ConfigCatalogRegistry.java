package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigSpec;

import java.util.Objects;

/**
 * Registers configuration definitions and exposes their read-only lookup capability.
 *
 * @author RollW
 */
public interface ConfigCatalogRegistry extends ConfigCatalog {
    /**
     * Creates an in-memory registry for application assembly.
     *
     * @return the in-memory registry
     */
    static ConfigCatalogRegistry inMemory() {
        return new InMemoryConfigCatalog();
    }

    /**
     * Registers one fixed or parameterized definition.
     *
     * @param spec the definition to register
     */
    void register(ConfigSpec<?> spec);

    /**
     * Registers multiple definitions in iteration order.
     *
     * @param specs the definitions to register
     */
    default void registerAll(Iterable<? extends ConfigSpec<?>> specs) {
        for (ConfigSpec<?> spec : Objects.requireNonNull(specs, "specs")) {
            register(Objects.requireNonNull(spec, "spec"));
        }
    }
}
