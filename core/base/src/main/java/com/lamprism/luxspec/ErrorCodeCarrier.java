package com.lamprism.luxspec;

/**
 * Exposes the stable error code carried by an object.
 *
 * @author RollW
 */
public interface ErrorCodeCarrier {
    /**
     * Returns the non-null stable business error carried by this object.
     *
     * @return the error code
     */
    ErrorCode getErrorCode();
}
