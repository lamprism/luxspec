package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.value.ConfigValueValidationException;
import com.lamprism.luxspec.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigSpecTest {
    @Test
    void validatesNonBlankText() {
        Validator<String> validator = ConfigSpec.nonBlankText();

        assertDoesNotThrow(() -> validator.validate("value"));
        assertThrows(ConfigValueValidationException.class, () -> validator.validate("  "));
    }

    @Test
    void validatesComparableRanges() {
        Validator<Integer> validator = ConfigSpec.betweenInclusive(1, 10);

        assertDoesNotThrow(() -> validator.validate(1));
        assertDoesNotThrow(() -> validator.validate(10));
        assertThrows(ConfigValueValidationException.class, () -> validator.validate(11));
    }

    @Test
    void validatesNumericBoundariesAndFiniteValues() {
        Validator<BigDecimal> positive = ConfigSpec.positiveNumber();
        Validator<Double> finite = ConfigSpec.finiteNumber();

        assertDoesNotThrow(() -> positive.validate(new BigDecimal("0.001")));
        assertThrows(ConfigValueValidationException.class, () -> positive.validate(BigDecimal.ZERO));
        assertDoesNotThrow(() -> finite.validate(1.0));
        assertThrows(ConfigValueValidationException.class, () -> finite.validate(Double.POSITIVE_INFINITY));
    }

    @Test
    void validatesMembershipAndListElements() {
        Validator<String> membership = ConfigSpec.oneOf(Set.of("a", "b"));
        Validator<List<String>> elements = ConfigSpec.elements(
                ConfigSpec.nonBlankText()
        );

        assertDoesNotThrow(() -> membership.validate("a"));
        assertThrows(ConfigValueValidationException.class, () -> membership.validate("c"));
        assertDoesNotThrow(() -> elements.validate(List.of("a", "b")));
        assertThrows(ConfigValueValidationException.class, () -> elements.validate(List.of("a", " ")));
    }
}
