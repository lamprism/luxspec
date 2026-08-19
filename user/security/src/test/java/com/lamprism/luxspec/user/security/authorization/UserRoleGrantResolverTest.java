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

package com.lamprism.luxspec.user.security.authorization;

import com.lamprism.luxspec.security.authorization.AuthorizationScope;
import com.lamprism.luxspec.security.authorization.AuthorizationScopeHierarchy;
import com.lamprism.luxspec.user.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRoleGrantResolverTest {
    @Test
    void administratorInheritsUserProfileScopesAndFeatureContributions() {
        AuthorizationScope userRead = AuthorizationScope.of("resource:user:read");
        AuthorizationScope userWrite = AuthorizationScope.of("resource:user:write");
        AuthorizationScopeHierarchy hierarchy = AuthorizationScopeHierarchy.builder()
                .declare(userRead)
                .declare(userWrite)
                .build();
        AuthorizationProfileContributor userScopes = registry -> {
            registry.grant(UserAuthorizationProfiles.USER, List.of(userRead));
            registry.grant(UserAuthorizationProfiles.ADMIN, List.of(userWrite));
        };
        UserRoleGrantResolver resolver = new UserRoleGrantResolverImpl(
                UserAuthorizationProfiles.defaultRoleProfiles(),
                List.of(UserAuthorizationProfiles.defaults(), userScopes),
                hierarchy
        );

        assertEquals(
                List.of(userRead),
                resolver.resolve(List.of(Role.USER)).getScopes().stream().sorted((left, right) -> left.name().compareTo(right.name())).toList()
        );
        assertEquals(
                List.of(userRead, userWrite),
                resolver.resolve(List.of(Role.ADMIN)).getScopes().stream().sorted((left, right) -> left.name().compareTo(right.name())).toList()
        );
    }

    @Test
    void rejectsProfileInheritanceCyclesDuringAssembly() {
        AuthorizationProfile first = AuthorizationProfile.of("first");
        AuthorizationProfile second = AuthorizationProfile.of("second");
        AuthorizationProfileContributor cyclic = registry -> {
            registry.register(first);
            registry.register(second);
            registry.inherit(first, second);
            registry.inherit(second, first);
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> new UserRoleGrantResolverImpl(
                        Map.of(),
                        List.of(cyclic),
                        AuthorizationScopeHierarchy.empty()
                )
        );
    }
}
