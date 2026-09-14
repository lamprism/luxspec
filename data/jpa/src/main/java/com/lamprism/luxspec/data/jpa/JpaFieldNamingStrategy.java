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

import com.lamprism.luxspec.naming.NameConverter;

import java.util.Objects;

/**
 * Adapts a provider-independent name converter to a JPA entity attribute strategy.
 *
 * @author RollW
 */
@FunctionalInterface
public interface JpaFieldNamingStrategy {
    /**
     * Creates a strategy backed by a reusable name converter.
     *
     * @param converter the query-field to entity-attribute converter
     * @return the JPA naming strategy
     */
    static JpaFieldNamingStrategy from(NameConverter converter) {
        NameConverter nonNullConverter = Objects.requireNonNull(converter, "converter");
        return nonNullConverter::convert;
    }

    /**
     * Creates an identity attribute strategy.
     *
     * @return the identity JPA naming strategy
     */
    static JpaFieldNamingStrategy identity() {
        return from(NameConverter.identity());
    }

    /**
     * Converts one query field name to a JPA attribute name.
     *
     * @param fieldName the query field name
     * @return the JPA attribute name
     */
    String toAttributeName(String fieldName);
}
