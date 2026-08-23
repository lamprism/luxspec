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

import com.lamprism.luxspec.security.authentication.AccessTokenCredentials;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Authenticator;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

import java.util.Objects;

/**
 * Adapts the provider-independent Access Token authenticator to Spring Security.
 *
 * @author RollW
 */
public class LuxspecAccessTokenAuthenticationProvider implements AuthenticationProvider {
    private final Authenticator<AccessTokenCredentials> authenticator;

    /**
     * Creates a provider backed by one Access Token authenticator.
     *
     * @param authenticator the provider-independent authenticator
     */
    public LuxspecAccessTokenAuthenticationProvider(
            Authenticator<AccessTokenCredentials> authenticator
    ) {
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
    }

    @Override
    public @Nullable Authentication authenticate(
            Authentication authentication
    ) {
        if (!(Objects.requireNonNull(authentication, "authentication")
                instanceof LuxspecBearerAuthenticationToken bearerToken)) {
            return null;
        }
        try {
            com.lamprism.luxspec.security.authentication.Authentication authenticated = authenticator.authenticate(
                    new AccessTokenCredentials(bearerToken.getAccessToken())
            );
            return new LuxspecSpringAuthentication(authenticated);
        } catch (AuthenticationException exception) {
            throw new LuxspecSpringAuthenticationException(exception);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return LuxspecBearerAuthenticationToken.class.isAssignableFrom(
                Objects.requireNonNull(authentication, "authentication")
        );
    }
}
