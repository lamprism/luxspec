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

package com.lamprism.luxspec.security.authorization;

import com.lamprism.luxspec.resource.ResourceType;

import java.util.Objects;

/**
 * Identifies a resource operation and its baseline authorization requirement.
 *
 * @param <ID> the resource ID type
 * @author RollW
 */
public final class ResourceAction<ID> {
    private final ResourceType<ID> resourceType;
    private final String name;
    private final AuthorizationRequirement requirement;

    private ResourceAction(ResourceType<ID> resourceType, String name, AuthorizationRequirement requirement) {
        this.resourceType = resourceType;
        this.name = name;
        this.requirement = requirement;
    }

    /**
     * Creates a resource action with a canonical name and baseline requirement.
     *
     * @param resourceType the handled resource type
     * @param name         the action name in accepted input form
     * @param requirement  the baseline authorization requirement
     * @param <ID>         the resource identifier type
     * @return the immutable resource action
     */
    public static <ID> ResourceAction<ID> of(
            ResourceType<ID> resourceType,
            String name,
            AuthorizationRequirement requirement
    ) {
        return new ResourceAction<>(
                Objects.requireNonNull(resourceType, "resourceType"),
                normalize(name),
                Objects.requireNonNull(requirement, "requirement")
        );
    }

    /**
     * Returns the resource type handled by this action.
     *
     * @return the handled resource type
     */
    public ResourceType<ID> getResourceType() {
        return resourceType;
    }

    /**
     * Returns the canonical action name.
     *
     * @return the action name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the baseline authorization requirement.
     *
     * @return the authorization requirement
     */
    public AuthorizationRequirement getRequirement() {
        return requirement;
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "name");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Action name must not be empty");
        }
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = true;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '-' || character == '_' || Character.isWhitespace(character)) {
                if (!previousSeparator) {
                    result.append('_');
                    previousSeparator = true;
                }
                continue;
            }
            if ((character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')) {
                result.append(Character.toUpperCase(character));
                previousSeparator = false;
                continue;
            }
            if (character >= '0' && character <= '9') {
                result.append(character);
                previousSeparator = false;
                continue;
            }
            throw new IllegalArgumentException("Action name contains an unsupported character");
        }
        if (result.isEmpty() || result.charAt(0) == '_' || result.charAt(result.length() - 1) == '_') {
            throw new IllegalArgumentException("Action name contains an empty segment");
        }
        return result.toString();
    }
}
