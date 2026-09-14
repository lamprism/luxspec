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

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Objects;

/**
 * Adapts one Luxspec Authentication into Spring Security's authenticated token model.
 *
 * @author RollW
 */
public final class LuxspecSpringAuthentication extends AbstractAuthenticationToken {
    /**
     * The framework-independent authentication adapted for this Spring Security token.
     */
    private final Authentication authentication;

    /**
     * Creates an authenticated Spring Security token from a Luxspec Authentication.
     *
     * @param authentication the framework-independent authentication value
     */
    public LuxspecSpringAuthentication(Authentication authentication) {
        super(authentication.grants().getScopes().stream()
                .map(AuthorizationScope::name)
                .map(SimpleGrantedAuthority::new)
                .toList());
        this.authentication = Objects.requireNonNull(authentication, "authentication");
        setAuthenticated(true);
    }

    /**
     * Returns the underlying framework-independent authentication value.
     *
     * @return the Luxspec authentication
     */
    public Authentication getLuxspecAuthentication() {
        return authentication;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return authentication.subject();
    }

    @Override
    public String getName() {
        return authentication.subject().getId();
    }
}
