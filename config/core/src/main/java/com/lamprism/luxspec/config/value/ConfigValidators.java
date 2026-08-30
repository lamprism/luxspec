package com.lamprism.luxspec.config.value;

import com.lamprism.luxspec.config.ConfigValue;
import com.lamprism.luxspec.validation.Validator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Provides focused validators for common configuration value constraints.
 *
 * <p>These validators describe domain rules. They do not replace the non-null API precondition
 * enforced by the typed codec and {@link ConfigValue} contract.</p>
 *
 * @author RollW
 */
public final class ConfigValidators {
    private ConfigValidators() {
    }

    /**
     * Creates a validator that rejects blank text.
     *
     * @return the non-blank text validator
     */
    public static Validator<String> nonBlankText() {
        return validator(
                value -> !value.isBlank(),
                "Text value must not be blank"
        );
    }

    /**
     * Creates an inclusive range validator for comparable values.
     *
     * @param minimum the inclusive lower bound
     * @param maximum the inclusive upper bound
     * @param <T>     the comparable value type
     * @return the range validator
     */
    public static <T extends Comparable<? super T>> Validator<T> betweenInclusive(
            T minimum,
            T maximum
    ) {
        T nonNullMinimum = Objects.requireNonNull(minimum, "minimum");
        T nonNullMaximum = Objects.requireNonNull(maximum, "maximum");
        if (nonNullMinimum.compareTo(nonNullMaximum) > 0) {
            throw new IllegalArgumentException("minimum must not be greater than maximum");
        }
        return validator(
                value -> value.compareTo(nonNullMinimum) >= 0
                        && value.compareTo(nonNullMaximum) <= 0,
                "Value is outside the configured range"
        );
    }

    /**
     * Creates a validator that accepts strictly positive numbers.
     *
     * @param <T> the numeric value type
     * @return the positive-number validator
     */
    public static <T extends Number> Validator<T> positiveNumber() {
        return validator(
                value -> compareWithZero(value) > 0,
                "Numeric value must be positive"
        );
    }

    /**
     * Creates a validator that accepts zero and positive numbers.
     *
     * @param <T> the numeric value type
     * @return the non-negative-number validator
     */
    public static <T extends Number> Validator<T> nonNegativeNumber() {
        return validator(
                value -> compareWithZero(value) >= 0,
                "Numeric value must not be negative"
        );
    }

    /**
     * Creates a validator that rejects non-finite floating-point values.
     *
     * @param <T> the numeric value type
     * @return the finite-number validator
     */
    public static <T extends Number> Validator<T> finiteNumber() {
        return validator(
                ConfigValidators::isFinite,
                "Numeric value must be finite"
        );
    }

    /**
     * Creates a validator that accepts only members of a fixed set.
     *
     * @param allowedValues the non-empty allowed value set
     * @param <T>           the value type
     * @return the membership validator
     */
    public static <T> Validator<T> oneOf(Set<? extends T> allowedValues) {
        Set<? extends T> values = Set.copyOf(Objects.requireNonNull(allowedValues, "allowedValues"));
        if (values.isEmpty()) {
            throw new IllegalArgumentException("allowedValues must not be empty");
        }
        return validator(
                values::contains,
                "Value is not an allowed member"
        );
    }

    /**
     * Creates a validator that applies one validator to every list element.
     *
     * @param elementValidator the element validator
     * @param <T>              the element type
     * @return the list element validator
     */
    public static <T> Validator<List<T>> elements(
            Validator<? super T> elementValidator
    ) {
        Validator<? super T> nonNullValidator = Objects.requireNonNull(
                elementValidator,
                "elementValidator"
        );
        return values -> {
            List<T> nonNullValues = Objects.requireNonNull(values, "value");
            for (T value : nonNullValues) {
                nonNullValidator.validate(value);
            }
        };
    }

    private static <T> Validator<T> validator(Predicate<? super T> predicate, String detail) {
        Predicate<? super T> nonNullPredicate = Objects.requireNonNull(predicate, "predicate");
        String nonBlankDetail = Objects.requireNonNull(detail, "detail");
        if (nonBlankDetail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        return value -> {
            T nonNullValue = Objects.requireNonNull(value, "value");
            if (!nonNullPredicate.test(nonNullValue)) {
                throw new ConfigValueValidationException(nonBlankDetail);
            }
        };
    }

    private static int compareWithZero(Number value) {
        Number nonNullValue = Objects.requireNonNull(value, "value");
        if (nonNullValue instanceof BigDecimal decimal) {
            return decimal.compareTo(BigDecimal.ZERO);
        }
        if (nonNullValue instanceof BigInteger integer) {
            return integer.signum();
        }
        if (nonNullValue instanceof Byte
                || nonNullValue instanceof Short
                || nonNullValue instanceof Integer
                || nonNullValue instanceof Long) {
            return Long.compare(nonNullValue.longValue(), 0L);
        }
        if (nonNullValue instanceof Float || nonNullValue instanceof Double) {
            double numericValue = nonNullValue.doubleValue();
            if (Double.isNaN(numericValue)) {
                return -2;
            }
            return numericValue < 0 ? -1 : numericValue > 0 ? 1 : 0;
        }
        throw new IllegalArgumentException("Unsupported numeric value type");
    }

    private static boolean isFinite(Number value) {
        Number nonNullValue = Objects.requireNonNull(value, "value");
        if (nonNullValue instanceof Float floatValue) {
            return Float.isFinite(floatValue);
        }
        if (nonNullValue instanceof Double doubleValue) {
            return Double.isFinite(doubleValue);
        }
        if (nonNullValue instanceof BigDecimal || nonNullValue instanceof BigInteger
                || nonNullValue instanceof Byte || nonNullValue instanceof Short
                || nonNullValue instanceof Integer || nonNullValue instanceof Long) {
            return true;
        }
        throw new IllegalArgumentException("Unsupported numeric value type");
    }
}
