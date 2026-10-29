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

package com.lamprism.luxspec.security.token;

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.AuthenticationResolver;
import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;

import java.util.Objects;

/**
 * Applies an application-selected grant policy to the actor's current authentication.
 *
 * @author RollW
 */
public final class CurrentTokenAuthenticationPolicy implements TokenAuthenticationPolicy {
    private final AuthenticationResolver resolver;
    private final TokenGrantPolicy grantPolicy;

    /**
     * Creates a policy that intersects issued and current grants.
     *
     * @param resolver the current authentication resolver
     */
    public CurrentTokenAuthenticationPolicy(AuthenticationResolver resolver) {
        this(resolver, TokenGrantPolicy.intersection());
    }

    /**
     * Creates a policy with application-selected grant semantics.
     *
     * @param resolver    the current authentication resolver
     * @param grantPolicy the policy for issued and current grants
     */
    public CurrentTokenAuthenticationPolicy(AuthenticationResolver resolver, TokenGrantPolicy grantPolicy) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.grantPolicy = Objects.requireNonNull(grantPolicy, "grantPolicy");
    }

    @Override
    public Authentication resolve(Subject subject, AuthorizationGrantSet issuedGrants) {
        Subject nonNullSubject = Objects.requireNonNull(subject, "subject");
        AuthorizationGrantSet nonNullGrants = Objects.requireNonNull(issuedGrants, "issuedGrants");
        Authentication current = Objects.requireNonNull(resolver.resolve(nonNullSubject), "current authentication");
        if (!SimpleSubject.from(nonNullSubject).equals(SimpleSubject.from(current.subject()))) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Token subject changed");
        }
        AuthorizationGrantSet effective = Objects.requireNonNull(
                grantPolicy.apply(nonNullGrants, current.grants()),
                "effective grants"
        );
        return new Authentication(current.subject(), effective);
    }
}
