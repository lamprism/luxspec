package com.lamprism.luxspec.config.source;

import java.util.List;
import java.util.Objects;

/**
 * Represents an immutable ordered list of raw configuration values.
 *
 * @author RollW
 */
public final class RawListValue implements RawConfigValue {
    private final List<RawConfigValue> elements;

    RawListValue(List<? extends RawConfigValue> elements) {
        this.elements = List.copyOf(Objects.requireNonNull(elements, "elements"));
    }

    @Override
    public Kind getKind() {
        return Kind.LIST;
    }

    @Override
    public List<RawConfigValue> requireList() {
        return elements;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RawListValue value)) {
            return false;
        }
        return elements.equals(value.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Kind.LIST, elements);
    }
}
