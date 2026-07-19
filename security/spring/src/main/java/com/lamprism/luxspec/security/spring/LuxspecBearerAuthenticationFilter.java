package com.lamprism.luxspec.security.spring;

import com.lamprism.luxspec.context.ExecutionContexts;
import com.lamprism.luxspec.security.authentication.AccessTokenCredentials;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Authenticator;
import com.lamprism.luxspec.security.token.access.AccessToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates standard Bearer access tokens without persisting Spring Security request state.
 *
 * <p>Install this filter after Spring Security's SecurityContextHolderFilter. It creates a
 * temporary authenticated context only while the downstream chain executes.</p>
 *
 * @author RollW
 */
public final class LuxspecBearerAuthenticationFilter extends OncePerRequestFilter {
    private final Authenticator<AccessTokenCredentials> authenticator;
    private final AuthorizationHeaderBearerTokenResolver tokenResolver;
    private final SpringAuthenticationExecutionContextBridge contextBridge;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Creates a stateless filter with standard Authorization-header resolution and HTTP 401 failures.
     *
     * @param authenticator the access-token authenticator
     */
    public LuxspecBearerAuthenticationFilter(Authenticator<AccessTokenCredentials> authenticator) {
        this(
                authenticator,
                new AuthorizationHeaderBearerTokenResolver(),
                new SpringAuthenticationExecutionContextBridge(),
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
        );
    }

    /**
     * Creates a stateless filter with explicit resolution, context, and failure policies.
     *
     * @param authenticator the access-token authenticator
     * @param tokenResolver the Bearer header resolver
     * @param contextBridge the downstream ExecutionContext bridge
     * @param authenticationEntryPoint the Spring Security authentication failure handler
     */
    public LuxspecBearerAuthenticationFilter(
            Authenticator<AccessTokenCredentials> authenticator,
            AuthorizationHeaderBearerTokenResolver tokenResolver,
            SpringAuthenticationExecutionContextBridge contextBridge,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.tokenResolver = Objects.requireNonNull(tokenResolver, "tokenResolver");
        this.contextBridge = Objects.requireNonNull(contextBridge, "contextBridge");
        this.authenticationEntryPoint = Objects.requireNonNull(authenticationEntryPoint, "authenticationEntryPoint");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Optional<AccessToken> token = tokenResolver.resolve(request);
            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }
            Authentication authentication = authenticator.authenticate(new AccessTokenCredentials(token.orElseThrow()));
            continueWithAuthentication(authentication, request, response, filterChain);
        } catch (AuthenticationException exception) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new LuxspecSpringAuthenticationException(exception)
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
        try (ExecutionContexts.Scope ignored = contextBridge.open(authentication)) {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }
}
