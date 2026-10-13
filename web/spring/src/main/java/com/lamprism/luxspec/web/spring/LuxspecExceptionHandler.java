/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.web.spring;

import com.lamprism.luxspec.CommonErrorCode;
import com.lamprism.luxspec.ErrorCode;
import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.message.MessageResolver;
import com.lamprism.luxspec.web.ErrorHttpStatusResolver;
import com.lamprism.luxspec.web.HttpResponse;
import com.lamprism.luxspec.web.HttpStatusCode;
import com.lamprism.luxspec.web.ResponseStatus;
import com.lamprism.luxspec.web.ValidationError;
import com.lamprism.luxspec.web.WebErrorCode;
import com.lamprism.luxspec.web.query.QueryBindingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Converts error-code-carrying exceptions into ordinary Luxspec JSON error envelopes.
 *
 * <p>This advice maps a domain failure into an HTTP result. It is intentionally not a
 * {@link com.lamprism.luxspec.failure.FailureHandler}: that callback observes a failure and has no
 * result, while this boundary must return a protocol response without exposing internal details.</p>
 *
 * @author RollW
 */
@RestControllerAdvice
public class LuxspecExceptionHandler {
    private final ErrorHttpStatusResolver statusResolver;
    private final @Nullable MessageResolver messageResolver;

    /**
     * Creates an exception advice with an explicit business-error HTTP mapping policy.
     *
     * @param statusResolver the business-error HTTP mapping policy
     */
    public LuxspecExceptionHandler(ErrorHttpStatusResolver statusResolver) {
        this(statusResolver, null);
    }

    /**
     * Creates an exception advice with optional safe message localization.
     *
     * @param statusResolver  the business-error HTTP mapping policy
     * @param messageResolver the optional message resolver
     */
    public LuxspecExceptionHandler(
            ErrorHttpStatusResolver statusResolver,
            @Nullable MessageResolver messageResolver
    ) {
        this.statusResolver = Objects.requireNonNull(statusResolver, "statusResolver");
        this.messageResolver = messageResolver;
    }

    /**
     * Maps a stable error-code-carrying exception without exposing internal exception details.
     *
     * @param exception the error-code-carrying exception
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(LuxspecException.class)
    public ResponseEntity<HttpResponse<Void>> handle(LuxspecException exception, Locale locale) {
        LuxspecException nonNullException = Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(nonNullException.getErrorCode(), null, nonNullLocale);
    }

    /**
     * Maps a provider-independent query binding failure to the common invalid-argument code.
     *
     * @param exception the query binding failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(QueryBindingException.class)
    public ResponseEntity<HttpResponse<Void>> handleQueryBinding(
            QueryBindingException exception,
            Locale locale
    ) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(CommonErrorCode.INVALID_ARGUMENT, null, nonNullLocale);
    }

    /**
     * Maps binding and bean validation failures to safe field-level errors.
     *
     * @param exception the Spring binding failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<HttpResponse<List<ValidationError>>> handleBinding(
            BindException exception,
            Locale locale
    ) {
        BindException nonNullException = Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(
                CommonErrorCode.INVALID_ARGUMENT,
                validationErrors(nonNullException.getBindingResult(), nonNullLocale),
                nonNullLocale
        );
    }

    /**
     * Maps method-level validation failures to safe field-level errors.
     *
     * @param exception the Spring method validation failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<HttpResponse<List<ValidationError>>> handleMethodValidation(
            HandlerMethodValidationException exception,
            Locale locale
    ) {
        HandlerMethodValidationException nonNullException = Objects.requireNonNull(
                exception,
                "exception"
        );
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        List<ValidationError> validationErrors = new ArrayList<>();
        for (ParameterValidationResult result : nonNullException.getParameterValidationResults()) {
            if (result instanceof Errors errors) {
                addBindingErrors(validationErrors, errors, nonNullLocale);
            } else {
                validationErrors.add(validationError(
                        fieldName(result.getMethodParameter().getParameterName(), "request"),
                        nonNullLocale
                ));
            }
        }
        if (!nonNullException.getCrossParameterValidationResults().isEmpty()) {
            validationErrors.add(validationError("request", nonNullLocale));
        }
        return failure(
                CommonErrorCode.INVALID_ARGUMENT,
                completeValidationErrors(validationErrors, nonNullLocale),
                nonNullLocale
        );
    }

    /**
     * Maps Jakarta Bean Validation failures to safe field-level errors.
     *
     * @param exception the bean validation failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<HttpResponse<List<ValidationError>>> handleConstraintViolation(
            ConstraintViolationException exception,
            Locale locale
    ) {
        ConstraintViolationException nonNullException = Objects.requireNonNull(
                exception,
                "exception"
        );
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        List<ValidationError> validationErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : nonNullException.getConstraintViolations()) {
            String path = String.valueOf(violation.getPropertyPath());
            validationErrors.add(validationError(fieldName(path, "request"), nonNullLocale));
        }
        return failure(
                CommonErrorCode.INVALID_ARGUMENT,
                completeValidationErrors(validationErrors, nonNullLocale),
                nonNullLocale
        );
    }

    /**
     * Maps malformed request bodies without returning parser or converter details.
     *
     * @param exception the unreadable request-body failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<HttpResponse<List<ValidationError>>> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            Locale locale
    ) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(
                CommonErrorCode.INVALID_ARGUMENT,
                List.of(validationError("body", nonNullLocale)),
                nonNullLocale
        );
    }

    /**
     * Maps scalar request-parameter conversion failures without exposing the rejected value.
     *
     * @param exception the type-mismatch failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<HttpResponse<List<ValidationError>>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            Locale locale
    ) {
        MethodArgumentTypeMismatchException nonNullException = Objects.requireNonNull(
                exception,
                "exception"
        );
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(
                CommonErrorCode.INVALID_ARGUMENT,
                List.of(validationError(
                        fieldName(nonNullException.getName(), "request"),
                        nonNullLocale
                )),
                nonNullLocale
        );
    }

    /**
     * Maps request binding failures to a generic request validation error.
     *
     * @param exception the request binding failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<HttpResponse<List<ValidationError>>> handleRequestBinding(
            ServletRequestBindingException exception,
            Locale locale
    ) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(
                CommonErrorCode.INVALID_ARGUMENT,
                List.of(validationError("request", nonNullLocale)),
                nonNullLocale
        );
    }

    /**
     * Maps unsupported request methods to the protocol error contract.
     *
     * @param exception the unsupported-method failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<HttpResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            Locale locale
    ) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(WebErrorCode.METHOD_NOT_ALLOWED, null, nonNullLocale);
    }

    /**
     * Maps unacceptable response media types to the protocol error contract.
     *
     * @param exception the unacceptable-media-type failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<HttpResponse<Void>> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException exception,
            Locale locale
    ) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(WebErrorCode.NOT_ACCEPTABLE, null, nonNullLocale);
    }

    /**
     * Maps unsupported request media types to the protocol error contract.
     *
     * @param exception the unsupported-media-type failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<HttpResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            Locale locale
    ) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(WebErrorCode.UNSUPPORTED_MEDIA_TYPE, null, nonNullLocale);
    }

    /**
     * Maps a missing MVC handler to the standard not-found error.
     *
     * @param exception the missing-handler failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<HttpResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException exception,
            Locale locale
    ) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(CommonErrorCode.NOT_FOUND, null, nonNullLocale);
    }

    /**
     * Maps a missing static resource to the standard not-found error.
     *
     * @param exception the missing-resource failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<HttpResponse<Void>> handleNoResourceFound(
            NoResourceFoundException exception,
            Locale locale
    ) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(CommonErrorCode.NOT_FOUND, null, nonNullLocale);
    }

    /**
     * Maps unexpected failures to a safe internal-server-error envelope.
     *
     * @param exception the unexpected failure
     * @param locale    the request locale
     * @return the HTTP response entity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpResponse<Void>> handleUnexpected(Exception exception, Locale locale) {
        Objects.requireNonNull(exception, "exception");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        return failure(CommonErrorCode.INTERNAL, null, nonNullLocale);
    }

    private <T> ResponseEntity<HttpResponse<T>> failure(
            ErrorCode errorCode,
            @Nullable T data,
            Locale locale
    ) {
        ErrorCode nonNullErrorCode = Objects.requireNonNull(errorCode, "errorCode");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        ResponseStatus status = ResponseStatus.failure(
                nonNullErrorCode,
                resolveMessage(nonNullErrorCode, nonNullLocale)
        );
        HttpStatusCode httpStatusCode = statusResolver.resolve(nonNullErrorCode);
        return ResponseEntity.status(httpStatusCode.value())
                .body(HttpResponse.failure(status, data, null));
    }

    private List<ValidationError> validationErrors(
            Errors errors,
            Locale locale
    ) {
        List<ValidationError> validationErrors = new ArrayList<>();
        addBindingErrors(validationErrors, errors, locale);
        return completeValidationErrors(validationErrors, locale);
    }

    private void addBindingErrors(
            List<ValidationError> validationErrors,
            Errors errors,
            Locale locale
    ) {
        for (ObjectError error : errors.getAllErrors()) {
            String field = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();
            validationErrors.add(validationError(fieldName(field, "request"), locale));
        }
    }

    private List<ValidationError> completeValidationErrors(
            List<ValidationError> validationErrors,
            Locale locale
    ) {
        if (validationErrors.isEmpty()) {
            validationErrors.add(validationError("request", locale));
        }
        return List.copyOf(validationErrors);
    }

    private ValidationError validationError(String field, Locale locale) {
        return new ValidationError(
                field,
                CommonErrorCode.INVALID_ARGUMENT,
                resolveMessage(CommonErrorCode.INVALID_ARGUMENT, locale)
        );
    }

    private String fieldName(@Nullable String field, String fallback) {
        if (field == null || field.isBlank()) {
            return fallback;
        }
        return field;
    }

    private @Nullable String resolveMessage(ErrorCode errorCode, Locale locale) {
        if (messageResolver == null) {
            return null;
        }
        return messageResolver.resolve(errorCode.getCode(), locale);
    }
}
