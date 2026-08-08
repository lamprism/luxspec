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

package com.lamprism.luxspec.data.jpa;

import com.lamprism.luxspec.data.query.QueryField;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import java.util.Objects;

final class DefaultJpaFieldResolver<T> implements JpaFieldResolver<T> {
    private final JpaFieldNamingStrategy namingStrategy;

    DefaultJpaFieldResolver(JpaFieldNamingStrategy namingStrategy) {
        this.namingStrategy = Objects.requireNonNull(namingStrategy, "namingStrategy");
    }

    @Override
    public <V> Expression<V> resolve(Root<T> root, QueryField<V> field) {
        Root<T> nonNullRoot = Objects.requireNonNull(root, "root");
        QueryField<V> nonNullField = Objects.requireNonNull(field, "field");
        String attributeName = Objects.requireNonNull(
                namingStrategy.toAttributeName(nonNullField.getName()),
                "naming strategy result"
        );
        if (attributeName.isEmpty()) {
            throw new IllegalArgumentException("Naming strategy must return a non-empty attribute name");
        }
        return nonNullRoot.<V>get(attributeName);
    }
}
