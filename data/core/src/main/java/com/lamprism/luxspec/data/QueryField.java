package com.lamprism.luxspec.data;

import java.util.Objects;

/**
 * Identifies one domain-owned query field and its Java value type.
 *
 * @param <T> the field value type
 * @author RollW
 */
public final class QueryField<T> {
    private final String name;
    private final Class<T> valueType;

    private QueryField(String name, Class<T> valueType) {
        this.name = name;
        this.valueType = valueType;
    }

    /**
     * Creates a typed field identity.
     *
     * @param name the stable domain field name
     * @param valueType the non-primitive Java value type
     * @param <T> the field value type
     * @return the immutable field identity
     */
    public static <T> QueryField<T> of(String name, Class<T> valueType) {
        Class<T> nonNullValueType = Objects.requireNonNull(valueType, "valueType");
        if (nonNullValueType.isPrimitive()) {
            throw new IllegalArgumentException("Query field value types must use boxed classes");
        }
        return new QueryField<>(requireName(name), nonNullValueType);
    }

    /**
     * Returns the stable domain field name.
     *
     * @return the field name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the Java type accepted by this field.
     *
     * @return the field value type
     */
    public Class<T> getValueType() {
        return valueType;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QueryField<?> queryField)) {
            return false;
        }
        return name.equals(queryField.name) && valueType.equals(queryField.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, valueType);
    }

    @Override
    public String toString() {
        return name;
    }

    void requireValue(T value) {
        T nonNullValue = Objects.requireNonNull(value, "value");
        if (!valueType.isInstance(nonNullValue)) {
            throw new IllegalArgumentException("Query value does not match field type: " + name);
        }
    }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Query field name must not be empty");
        }
        boolean previousWasSeparator = false;
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (isAsciiLetter(character) || (character >= '0' && character <= '9')) {
                if (index == 0 && !isAsciiLetter(character)) {
                    throw new IllegalArgumentException("Query field name must start with a letter");
                }
                previousWasSeparator = false;
                continue;
            }
            if (character == '-' || character == '_') {
                if (index == 0 || previousWasSeparator || index == name.length() - 1) {
                    throw new IllegalArgumentException("Query field name contains an empty segment");
                }
                previousWasSeparator = true;
                continue;
            }
            throw new IllegalArgumentException("Query field name contains an unsupported character");
        }
        return name;
    }

    private static boolean isAsciiLetter(char character) {
        return (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
    }
}
