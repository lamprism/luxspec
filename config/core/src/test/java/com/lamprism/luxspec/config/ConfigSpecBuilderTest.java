package com.lamprism.luxspec.config;

import com.lamprism.luxspec.message.LocalizedText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSpecBuilderTest {
    @Test
    void buildsParameterizedDefinitionsThroughOneFluentPath() {
        ConfigSpec<Integer> spec = ConfigSpec.builder("tenant.{name}.limit", ConfigCodecs.integer())
                .parameter(new ConfigParameter("name", Set.of("acme")))
                .textDescription("Maximum tenant request limit")
                .defaultValue(10)
                .validator(ConfigValueValidator.of(value -> value > 0, "Value must be positive"))
                .build();

        ConfigBinding<Integer> binding = spec.bind(java.util.Map.of("name", "acme"));

        assertEquals("tenant.acme.limit", binding.getKey().getValue());
        assertEquals("Maximum tenant request limit", spec.getDescription().resolve(Locale.US));
        assertEquals(10, spec.getDefaultValue());
        spec.validate(1);
    }

    @Test
    void preservesExplicitParameterListAndSensitivity() {
        ConfigSpec<String> spec = ConfigSpec.builder("service.{region}.token", ConfigCodecs.string())
                .parameters(List.of(new ConfigParameter("region", Set.of("global"))))
                .sensitive()
                .build();

        assertTrue(spec.isSensitive());
        assertEquals("global", spec.bind(java.util.Map.of("region", "global"))
                .getArguments()
                .get("region"));
    }

    @Test
    void preservesLocallyTranslatedDescriptionMetadata() {
        ConfigSpec<String> spec = ConfigSpec.builder("service.name", ConfigCodecs.string())
                .localizedDescription(LocalizedText.of("Service name", Locale.SIMPLIFIED_CHINESE, "服务名称"))
                .build();

        assertEquals("Service name", spec.getDescription().resolve(Locale.US));
        assertEquals("服务名称", spec.getDescription().resolve(Locale.SIMPLIFIED_CHINESE));
    }

    @Test
    void resolvesTextDescriptionsWithoutAMessageResolver() {
        assertEquals(
                "Inline text",
                ConfigDescription.text("Inline text").resolve(Locale.US)
        );
    }
}
