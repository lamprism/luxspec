package com.lamprism.luxspec.web.spring;

import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Provides conservative default HTTP statuses for common foundation errors.
 *
 * @author RollW
 */
public final class DefaultErrorHttpStatusResolver implements ErrorHttpStatusResolver {
    @Override
    public HttpStatusCode resolve(ErrorCode errorCode) {
        if (errorCode == CommonErrorCode.INVALID_ARGUMENT) {
            return HttpStatus.BAD_REQUEST;
        }
        if (errorCode == CommonErrorCode.NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        if (errorCode == CommonErrorCode.UNSUPPORTED_OPERATION) {
            return HttpStatus.NOT_IMPLEMENTED;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
