package com.lamprism.luxspec.config.source;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents provider-neutral scalar or list data before codec conversion.
 *
 * <p>This sealed algebra is the normalized boundary between a Source and a Codec. Source adapters
 * use the factories to translate provider-native values and do not implement this contract.</p>
 *
 * @author RollW
 */
public sealed interface RawConfigValue permits RawScalarValue, RawListValue {
    /**
     * Identifies the structural shape of a raw configuration value.
     */
    enum Kind {
        /**
         * A single scalar value.
         */
        SCALAR,
        /**
         * An ordered list of raw values.
         */
        LIST
    }

    /**
     * Identifies the supported scalar source kinds.
     */
    enum ScalarKind {
        /**
         * A textual source value.
         */
        STRING,
        /**
         * A Boolean source value.
         */
        BOOLEAN,
        /**
         * An integral numeric source value.
         */
        INTEGER,
        /**
         * A decimal numeric source value.
         */
        DECIMAL
    }

    /**
     * Creates a raw string value.
     *
     * @param value the string value
     * @return the raw value
     */
    static RawConfigValue string(String value) {
        return new StringRawValue(Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a raw Boolean value.
     *
     * @param value the Boolean value
     * @return the raw value
     */
    static RawConfigValue booleanValue(boolean value) {
        return new BooleanRawValue(value);
    }

    /**
     * Creates a raw integral value.
     *
     * @param value the integral value
     * @return the raw value
     */
    static RawConfigValue integer(long value) {
        return new IntegerRawValue(value);
    }

    /**
     * Creates a raw decimal value.
     *
     * @param value the decimal value
     * @return the raw value
     */
    static RawConfigValue decimal(BigDecimal value) {
        return new DecimalRawValue(Objects.requireNonNull(value, "value"));
    }

    /**
     * Creates a raw value from one supported provider scalar.
     *
     * @param value the provider scalar or an existing raw value
     * @return the raw value
     * @throws IllegalArgumentException when the provider value is unsupported
     */
    static RawConfigValue from(Object value) {
        Object nonNullValue = Objects.requireNonNull(value, "value");
        if (nonNullValue instanceof RawConfigValue rawValue) {
            return rawValue;
        }
        if (nonNullValue instanceof String string) {
            return string(string);
        }
        if (nonNullValue instanceof Boolean booleanValue) {
            return booleanValue(booleanValue);
        }
        if (nonNullValue instanceof Number number) {
            return fromNumber(number);
        }
        throw new IllegalArgumentException("Unsupported raw configuration value type: "
                + nonNullValue.getClass().getName());
    }

    /**
     * Creates a raw list and converts supported provider scalar elements without merging boundaries.
     *
     * @param values the raw elements or supported provider scalar elements
     * @return the raw list
     */
    static RawConfigValue list(List<?> values) {
        List<RawConfigValue> elements = new ArrayList<>();
        for (Object value : Objects.requireNonNull(values, "values")) {
            elements.add(from(value));
        }
        return new RawListValue(elements);
    }

    /**
     * Creates a raw string list.
     *
     * @param values the string elements
     * @return the raw list
     */
    static RawConfigValue stringList(List<String> values) {
        return list(values);
    }

    /**
     * Returns the structural shape.
     *
     * @return the value kind
     */
    Kind getKind();

    /**
     * Returns the scalar kind.
     *
     * @return the scalar kind
     * @throws IllegalStateException when this value is a list
     */
    default ScalarKind getScalarKind() {
        throw notScalar();
    }

    /**
     * Returns the scalar object.
     *
     * @return the scalar object
     * @throws IllegalStateException when this value is a list
     */
    default Object requireScalar() {
        throw notScalar();
    }

    /**
     * Returns a raw string scalar.
     *
     * @return the string value
     * @throws IllegalStateException when the scalar is not a string
     */
    default String requireString() {
        requireScalarKind(ScalarKind.STRING);
        return (String) requireScalar();
    }

    /**
     * Returns a raw Boolean scalar.
     *
     * @return the Boolean value
     * @throws IllegalStateException when the scalar is not a Boolean
     */
    default boolean requireBoolean() {
        requireScalarKind(ScalarKind.BOOLEAN);
        return (Boolean) requireScalar();
    }

    /**
     * Returns a raw integral scalar.
     *
     * @return the integral value
     * @throws IllegalStateException when the scalar is not integral
     */
    default long requireInteger() {
        requireScalarKind(ScalarKind.INTEGER);
        return (Long) requireScalar();
    }

    /**
     * Returns a raw decimal scalar.
     *
     * @return the decimal value
     * @throws IllegalStateException when the scalar is not decimal
     */
    default BigDecimal requireDecimal() {
        requireScalarKind(ScalarKind.DECIMAL);
        return (BigDecimal) requireScalar();
    }

    /**
     * Returns the string representation of a string scalar.
     *
     * @return the string source value
     * @throws IllegalStateException when the scalar is not a string
     */
    default String requireText() {
        return requireString();
    }

    /**
     * Returns immutable list elements, retaining each element's scalar kind.
     *
     * @return the raw list elements
     * @throws IllegalStateException when this value is scalar
     */
    default List<RawConfigValue> requireList() {
        throw notList();
    }

    private void requireScalarKind(ScalarKind expected) {
        if (getScalarKind() != expected) {
            throw new IllegalStateException("Raw configuration scalar is not " + expected);
        }
    }

    private IllegalStateException notScalar() {
        return new IllegalStateException("Raw configuration value is not scalar");
    }

    private IllegalStateException notList() {
        return new IllegalStateException("Raw configuration value is not a list");
    }

    private static RawConfigValue fromNumber(Number number) {
        if (number instanceof Byte || number instanceof Short || number instanceof Integer
                || number instanceof Long || number instanceof BigInteger) {
            try {
                return integer(new BigInteger(number.toString()).longValueExact());
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Integral raw configuration value is out of range", exception);
            }
        }
        if (number instanceof Float || number instanceof Double) {
            double value = number.doubleValue();
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Decimal raw configuration value must be finite");
            }
            return decimal(BigDecimal.valueOf(value));
        }
        if (number instanceof BigDecimal decimal) {
            return decimal(decimal);
        }
        throw new IllegalArgumentException("Unsupported numeric raw configuration value type: "
                + number.getClass().getName());
    }
}
