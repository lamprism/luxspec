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
 * Identifies one safe value transformation supported by the query model.
 *
 * <p>The closed set prevents request data from supplying arbitrary provider or SQL function
 * names. {@link #CAST} identifies a typed cast expression even though its target type is carried
 * by the corresponding {@link QueryValue}.</p>
 *
 * @author RollW
 */
public enum QueryFunction {
    /**
     * Converts a string value to lower case using provider-defined string semantics.
     */
    LOWER,
    /**
     * Converts a string value to upper case using provider-defined string semantics.
     */
    UPPER,
    /**
     * Returns the first non-null value from a two-value expression.
     */
    COALESCE,
    /**
     * Converts a value to a declared target type.
     */
    CAST
}
