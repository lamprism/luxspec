package com.lamprism.luxspec.web.spring;

import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.web.HttpResponse;
import com.lamprism.luxspec.web.ResponseStatus;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts error-code-carrying exceptions into ordinary Luxspec JSON error envelopes.
 *
 * @author RollW
 */
@RestControllerAdvice
public final class LuxspecExceptionHandler {
    private final ErrorHttpStatusResolver statusResolver;

    /**
     * Creates an exception advice with an explicit business-error HTTP mapping policy.
     */
    public LuxspecExceptionHandler(ErrorHttpStatusResolver statusResolver) {
        this.statusResolver = Objects.requireNonNull(statusResolver, "statusResolver");
    }

    /**
     * Maps a stable error-code-carrying exception without exposing internal exception details.
     */
    @ExceptionHandler(LuxspecException.class)
    public ResponseEntity<HttpResponse<Void>> handle(LuxspecException exception) {
        ResponseStatus status = ResponseStatus.failure(exception.getErrorCode(), null);
        return ResponseEntity.status(statusResolver.resolve(exception.getErrorCode()))
                .body(HttpResponse.failure(status, null, null));
    }
}
