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

package com.lamprism.luxspec.security.spring.authentication;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import com.lamprism.luxspec.security.authentication.AccessTokenCredential;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Authenticator;
import com.lamprism.luxspec.security.authentication.CredentialType;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuxspecBearerAuthenticationFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesBearerCredentialForTheDownstreamChain() throws Exception {
        Authentication authentication = authentication();
        TestAuthenticator authenticator = new TestAuthenticator(authentication, false);
        LuxspecBearerAuthenticationFilter filter = new LuxspecBearerAuthenticationFilter(authenticator);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer opaque-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean();

        filter.doFilter(request, response, verifyingChain(authentication, chainReached));

        assertTrue(chainReached.get());
        assertEquals(1, authenticator.getInvocationCount());
        assertEquals("opaque-access-token", authenticator.getPresentedTokenValue());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void opensTheExplicitAuthenticationContextWhenConfigured() throws Exception {
        Authentication authentication = authentication();
        ExecutionContextStorage storage = new ThreadLocalExecutionContextStorage();
        LuxspecBearerAuthenticationFilter filter = new LuxspecBearerAuthenticationFilter(
                new TestAuthenticator(authentication, false),
                storage
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer opaque-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> assertSame(
                authentication,
                storage.requireCurrent().get(Authentication.CONTEXT_KEY).orElseThrow()
        ));

        assertTrue(storage.current().isEmpty());
    }

    @Test
    void leavesTheChainUnchangedWhenNoBearerCredentialArePresent() throws Exception {
        TestAuthenticator authenticator = new TestAuthenticator(authentication(), false);
        LuxspecBearerAuthenticationFilter filter = new LuxspecBearerAuthenticationFilter(authenticator);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainReached = new AtomicBoolean();

        filter.doFilter(new MockHttpServletRequest(), response, (request, servletResponse) -> chainReached.set(true));

        assertTrue(chainReached.get());
        assertEquals(0, authenticator.getInvocationCount());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsMalformedOrRejectedBearerCredentialWithoutRunningTheChain() throws Exception {
        TestAuthenticator authenticator = new TestAuthenticator(authentication(), true);
        LuxspecBearerAuthenticationFilter filter = new LuxspecBearerAuthenticationFilter(authenticator);
        MockHttpServletRequest malformedRequest = new MockHttpServletRequest();
        malformedRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer");
        MockHttpServletResponse malformedResponse = new MockHttpServletResponse();
        AtomicBoolean malformedChainReached = new AtomicBoolean();

        filter.doFilter(
                malformedRequest,
                malformedResponse,
                (request, response) -> malformedChainReached.set(true)
        );

        MockHttpServletRequest rejectedRequest = new MockHttpServletRequest();
        rejectedRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer opaque-access-token");
        MockHttpServletResponse rejectedResponse = new MockHttpServletResponse();
        AtomicBoolean rejectedChainReached = new AtomicBoolean();
        filter.doFilter(
                rejectedRequest,
                rejectedResponse,
                (request, response) -> rejectedChainReached.set(true)
        );

        assertEquals(401, malformedResponse.getStatus());
        assertFalse(malformedChainReached.get());
        assertEquals(401, rejectedResponse.getStatus());
        assertFalse(rejectedChainReached.get());
        assertEquals(1, authenticator.getInvocationCount());
    }

    private static FilterChain verifyingChain(Authentication expectedAuthentication, AtomicBoolean chainReached) {
        return (request, response) -> {
            LuxspecSpringAuthentication springAuthentication = assertInstanceOf(
                    LuxspecSpringAuthentication.class,
                    SecurityContextHolder.getContext().getAuthentication()
            );
            assertSame(expectedAuthentication, springAuthentication.getLuxspecAuthentication());
            chainReached.set(true);
        };
    }

    private static Authentication authentication() {
        return new Authentication(
                new UserSubject(42L),
                AuthorizationGrantSet.of(List.of(AuthorizationScope.of("account:read")))
        );
    }

    private static final class TestAuthenticator implements Authenticator<AccessTokenCredential> {
        private static final CredentialType<AccessTokenCredential> CREDENTIAL_TYPE = CredentialType.of(
                "test-access-token",
                AccessTokenCredential.class
        );

        private final Authentication authentication;
        private final boolean rejected;
        private int invocationCount;
        private String presentedTokenValue;

        private TestAuthenticator(Authentication authentication, boolean rejected) {
            this.authentication = Objects.requireNonNull(authentication, "authentication");
            this.rejected = rejected;
        }

        @Override
        public CredentialType<AccessTokenCredential> getCredentialType() {
            return CREDENTIAL_TYPE;
        }

        @Override
        public Authentication authenticate(AccessTokenCredential credentials) {
            AccessTokenCredential nonNullCredential = Objects.requireNonNull(credentials, "credentials");
            invocationCount++;
            presentedTokenValue = nonNullCredential.getAccessToken().getValue();
            if (rejected) {
                throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Access token is invalid");
            }
            return authentication;
        }

        private int getInvocationCount() {
            return invocationCount;
        }

        private String getPresentedTokenValue() {
            return presentedTokenValue;
        }
    }
}
