package com.lamprism.luxspec.data;

/**
 * Identifies one supported field-level structured query operation.
 *
 * @author RollW
 */
public enum QueryOperator {
    /** Matches an equal value. */
    EQUAL,
    /** Matches a non-equal value. */
    NOT_EQUAL,
    /** Matches a value greater than the comparison value. */
    GREATER_THAN,
    /** Matches a value greater than or equal to the comparison value. */
    GREATER_THAN_OR_EQUAL,
    /** Matches a value less than the comparison value. */
    LESS_THAN,
    /** Matches a value less than or equal to the comparison value. */
    LESS_THAN_OR_EQUAL,
    /** Matches a String pattern. */
    LIKE,
    /** Matches one value in a supplied collection. */
    IN,
    /** Matches no value in a supplied collection. */
    NOT_IN
}
