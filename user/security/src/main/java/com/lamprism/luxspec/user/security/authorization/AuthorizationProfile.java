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
import java.util.Objects;
import java.util.Set;

/**
 * Identifies one assembly-only grouping of user roles and concrete authorization scopes.
 *
 * @author RollW
 */
public final class AuthorizationProfile {
    /**
     * Contains scopes granted to ordinary users.
     */
    public static final AuthorizationProfile USER = AuthorizationProfile.of("user");
    /**
     * Contains scopes granted only to administrators in addition to user scopes.
     */
    public static final AuthorizationProfile ADMIN = AuthorizationProfile.of("admin");

    private final String name;

    private AuthorizationProfile(String name) {
        this.name = name;
    }

    /**
     * Creates one canonical lower-kebab authorization profile identity.
     *
     * @param name the canonical profile name
     * @return the authorization profile
     */
    public static AuthorizationProfile of(String name) {
        return new AuthorizationProfile(requireCanonical(name));
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

    /**
     * Returns the canonical profile name.
     *
     * @return the profile name
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AuthorizationProfile profile)) {
            return false;
        }
        return name.equals(profile.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    private static String requireCanonical(String value) {
        String name = Objects.requireNonNull(value, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Profile name must not be empty");
        }
        boolean segmentStart = true;
        boolean previousHyphen = false;
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (character == '-') {
                if (segmentStart || previousHyphen) {
                    throw new IllegalArgumentException("Profile name contains an invalid segment");
                }
                previousHyphen = true;
                continue;
            }
            if (segmentStart) {
                if (character < 'a' || character > 'z') {
                    throw new IllegalArgumentException("Profile name must start with a lowercase letter");
                }
                segmentStart = false;
                continue;
            }
            if ((character < 'a' || character > 'z') && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("Profile name contains an unsupported character");
            }
            previousHyphen = false;
        }
        if (previousHyphen) {
            throw new IllegalArgumentException("Profile name must not end with a hyphen");
        }
        return name;
    }
}
