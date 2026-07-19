package com.lamprism.luxspec.data;

/**
 * Identifies the connective used by a logical query group.
 *
 * @author RollW
 */
public enum LogicalOperator {
    /** Requires every child expression to match. */
    AND,
    /** Requires at least one child expression to match. */
    OR
}
