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
import com.lamprism.luxspec.security.authentication.AuthenticationException;
import com.lamprism.luxspec.security.authentication.UserSubject;
import com.lamprism.luxspec.security.authorization.AuthorizationGrantSet;
import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.UserStatus;
import com.lamprism.luxspec.user.resource.InMemoryUserStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class UserAuthenticationResolverTest {
    @Test
    void resolvesCurrentRolesAndRejectsInactiveAccounts() {
        InMemoryUserStore users = new InMemoryUserStore();
        User user = users.register("alice", null, Set.of(Role.USER));
        AuthorizationGrantSet currentGrants = AuthorizationGrantSet.of(List.of(AuthorizationScope.of("role:user")));
        UserAuthenticationResolver resolver = new UserAuthenticationResolver(users, roles -> currentGrants);
        UserSubject subject = new UserSubject(user.id());

        assertEquals(currentGrants, resolver.resolve(subject).grants());
        users.changeStatus(user.id(), UserStatus.DISABLED);
        AuthenticationException failure = assertThrows(AuthenticationException.class, () -> resolver.resolve(subject));
        assertEquals(AuthErrorCode.SUBJECT_DISABLED, failure.getErrorCode());
    }
}
