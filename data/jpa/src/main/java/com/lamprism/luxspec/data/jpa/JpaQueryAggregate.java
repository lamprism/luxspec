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
import jakarta.persistence.criteria.From;

import java.util.List;

/**
 * Translates an application-defined aggregate to a JPA Criteria expression.
 *
 * <p>Implementations must agree with any equivalent in-memory aggregate, including null and
 * empty-group semantics. They must be safe for concurrent use when the translator is shared.</p>
 *
 * @param <T> the aggregate result type
 * @author RollW
 */
@FunctionalInterface
public interface JpaQueryAggregate<T> {
    /**
     * Translates a group aggregate.
     *
     * @param builder   the Criteria builder
     * @param source    the query source for row-only aggregates
     * @param arguments the ordered immutable Criteria operand expressions
     * @return the aggregate expression
     */
    Expression<T> translate(CriteriaBuilder builder, From<?, ?> source, List<Expression<?>> arguments);
}
