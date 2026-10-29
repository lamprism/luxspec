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

package com.lamprism.luxspec.user.security.authentication;

import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.AuthenticationResolver;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.resource.UserProvider;
import com.lamprism.luxspec.user.security.authorization.UserRoleGrantResolver;

import java.util.Objects;

/**
 * Resolves account state and current role grants from one user snapshot.
 *
 * @author RollW
 */
public final class UserAuthenticationResolver implements AuthenticationResolver {
    private final UserSubjectResolver subjectResolver;
    private final UserRoleGrantResolver grantResolver;

    /**
     * Creates a current-user authentication resolver.
     *
     * @param userProvider  the authoritative user lookup
     * @param grantResolver the role-to-grants resolver
     */
    public UserAuthenticationResolver(UserProvider userProvider, UserRoleGrantResolver grantResolver) {
        this.subjectResolver = new UserSubjectResolver(userProvider);
        this.grantResolver = Objects.requireNonNull(grantResolver, "grantResolver");
    }

    @Override
    public Authentication resolve(Subject subject) {
        Subject nonNullSubject = Objects.requireNonNull(subject, "subject");
        User user = subjectResolver.resolveUser(nonNullSubject.getType(), nonNullSubject.getId());
        UserSubject currentSubject = UserSubjectResolver.requireActive(user);
        return new Authentication(currentSubject, grantResolver.resolve(user.roles()));
    }
}
