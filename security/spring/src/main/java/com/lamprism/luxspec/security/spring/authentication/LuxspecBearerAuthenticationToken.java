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

import com.lamprism.luxspec.security.token.access.AccessToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;
import java.util.Objects;

/**
 * Carries one Bearer Access Token as an unauthenticated Spring Security request.
 *
 * <p>The token is exposed only through the typed accessor. Credentials and string output remain
 * redacted so framework diagnostics cannot accidentally log the raw value.</p>
 *
 * @author RollW
 */
public final class LuxspecBearerAuthenticationToken extends AbstractAuthenticationToken {
    private final AccessToken accessToken;

    /**
     * Creates an unauthenticated Bearer request token.
     *
     * @param accessToken the presented opaque Access Token
     */
    public LuxspecBearerAuthenticationToken(AccessToken accessToken) {
        super(List.of());
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
    }

    /**
     * Returns the presented Access Token for the authentication provider.
     *
     * @return the Access Token
     */
    public AccessToken getAccessToken() {
        return accessToken;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return "access-token";
    }

    @Override
    public String getName() {
        return "access-token";
    }

    @Override
    public String toString() {
        return "LuxspecBearerAuthenticationToken[authenticated=" + isAuthenticated() + "]";
    }
}
