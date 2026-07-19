package com.lamprism.luxspec.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfigCatalogTest {
    @Test
    void resolvesBoundTemplateFromACompleteKey() {
        TemplateConfigSpec<String> template = TemplateConfigSpec.of(
                "some.{name}.config",
                List.of(new ConfigParameter("name", Set.of("x1", "x2"))),
                ConfigCodecs.string(),
                null,
                false
        );
        ConfigCatalog catalog = new ConfigCatalog();
        catalog.register(template);

        ConfigSpec<?> resolved = catalog.resolve(ConfigKey.of("some.x1.config")).orElseThrow();

        BoundConfigSpec<?> bound = assertInstanceOf(BoundConfigSpec.class, resolved);
        assertEquals("x1", bound.getArguments().get("name"));
        assertEquals("some.x1.config", bound.getKey().getValue());
    }

    @Test
    void fixedDefinitionWinsOverATemplate() {
        ConfigCatalog catalog = new ConfigCatalog();
        FixedConfigSpec<String> fixed = FixedConfigSpec.of(ConfigKey.of("some.x1.config"), ConfigCodecs.string(), null, false);
        catalog.register(TemplateConfigSpec.of("some.{name}.config", List.of(new ConfigParameter("name", Set.of())), ConfigCodecs.string(), null, false));
        catalog.register(fixed);

        assertEquals(fixed, catalog.resolve(ConfigKey.of("some.x1.config")).orElseThrow());
    }

    @Test
    void rejectsOverlappingTemplatesWithEqualSpecificity() {
        ConfigCatalog catalog = new ConfigCatalog();
        catalog.register(TemplateConfigSpec.of("some.{name}.config", List.of(new ConfigParameter("name", Set.of())), ConfigCodecs.string(), null, false));

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.register(TemplateConfigSpec.of("some.{other}.config", List.of(new ConfigParameter("other", Set.of())), ConfigCodecs.string(), null, false))
        );
    }
}
