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

import com.lamprism.luxspec.user.Role;

import java.util.Map;
import java.util.Set;

/**
 * Defines the stable profiles attached to predefined generic user roles.
 *
 * @author RollW
 */
public final class UserAuthorizationProfiles {
    /**
     * Contains scopes granted to ordinary users.
     */
    public static final AuthorizationProfile USER = AuthorizationProfile.of("user");
    /**
     * Contains scopes granted only to administrators in addition to user scopes.
     */
    public static final AuthorizationProfile ADMIN = AuthorizationProfile.of("admin");

    private UserAuthorizationProfiles() {
    }

    /**
     * Returns the default Role-to-Profile mapping without listing concrete feature scopes.
     *
     * @return immutable predefined role profile mappings
     */
    public static Map<Role, Set<AuthorizationProfile>> defaultRoleProfiles() {
        return Map.of(
                Role.USER, Set.of(USER),
                Role.ADMIN, Set.of(ADMIN)
        );
    }

    /**
     * Creates the default profile inheritance contribution.
     *
     * @return the default profile contributor
     */
    public static AuthorizationProfileContributor defaults() {
        return registry -> {
            registry.register(USER);
            registry.register(ADMIN);
            registry.inherit(ADMIN, USER);
        };
    }
}
