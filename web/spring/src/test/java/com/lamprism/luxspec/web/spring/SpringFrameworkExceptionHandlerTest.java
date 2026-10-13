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

import com.lamprism.luxspec.web.DefaultErrorHttpStatusResolver;
import com.lamprism.luxspec.web.HttpResponse;
import com.lamprism.luxspec.web.ValidationError;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpringFrameworkExceptionHandlerTest {
    private final LuxspecExceptionHandler handler = new LuxspecExceptionHandler(
            new DefaultErrorHttpStatusResolver(List.of())
    );

    @Test
    void convertsBindingFailuresToSafeFieldErrors() {
        BindException exception = new BindException(new Object(), "request");
        exception.addError(new FieldError(
                "request",
                "email",
                "database password and parser details"
        ));

        ResponseEntity<HttpResponse<List<ValidationError>>> entity = handler.handleBinding(
                exception,
                Locale.US
        );
        HttpResponse<List<ValidationError>> response = entity.getBody();

        assertEquals(HttpStatus.BAD_REQUEST.value(), responseStatusValue(entity));
        assertEquals("common:invalid-argument", response.status().code());
        assertEquals(1, response.data().size());
        assertEquals("email", response.data().get(0).field());
        assertNull(response.data().get(0).message());
    }

    @Test
    void convertsMethodValidationFailuresToARequestErrorWhenNoFieldIsAvailable() throws Exception {
        Method method = SampleController.class.getDeclaredMethod("endpoint", Integer.class);
        HandlerMethodValidationException exception = new HandlerMethodValidationException(
                MethodValidationResult.create(
                        new Object(),
                        method,
                        List.of(new ParameterValidationResult(
                                new MethodParameter(method, 0),
                                new Object(),
                                List.of(new DefaultMessageSourceResolvable("validation")),
                                new Object(),
                                0,
                                new Object(),
                                (resolvable, targetType) -> new Object()
                        ))
                )
        );

        HttpResponse<List<ValidationError>> response = handler.handleMethodValidation(
                exception,
                Locale.US
        ).getBody();

        assertEquals("common:invalid-argument", response.status().code());
        assertEquals("limit", response.data().get(0).field());
    }

    @Test
    void convertsConstraintViolationsToARequestErrorWhenNoViolationDetailsAreAvailable() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of());

        HttpResponse<List<ValidationError>> response = handler.handleConstraintViolation(
                exception,
                Locale.US
        ).getBody();

        assertEquals("common:invalid-argument", response.status().code());
        assertEquals("request", response.data().get(0).field());
    }

    @Test
    void hidesMalformedBodyAndTypeMismatchDetails() throws Exception {
        HttpMessageNotReadableException unreadableBody = new HttpMessageNotReadableException(
                "raw parser and SQL details",
                new MockHttpInputMessage(new byte[0])
        );
        Method method = SampleController.class.getDeclaredMethod("endpoint", Integer.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentTypeMismatchException typeMismatch = new MethodArgumentTypeMismatchException(
                "raw value",
                Integer.class,
                "limit",
                parameter,
                new IllegalArgumentException("raw conversion details")
        );

        HttpResponse<List<ValidationError>> bodyResponse = handler.handleUnreadableBody(
                unreadableBody,
                Locale.US
        ).getBody();
        HttpResponse<List<ValidationError>> parameterResponse = handler.handleTypeMismatch(
                typeMismatch,
                Locale.US
        ).getBody();

        assertEquals("body", bodyResponse.data().get(0).field());
        assertEquals("limit", parameterResponse.data().get(0).field());
        assertNull(bodyResponse.status().message());
        assertNull(parameterResponse.status().message());
    }

    @Test
    void mapsProtocolFailuresToStableCodesAndStatuses() throws Exception {
        HttpResponse<Void> methodResponse = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("TRACE"),
                Locale.US
        ).getBody();
        HttpResponse<Void> acceptResponse = handler.handleNotAcceptable(
                new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_XML)),
                Locale.US
        ).getBody();
        HttpResponse<Void> mediaResponse = handler.handleUnsupportedMediaType(
                new HttpMediaTypeNotSupportedException(
                        MediaType.APPLICATION_XML,
                        List.of(MediaType.APPLICATION_JSON)
                ),
                Locale.US
        ).getBody();

        assertEquals("web:method-not-allowed", methodResponse.status().code());
        assertEquals("web:not-acceptable", acceptResponse.status().code());
        assertEquals("web:unsupported-media-type", mediaResponse.status().code());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), responseStatusValue(
                handler.handleMethodNotAllowed(
                        new HttpRequestMethodNotSupportedException("TRACE"),
                        Locale.US
                )
        ));
        assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), responseStatusValue(
                handler.handleNotAcceptable(
                        new HttpMediaTypeNotAcceptableException(List.of(MediaType.APPLICATION_XML)),
                        Locale.US
                )
        ));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), responseStatusValue(
                handler.handleUnsupportedMediaType(
                        new HttpMediaTypeNotSupportedException(
                                MediaType.APPLICATION_XML,
                                List.of(MediaType.APPLICATION_JSON)
                        ),
                        Locale.US
                )
        ));
    }

    @Test
    void mapsMissingHandlersAndResourcesToNotFound() {
        HttpResponse<Void> handlerResponse = handler.handleNoHandlerFound(
                new NoHandlerFoundException("GET", "/missing", new HttpHeaders()),
                Locale.US
        ).getBody();
        HttpResponse<Void> resourceResponse = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/missing", "raw details"),
                Locale.US
        ).getBody();

        assertEquals("common:not-found", handlerResponse.status().code());
        assertEquals("common:not-found", resourceResponse.status().code());
    }

    @Test
    void hidesUnexpectedFailureDetailsBehindInternalError() {
        HttpResponse<Void> response = handler.handleUnexpected(
                new IllegalStateException("database password and SQL details"),
                Locale.US
        ).getBody();

        assertEquals("common:internal", response.status().code());
        assertNull(response.status().message());
        assertNull(response.data());
    }

    private int responseStatusValue(
            ResponseEntity<?> response
    ) {
        return response.getStatusCode().value();
    }

    private static final class SampleController {
        private static void endpoint(Integer limit) {
        }
    }
}
