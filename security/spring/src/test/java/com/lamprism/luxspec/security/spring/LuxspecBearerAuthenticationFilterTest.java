package com.lamprism.luxspec.security.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.context.ExecutionContexts;
import com.lamprism.luxspec.security.authentication.AccessTokenCredentials;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Authenticator;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.authentication.CredentialType;
import com.lamprism.luxspec.security.authentication.SecurityContextKeys;
import com.lamprism.luxspec.security.authentication.UserSubject;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class LuxspecBearerAuthenticationFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesBearerCredentialsAndBridgesTheExecutionContext() throws Exception {
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
        assertTrue(ExecutionContexts.current().isEmpty());
    }

    @Test
    void leavesTheChainUnchangedWhenNoBearerCredentialsArePresent() throws Exception {
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
    void rejectsMalformedOrRejectedBearerCredentialsWithoutRunningTheChain() throws Exception {
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
            assertSame(
                    expectedAuthentication,
                    ExecutionContexts.requireCurrent().get(SecurityContextKeys.AUTHENTICATION).orElseThrow()
            );
            chainReached.set(true);
        };
    }

    private static Authentication authentication() {
        return new Authentication(
                new UserSubject(42L),
                AuthorizationGrantSet.of(List.of(AuthorizationScope.of("account:read")))
        );
    }

    private static final class TestAuthenticator implements Authenticator<AccessTokenCredentials> {
        private static final CredentialType<AccessTokenCredentials> CREDENTIAL_TYPE = CredentialType.of(
                "test-access-token",
                AccessTokenCredentials.class
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
        public CredentialType<AccessTokenCredentials> getCredentialType() {
            return CREDENTIAL_TYPE;
        }

        @Override
        public Authentication authenticate(AccessTokenCredentials credentials) {
            AccessTokenCredentials nonNullCredentials = Objects.requireNonNull(credentials, "credentials");
            invocationCount++;
            presentedTokenValue = nonNullCredentials.getAccessToken().getValue();
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
