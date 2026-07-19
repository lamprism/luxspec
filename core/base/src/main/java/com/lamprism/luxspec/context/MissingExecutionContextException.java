package com.lamprism.luxspec.context;

import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.LuxspecException;

/**
 * Indicates that code requiring an ambient ExecutionContext ran without one.
 *
 * @author RollW
 */
public final class MissingExecutionContextException extends LuxspecException {
    /**
     * Creates a missing-context failure.
     */
    public MissingExecutionContextException() {
        super(CommonErrorCode.ILLEGAL_STATE, "No ExecutionContext is active");
    }
}
