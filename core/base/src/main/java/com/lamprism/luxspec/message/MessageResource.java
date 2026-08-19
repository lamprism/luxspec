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

package com.lamprism.luxspec.message;

import java.util.Objects;

/**
 * Identifies one localized message in a stable namespace.
 *
 * <p>The namespace belongs to the message owner, not to a storage format. A message resolver can
 * map the same resource to a file, a database row, or another localized store.</p>
 *
 * @param namespace the non-blank namespace owned by the message publisher
 * @param key       the message key within the namespace
 * @author RollW
 */
public record MessageResource(String namespace, String key) {
    public MessageResource {
        namespace = requireText(namespace, "namespace");
        key = requireText(key, "key");
    }

    /**
     * Creates one message resource identifier.
     *
     * @param namespace the namespace owned by the message publisher
     * @param key       the message key within the namespace
     * @return the immutable message resource
     */
    public static MessageResource of(String namespace, String key) {
        return new MessageResource(namespace, key);
    }

    /**
     * Returns the key used by flat message providers to address this resource.
     *
     * @return the namespace and key joined with a period
     */
    public String getQualifiedKey() {
        return namespace + "." + key;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name).trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
