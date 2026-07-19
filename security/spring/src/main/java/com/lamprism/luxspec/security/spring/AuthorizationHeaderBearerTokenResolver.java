package com.lamprism.luxspec.security.spring;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.token.access.AccessToken;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpHeaders;

/**
 * Resolves one standard Bearer access token from HTTP Authorization headers.
 *
 * @author RollW
 */
public final class AuthorizationHeaderBearerTokenResolver {
    private static final String BEARER_SCHEME = "Bearer";

    /**
     * Resolves at most one Bearer token while leaving other authorization schemes untouched.
     *
     * @param request the current HTTP request
     * @return the Bearer access token when present
     * @throws AuthenticationException when a Bearer header is malformed or repeated
     */
    public Optional<AccessToken> resolve(HttpServletRequest request) {
        Enumeration<String> headerValues = Objects.requireNonNull(request, "request").getHeaders(HttpHeaders.AUTHORIZATION);
        AccessToken resolvedToken = null;
        while (headerValues.hasMoreElements()) {
            Optional<AccessToken> candidate = resolveHeader(headerValues.nextElement());
            if (candidate.isEmpty()) {
                continue;
            }
            if (resolvedToken != null) {
                throw invalidBearerToken();
            }
            resolvedToken = candidate.orElseThrow();
        }
        return Optional.ofNullable(resolvedToken);
    }

    private Optional<AccessToken> resolveHeader(String headerValue) {
        String nonNullHeaderValue = Objects.requireNonNull(headerValue, "headerValue");
        int schemeEnd = findWhitespace(nonNullHeaderValue, 0);
        if (schemeEnd < 0) {
            if (BEARER_SCHEME.equalsIgnoreCase(nonNullHeaderValue)) {
                throw invalidBearerToken();
            }
            return Optional.empty();
        }
        String scheme = nonNullHeaderValue.substring(0, schemeEnd);
        if (!BEARER_SCHEME.equalsIgnoreCase(scheme)) {
            return Optional.empty();
        }
        int tokenStart = skipWhitespace(nonNullHeaderValue, schemeEnd);
        if (tokenStart == nonNullHeaderValue.length()) {
            throw invalidBearerToken();
        }
        String tokenValue = nonNullHeaderValue.substring(tokenStart);
        if (findWhitespace(tokenValue, 0) >= 0) {
            throw invalidBearerToken();
        }
        return Optional.of(new AccessToken(tokenValue));
    }

    private static int findWhitespace(String value, int startIndex) {
        for (int index = startIndex; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private static int skipWhitespace(String value, int startIndex) {
        int index = startIndex;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static AuthenticationException invalidBearerToken() {
        return new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Bearer access token is invalid");
    }
}
