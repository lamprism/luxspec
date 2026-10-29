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
 * Visits the typed value nodes used by a structured query predicate.
 *
 * @param <R> the visitor result type
 *
 * @author RollW
 */
public interface QueryValueVisitor<R> {
    /**
     * Visits a field value.
     *
     * @param field the referenced field
     * @param <T>   the field value type
     * @return the visitor result
     */
    <T> R visitField(QueryField<T> field);

    /**
     * Visits a non-null literal value.
     *
     * @param value the literal node
     * @param <T>   the literal type
     * @return the visitor result
     */
    <T> R visitLiteral(QueryValue.Literal<T> value);

    /**
     * Visits an allowlisted function value.
     *
     * @param value the function node
     * @param <T>   the function result type
     * @return the visitor result
     */
    <T> R visitFunction(QueryValue.Function<T> value);

    /**
     * Visits an application-owned function value.
     *
     * @param value the function node
     * @param <T>   the function result type
     * @return the visitor result
     */
    <T> R visitCustomFunction(QueryValue.CustomFunction<T> value);

    /**
     * Visits a typed cast value.
     *
     * @param value the cast node
     * @param <T>   the cast result type
     * @return the visitor result
     */
    <T> R visitCast(QueryValue.Cast<T> value);

    /**
     * Visits an aggregate value.
     *
     * @param value the aggregate node
     * @param <T>   the aggregate result type
     * @return the visitor result
     */
    <T> R visitAggregate(QueryValue.Aggregate<T> value);
}
