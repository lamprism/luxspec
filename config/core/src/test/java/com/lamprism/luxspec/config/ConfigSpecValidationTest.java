package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigSpecValidationTest {
    @Test
    void rejectsAnInvalidDefaultWhenTheDefinitionIsCreated() {
        ConfigValueValidator<Integer> positive = ConfigValueValidator.of(
                value -> value > 0,
                "Value must be positive"
        );

        assertThrows(
                ConfigValueValidationException.class,
                () -> ConfigSpec.of("sample.limit", ConfigCodecs.integer(), 0, false, positive)
        );
    }
}
