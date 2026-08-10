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
 * Defines one typed ordering term for a structured query.
 *
 * @param <T> the field value type
 * @author RollW
 */
public final class OrderBy<T> {
    /**
     * Identifies the ordering direction.
     */
    public enum Direction {
        /**
         * Sorts from the lowest value to the highest value.
         */
        ASCENDING,
        /**
         * Sorts from the highest value to the lowest value.
         */
        DESCENDING
    }

    private final QueryField<T> field;
    private final Direction direction;

    /**
     * Creates a typed ordering term.
     *
     * @param field     the field to order
     * @param direction the ordering direction
     */
    public OrderBy(QueryField<T> field, Direction direction) {
        this.field = Objects.requireNonNull(field, "field");
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    /**
     * Creates an ascending ordering term.
     *
     * @param field the field to order
     * @param <T>   the field value type
     * @return the ascending ordering term
     */
    public static <T> OrderBy<T> ascending(QueryField<T> field) {
        return new OrderBy<>(field, Direction.ASCENDING);
    }

    /**
     * Creates a descending ordering term.
     *
     * @param field the field to order
     * @param <T>   the field value type
     * @return the descending ordering term
     */
    public static <T> OrderBy<T> descending(QueryField<T> field) {
        return new OrderBy<>(field, Direction.DESCENDING);
    }

    /**
     * Returns the ordered field.
     *
     * @return the typed field
     */
    public QueryField<T> getField() {
        return field;
    }

    /**
     * Returns the ordering direction.
     *
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }
}
