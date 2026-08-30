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

import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.slf4j.Slf4jMdcScope;
import com.lamprism.luxspec.security.authentication.AccessTokenCredentials;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.Authenticator;
import com.lamprism.luxspec.security.token.access.AccessToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Authenticates standard Bearer access tokens without persisting Spring Security request state.
 *
 * <p>Install this filter after Spring Security's SecurityContextHolderFilter. It creates a
 * temporary authenticated context only while the downstream chain executes.</p>
 *
 * @author RollW
 */
public class LuxspecBearerAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationManager authenticationManager;
    private final AuthorizationHeaderBearerTokenResolver tokenResolver;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final @Nullable ExecutionContextStorage storage;
    private final SpringAuthenticationExecutionContextBridge contextBridge;

    /**
     * Creates a stateless filter with standard Authorization-header resolution and HTTP 401 failures.
     *
     * @param authenticator the access-token authenticator
     */
    public LuxspecBearerAuthenticationFilter(Authenticator<AccessTokenCredentials> authenticator) {
        this(
                new ProviderManager(new LuxspecAccessTokenAuthenticationProvider(authenticator)),
                new AuthorizationHeaderBearerTokenResolver(),
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                null
        );
    }

    /**
     * Creates a stateless filter with optional context storage.
     *
     * @param authenticator the access-token authenticator
     * @param storage       the optional context storage for authenticated request values
     */
    public LuxspecBearerAuthenticationFilter(
            Authenticator<AccessTokenCredentials> authenticator,
            @Nullable ExecutionContextStorage storage
    ) {
        this(
                new ProviderManager(new LuxspecAccessTokenAuthenticationProvider(authenticator)),
                new AuthorizationHeaderBearerTokenResolver(),
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                storage
        );
    }

    /**
     * Creates a stateless filter backed by a Spring AuthenticationManager.
     *
     * @param authenticationManager the manager that authenticates Bearer request tokens
     */
    public LuxspecBearerAuthenticationFilter(AuthenticationManager authenticationManager) {
        this(
                authenticationManager,
                new AuthorizationHeaderBearerTokenResolver(),
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                null
        );
    }

    /**
     * Creates a stateless filter with explicit resolution and failure policies.
     *
     * @param authenticationManager    the manager that authenticates Bearer request tokens
     * @param tokenResolver            the Bearer header resolver
     * @param authenticationEntryPoint the Spring Security authentication failure handler
     */
    public LuxspecBearerAuthenticationFilter(
            AuthenticationManager authenticationManager,
            AuthorizationHeaderBearerTokenResolver tokenResolver,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this(authenticationManager, tokenResolver, authenticationEntryPoint, null);
    }

    /**
     * Creates a stateless filter with optional context storage.
     *
     * @param authenticationManager    the manager that authenticates Bearer request tokens
     * @param tokenResolver            the Bearer header resolver
     * @param authenticationEntryPoint the Spring Security authentication failure handler
     * @param storage                  the optional context storage for authenticated request values
     */
    public LuxspecBearerAuthenticationFilter(
            AuthenticationManager authenticationManager,
            AuthorizationHeaderBearerTokenResolver tokenResolver,
            AuthenticationEntryPoint authenticationEntryPoint,
            @Nullable ExecutionContextStorage storage
    ) {
        this.authenticationManager = Objects.requireNonNull(authenticationManager, "authenticationManager");
        this.tokenResolver = Objects.requireNonNull(tokenResolver, "tokenResolver");
        this.authenticationEntryPoint = Objects.requireNonNull(authenticationEntryPoint, "authenticationEntryPoint");
        this.storage = storage;
        this.contextBridge = new SpringAuthenticationExecutionContextBridge();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Optional<AccessToken> token = tokenResolver.resolve(request);
            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }
            org.springframework.security.core.Authentication springAuthentication = authenticationManager.authenticate(
                    new LuxspecBearerAuthenticationToken(token.orElseThrow())
            );
            if (!(springAuthentication instanceof LuxspecSpringAuthentication luxspecAuthentication)) {
                throw new BadCredentialsException("Luxspec authentication provider returned an unsupported token");
            }
            continueWithAuthentication(
                    luxspecAuthentication.getLuxspecAuthentication(),
                    request,
                    response,
                    filterChain
            );
        } catch (com.lamprism.luxspec.security.authentication.AuthenticationException exception) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new LuxspecSpringAuthenticationException(exception)
            );
        } catch (org.springframework.security.core.AuthenticationException exception) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    exception
            );
        }
    }

    private void continueWithAuthentication(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        SecurityContext previousContext = SecurityContextHolder.getContext();
        SecurityContext authenticatedContext = SecurityContextHolder.createEmptyContext();
        authenticatedContext.setAuthentication(new LuxspecSpringAuthentication(authentication));
        SecurityContextHolder.setContext(authenticatedContext);
        if (storage == null) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.setContext(previousContext);
            }
            return;
        }
        ExecutionContext currentContext = storage.current().orElseGet(ExecutionContext::empty);
        ExecutionContext authenticatedExecutionContext = contextBridge.withAuthentication(
                currentContext,
                authentication
        );
        try {
            try (ExecutionContextStorage.Scope ignored = storage.open(authenticatedExecutionContext);
                 Slf4jMdcScope mdcScope = Slf4jMdcScope.open(authenticatedExecutionContext)) {
                filterChain.doFilter(request, response);
            }
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }
}
