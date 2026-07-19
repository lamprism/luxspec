package com.lamprism.luxspec;

/**
 * A stable provider-independent business error identifier.
 *
 * @author RollW
 */
public interface ErrorCode {
    /**
     * Returns the stable canonical business error identity.
     *
     * @return the error code
     */
    String getCode();
}
