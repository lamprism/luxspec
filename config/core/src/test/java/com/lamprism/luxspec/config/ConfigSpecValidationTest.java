package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import com.lamprism.luxspec.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigSpecValidationTest {
    @Test
    void rejectsAnInvalidDefaultWhenTheDefinitionIsCreated() {
        Validator<Integer> positive = Validator.of(
                value -> value > 0,
                "Value must be positive"
        );

        assertThrows(
                ConfigValueValidationException.class,
                () -> ConfigSpec.of("sample.limit", ConfigCodecs.integer(), 0, false, positive)
        );
    }
}
