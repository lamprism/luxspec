package com.lamprism.luxspec.resource;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.LuxspecException;

/**
 * Indicates a provider-independent resource failure.
 *
 * @author RollW
 */
public class ResourceException extends LuxspecException {
    /**
     * Creates a resource failure with a stable provider-independent error code.
     *
     * @param errorCode the stable resource error code
     * @param message the safe failure description
     */
    public ResourceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
