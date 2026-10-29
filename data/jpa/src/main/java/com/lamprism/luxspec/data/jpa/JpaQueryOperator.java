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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.List;

/**
 * Translates an application-defined query operator into a JPA predicate.
 *
 * <p>The implementation owns null semantics and must agree with any equivalent in-memory
 * evaluator. Its predicate must evaluate to true or false for null operands rather than SQL
 * unknown, so negation has the same meaning in both execution models. Implementations must be
 * safe for concurrent use when the translator is shared.</p>
 *
 * @param <T> the operand type
 * @param <A> the argument type
 * @author RollW
 */
@FunctionalInterface
public interface JpaQueryOperator<T, A> {
    /**
     * Translates one operation using the provider's Criteria API.
     *
     * @param builder   the Criteria builder
     * @param operand   the already translated operand
     * @param arguments the immutable typed argument values
     * @return the predicate
     */
    Predicate translate(CriteriaBuilder builder, Expression<T> operand, List<A> arguments);
}
