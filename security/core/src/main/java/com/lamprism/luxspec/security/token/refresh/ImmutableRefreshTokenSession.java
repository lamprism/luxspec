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

package com.lamprism.luxspec.security.token.refresh;

import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.token.SessionLifetime;

import java.util.Objects;

final class ImmutableRefreshTokenSession implements RefreshTokenSession {
    private final RefreshTokenSessionId id;
    private final Subject subject;
    private final AuthorizationGrantSet authorizationCeiling;
    private final SessionLifetime lifetime;

    ImmutableRefreshTokenSession(
            RefreshTokenSessionId id,
            Subject subject,
            AuthorizationGrantSet authorizationCeiling,
            SessionLifetime lifetime
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.subject = Objects.requireNonNull(subject, "subject");
        this.authorizationCeiling = Objects.requireNonNull(authorizationCeiling, "authorizationCeiling");
        this.lifetime = Objects.requireNonNull(lifetime, "lifetime");
    }

    @Override
    public RefreshTokenSessionId getId() {
        return id;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public AuthorizationGrantSet getAuthorizationCeiling() {
        return authorizationCeiling;
    }

    @Override
    public SessionLifetime getLifetime() {
        return lifetime;
    }
}
