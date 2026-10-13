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
 * The closed set of aggregate operations supported by provider adapters.
 *
 * @author RollW
 */
public enum QueryAggregateFunction {
    /**
     * Counts rows or non-null values.
     */
    COUNT,
    /**
     * Counts distinct non-null values.
     */
    COUNT_DISTINCT,
    /**
     * Sums numeric values.
     */
    SUM,
    /**
     * Calculates the arithmetic average of numeric values.
     */
    AVERAGE,
    /**
     * Selects the lowest comparable value.
     */
    MIN,
    /**
     * Selects the highest comparable value.
     */
    MAX
}
