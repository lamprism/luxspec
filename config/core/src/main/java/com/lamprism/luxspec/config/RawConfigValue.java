package com.lamprism.luxspec.config;

import java.util.List;
import java.util.Objects;

/**
 * Represents provider-neutral scalar or list data before codec conversion.
 *
 * @author RollW
 */
public final class RawConfigValue {
    /**
     * Identifies the structural shape of a raw configuration value.
     */
    public enum Kind {
        SCALAR,
        LIST
    }

    private final Kind kind;
    private final String scalar;
    private final List<String> list;

    private RawConfigValue(Kind kind, String scalar, List<String> list) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.scalar = scalar;
        this.list = list;
    }

    /**
     * Creates a raw scalar value.
     *
     * @param value the scalar source value
     * @return the raw scalar value
     */
    public static RawConfigValue scalar(String value) {
        return new RawConfigValue(Kind.SCALAR, Objects.requireNonNull(value, "value"), List.of());
    }

    /**
     * Creates a raw list value while preserving each element boundary.
     *
     * @param values the source list elements
     * @return the raw list value
     */
    public static RawConfigValue list(List<String> values) {
        return new RawConfigValue(Kind.LIST, null, List.copyOf(values));
    }

    /**
     * Returns the structural shape of this value.
     *
     * @return the value kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the scalar value.
     *
     * @return the scalar source value
     * @throws IllegalStateException when this value is a list
     */
    public String requireScalar() {
        if (kind != Kind.SCALAR) {
            throw new IllegalStateException("Raw configuration value is not scalar");
        }
        return scalar;
    }

    /**
     * Returns the immutable list elements.
     *
     * @return the source list elements
     * @throws IllegalStateException when this value is scalar
     */
    public List<String> requireList() {
        if (kind != Kind.LIST) {
            throw new IllegalStateException("Raw configuration value is not a list");
        }
        return list;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RawConfigValue value)) {
            return false;
        }
        return kind == value.kind && Objects.equals(scalar, value.scalar) && list.equals(value.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, scalar, list);
    }
}
