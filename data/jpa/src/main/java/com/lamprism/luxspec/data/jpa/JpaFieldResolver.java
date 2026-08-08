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

/**
 * Resolves explicitly allowed typed query fields to JPA criteria expressions.
 *
 * @param <T> the persistence entity type
 * @author RollW
 */
public interface JpaFieldResolver<T> {
    /**
     * Creates a resolver that maps each query field name to a direct entity attribute with the same name.
     *
     * <p>The resolver delegates attribute lookup to the JPA Criteria provider through
     * {@link Root#get(String)}. It does not inspect entity fields or methods through Java reflection.
     *
     * @param <T> the persistence entity type
     * @return a resolver for direct same-name attributes
     */
    static <T> JpaFieldResolver<T> byAttributeName() {
        return byAttributeName(JpaFieldNamingStrategy.identity());
    }

    /**
     * Creates a resolver that converts each query field name before direct JPA attribute lookup.
     *
     * @param namingStrategy the query-to-attribute naming strategy
     * @param <T>            the persistence entity type
     * @return a resolver for direct attributes using the supplied naming strategy
     */
    static <T> JpaFieldResolver<T> byAttributeName(JpaFieldNamingStrategy namingStrategy) {
        return new DefaultJpaFieldResolver<>(Objects.requireNonNull(namingStrategy, "namingStrategy"));
    }

    /**
     * Resolves one application-defined typed field for an entity root.
     *
     * @param root  the query entity root
     * @param field the explicit allowed typed field
     * @param <V>   the field value type
     * @return the JPA expression for that typed field
     */
    <V> Expression<V> resolve(Root<T> root, QueryField<V> field);
}
