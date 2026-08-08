package com.lamprism.luxspec.config.source;

import java.math.BigDecimal;
import java.util.Objects;

final class StringRawValue implements RawScalarValue {
    private final String value;

    StringRawValue(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public ScalarKind getScalarKind() {
        return ScalarKind.STRING;
    }

    @Override
    public Object requireScalar() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StringRawValue value)) {
            return false;
        }
        return this.value.equals(value.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Kind.SCALAR, ScalarKind.STRING, value);
    }
}

final class BooleanRawValue implements RawScalarValue {
    private final boolean value;

    BooleanRawValue(boolean value) {
        this.value = value;
    }

    @Override
    public ScalarKind getScalarKind() {
        return ScalarKind.BOOLEAN;
    }

    @Override
    public Object requireScalar() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BooleanRawValue value)) {
            return false;
        }
        return this.value == value.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Kind.SCALAR, ScalarKind.BOOLEAN, value);
    }
}

final class IntegerRawValue implements RawScalarValue {
    private final long value;

    IntegerRawValue(long value) {
        this.value = value;
    }

    @Override
    public ScalarKind getScalarKind() {
        return ScalarKind.INTEGER;
    }

    @Override
    public Object requireScalar() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IntegerRawValue value)) {
            return false;
        }
        return this.value == value.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Kind.SCALAR, ScalarKind.INTEGER, value);
    }
}

final class DecimalRawValue implements RawScalarValue {
    private final BigDecimal value;

    DecimalRawValue(BigDecimal value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public ScalarKind getScalarKind() {
        return ScalarKind.DECIMAL;
    }

    @Override
    public Object requireScalar() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DecimalRawValue decimal)) {
            return false;
        }
        return value.equals(decimal.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Kind.SCALAR, ScalarKind.DECIMAL, value);
    }
}
