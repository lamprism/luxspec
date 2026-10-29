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

package com.lamprism.luxspec.data.query;

import java.util.Objects;

/**
 * Identifies an explicitly allowed relationship from a query candidate to another query type.
 *
 * <p>The relationship name is an opaque domain key. In-memory adapters bind it to an accessor and
 * JPA adapters bind it to an entity association. Keeping the identity separate from either
 * provider prevents a query expression from carrying persistence metadata.</p>
 *
 * @param <T> the related value type
 *
 * @author RollW
 */
public final class QueryRelation<T> {
    /**
     * Describes whether a relation returns one value or a collection of values.
     */
    public enum Cardinality {
        /**
         * The relation returns at most one value.
         */
        TO_ONE,
        /**
         * The relation returns zero or more values.
         */
        TO_MANY
    }

    private final String name;
    private final Class<T> targetType;
    private final Cardinality cardinality;

    private QueryRelation(String name, Class<T> targetType, Cardinality cardinality) {
        this.name = requireName(name);
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.cardinality = Objects.requireNonNull(cardinality, "cardinality");
    }

    /**
     * Creates an explicitly allowed to-one relationship.
     *
     * @param name       the stable relationship name
     * @param targetType the related Java type
     * @param <T>        the related type
     * @return the relationship identity
     */
    public static <T> QueryRelation<T> toOne(String name, Class<T> targetType) {
        return new QueryRelation<>(name, targetType, Cardinality.TO_ONE);
    }

    /**
     * Creates an explicitly allowed to-many relationship.
     *
     * @param name       the stable relationship name
     * @param targetType the related Java type
     * @param <T>        the related type
     * @return the relationship identity
     */
    public static <T> QueryRelation<T> toMany(String name, Class<T> targetType) {
        return new QueryRelation<>(name, targetType, Cardinality.TO_MANY);
    }

    /**
     * Returns the stable relationship name.
     *
     * @return the relationship name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the related Java type.
     *
     * @return the target type
     */
    public Class<T> getTargetType() {
        return targetType;
    }

    /**
     * Returns the relationship cardinality.
     *
     * @return the cardinality
     */
    public Cardinality getCardinality() {
        return cardinality;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QueryRelation<?> relation)) {
            return false;
        }
        return name.equals(relation.name)
                && targetType.equals(relation.targetType)
                && cardinality == relation.cardinality;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, targetType, cardinality);
    }

    @Override
    public String toString() {
        return name;
    }

    private static String requireName(String name) {
        String nonNullName = Objects.requireNonNull(name, "name");
        if (nonNullName.isBlank()) {
            throw new IllegalArgumentException("Query relation name must not be blank");
        }
        for (int index = 0; index < nonNullName.length(); index++) {
            char character = nonNullName.charAt(index);
            if (Character.isWhitespace(character) || Character.isISOControl(character)) {
                throw new IllegalArgumentException("Query relation name must not contain whitespace");
            }
        }
        return nonNullName;
    }
}
