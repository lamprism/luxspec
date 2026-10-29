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

/**
 * Identifies a structured query operation.
 *
 * <p>Standard operators are provided as constants. Applications may define other typed
 * operations through {@link QueryOperatorDefinition} and register their implementations with
 * each query executor.</p>
 *
 * @author RollW
 */
public interface QueryOperator {
    /**
     * Matches equal values.
     */
    Standard EQUAL = Standard.EQUAL;
    /**
     * Matches non-equal values.
     */
    Standard NOT_EQUAL = Standard.NOT_EQUAL;
    /**
     * Matches values above a boundary.
     */
    Standard GREATER_THAN = Standard.GREATER_THAN;
    /**
     * Matches values at or above a boundary.
     */
    Standard GREATER_THAN_OR_EQUAL = Standard.GREATER_THAN_OR_EQUAL;
    /**
     * Matches values below a boundary.
     */
    Standard LESS_THAN = Standard.LESS_THAN;
    /**
     * Matches values at or below a boundary.
     */
    Standard LESS_THAN_OR_EQUAL = Standard.LESS_THAN_OR_EQUAL;
    /**
     * Matches a string pattern.
     */
    Standard LIKE = Standard.LIKE;
    /**
     * Matches values in a collection.
     */
    Standard IN = Standard.IN;
    /**
     * Matches values outside a collection.
     */
    Standard NOT_IN = Standard.NOT_IN;
    /**
     * Matches null values.
     */
    Standard IS_NULL = Standard.IS_NULL;
    /**
     * Matches non-null values.
     */
    Standard IS_NOT_NULL = Standard.IS_NOT_NULL;

    /**
     * Standard operations understood by the built-in adapters.
     */
    enum Standard implements QueryOperator {
        /**
         * Matches equal values.
         */
        EQUAL,
        /**
         * Matches non-equal values.
         */
        NOT_EQUAL,
        /**
         * Matches values above a boundary.
         */
        GREATER_THAN,
        /**
         * Matches values at or above a boundary.
         */
        GREATER_THAN_OR_EQUAL,
        /**
         * Matches values below a boundary.
         */
        LESS_THAN,
        /**
         * Matches values at or below a boundary.
         */
        LESS_THAN_OR_EQUAL,
        /**
         * Matches a string pattern.
         */
        LIKE,
        /**
         * Matches values in a collection.
         */
        IN,
        /**
         * Matches values outside a collection.
         */
        NOT_IN,
        /**
         * Matches null values.
         */
        IS_NULL,
        /**
         * Matches non-null values.
         */
        IS_NOT_NULL
    }
}
