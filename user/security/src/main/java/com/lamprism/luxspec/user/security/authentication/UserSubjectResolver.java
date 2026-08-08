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

import com.lamprism.luxspec.AuthErrorCode;
import com.lamprism.luxspec.resource.ResourceException;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.Subject;
import com.lamprism.luxspec.security.authentication.SubjectResolver;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;
import com.lamprism.luxspec.user.resource.UserProvider;
import com.lamprism.luxspec.user.resource.UserResourceTypes;

import java.util.Objects;

/**
 * Resolves current user accounts for access-token authentication.
 *
 * @author RollW
 */
public final class UserSubjectResolver implements SubjectResolver {
    private final UserProvider userProvider;

    /**
     * Creates a user-subject resolver using the generic user lookup role.
     *
     * @param userProvider the authoritative user lookup role
     */
    public UserSubjectResolver(UserProvider userProvider) {
        this.userProvider = Objects.requireNonNull(userProvider, "userProvider");
    }

    @Override
    public Subject resolve(String subjectType, String subjectId) {
        if (!"user".equals(subjectType)) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Access token subject type is unsupported");
        }
        long userId = parseUserId(subjectId);
        User user = findUser(userId);
        return requireActive(user);
    }

    private long parseUserId(String value) {
        try {
            long userId = Long.parseLong(Objects.requireNonNull(value, "subjectId"));
            if (userId < 1L) {
                throw new NumberFormatException("User ID must be positive");
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new AuthenticationException(AuthErrorCode.INVALID_TOKEN, "Access token subject ID is invalid", exception);
        }
    }

    private User findUser(long userId) {
        try {
            return userProvider.provide(new ResourceReference<>(UserResourceTypes.USER, userId));
        } catch (ResourceException exception) {
            throw new AuthenticationException(AuthErrorCode.SUBJECT_NOT_FOUND, "User subject was not found", exception);
        }
    }

    static UserSubject requireActive(User user) {
        User nonNullUser = Objects.requireNonNull(user, "user");
        UserStatus status = nonNullUser.status();
        if (status == UserStatus.ACTIVE) {
            return new UserSubject(nonNullUser.id());
        }
        if (status == UserStatus.DISABLED) {
            throw new AuthenticationException(AuthErrorCode.SUBJECT_DISABLED, "User subject is disabled");
        }
        if (status == UserStatus.LOCKED) {
            throw new AuthenticationException(AuthErrorCode.SUBJECT_LOCKED, "User subject is locked");
        }
        throw new AuthenticationException(AuthErrorCode.SUBJECT_CANCELED, "User subject is canceled");
    }
}
