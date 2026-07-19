package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.resource.ResourceException;

/**
 * Indicates that an otherwise supported resource action was denied.
 *
 * @author RollW
 */
public final class ResourceAccessDeniedException extends ResourceException {
    /**
     * Creates a denial exception with its stable authorization reason.
     *
     * @param reasonCode the stable denial reason
     */
    public ResourceAccessDeniedException(ErrorCode reasonCode) {
        super(reasonCode, "Resource action was denied");
    }
}
