package com.lamprism.luxspec.config.source;

/**
 * Represents one scalar raw configuration value.
 *
 * @author RollW
 */
public sealed interface RawScalarValue extends RawConfigValue
        permits StringRawValue, BooleanRawValue, IntegerRawValue, DecimalRawValue {
    @Override
    default Kind getKind() {
        return Kind.SCALAR;
    }

    @Override
    ScalarKind getScalarKind();

    @Override
    Object requireScalar();
}
