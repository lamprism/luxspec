package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.catalog.ConfigCatalog;
import com.lamprism.luxspec.config.catalog.ConfigCatalogRegistry;
import com.lamprism.luxspec.config.catalog.InMemoryConfigCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigCatalogTest {
    @Test
    void resolvesAParameterizedDefinitionFromACompleteKey() {
        ConfigSpec<String> spec = ConfigSpec.of(
                "some.{name}.config",
                List.of(new ConfigParameter("name", Set.of("x1", "x2"))),
                ConfigCodecs.string(),
                null,
                false
        );
        ConfigCatalogRegistry catalog = ConfigCatalogRegistry.inMemory();
        catalog.register(spec);

        ConfigCatalog lookup = catalog;
        ConfigBinding<?> binding = lookup.resolve(ConfigKey.of("some.x1.config"));

        assertNotNull(binding);
        assertEquals(spec, binding.getSpec());
        assertEquals("x1", binding.getArguments().get("name"));
        assertEquals("some.x1.config", binding.getKey().getValue());
    }

    @Test
    void fixedDefinitionWinsOverAParameterizedDefinition() {
        ConfigCatalogRegistry catalog = ConfigCatalogRegistry.inMemory();
        ConfigSpec<String> fixed = ConfigSpec.of(
                "some.x1.config",
                ConfigCodecs.string(),
                null,
                false
        );
        catalog.register(ConfigSpec.of(
                "some.{name}.config",
                List.of(new ConfigParameter("name", Set.of())),
                ConfigCodecs.string(),
                null,
                false
        ));
        catalog.register(fixed);

        assertEquals(fixed, catalog.resolve(ConfigKey.of("some.x1.config")).getSpec());
    }

    @Test
    void rejectsOverlappingParameterizedDefinitionsWithEqualSpecificity() {
        ConfigCatalogRegistry catalog = ConfigCatalogRegistry.inMemory();
        catalog.register(ConfigSpec.of(
                "some.{name}.config",
                List.of(new ConfigParameter("name", Set.of())),
                ConfigCodecs.string(),
                null,
                false
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.register(ConfigSpec.of(
                        "some.{other}.config",
                        List.of(new ConfigParameter("other", Set.of())),
                        ConfigCodecs.string(),
                        null,
                        false
                ))
        );
    }

    @Test
    void exposesDefinitionsInRegistrationOrder() {
        ConfigSpec<String> first = ConfigSpec.of(
                "first.value",
                ConfigCodecs.string(),
                null,
                false
        );
        ConfigSpec<Integer> second = ConfigSpec.of(
                "second.value",
                ConfigCodecs.integer(),
                null,
                false
        );
        ConfigCatalogRegistry catalog = new InMemoryConfigCatalog();

        catalog.registerAll(List.of(first, second));

        assertEquals(List.of(first, second), catalog.getDefinitions());
        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.getDefinitions().add(first)
        );
    }
}
