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

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;

import java.util.Objects;

/**
 * Reconstructs an authentication using a verified identity and its issued grant ceiling.
 *
 * @author RollW
 */
@FunctionalInterface
public interface TokenAuthenticationPolicy {
    /**
     * Uses the grants from the token while resolving the subject's current state.
     *
     * @param resolver the subject and account-state resolver
     * @return the issued-grants policy
     */
    static TokenAuthenticationPolicy issuedGrants(SubjectResolver resolver) {
        SubjectResolver nonNullResolver = Objects.requireNonNull(resolver, "resolver");
        return (subject, grants) -> new Authentication(
                nonNullResolver.resolve(subject.getType(), subject.getId()),
                grants
        );
    }

    /**
     * Resolves an authentication for verified token or session data.
     *
     * @param subject      the verified subject identity
     * @param issuedGrants the grants recorded in the token or session
     * @return the effective authentication
     */
    Authentication resolve(Subject subject, AuthorizationGrantSet issuedGrants);
}
