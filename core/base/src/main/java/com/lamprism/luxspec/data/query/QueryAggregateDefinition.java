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

import java.util.List;

/**
 * Identifies an application-owned aggregate and its ordered typed operands.
 *
 * <p>The name is a domain identity, not a database function name or SQL fragment. Implementations
 * must be immutable and have stable equality and hash codes while registered.</p>
 *
 * @param <T> the aggregate result type
 * @author RollW
 */
public interface QueryAggregateDefinition<T> extends QueryAggregateFunction {
    /**
     * Creates an immutable aggregate definition.
     *
     * @param name          the domain identity
     * @param resultType    the non-primitive result type
     * @param argumentTypes the ordered non-primitive operand types
     * @param <T>           the aggregate result type
     * @return the aggregate definition
     */
    static <T> QueryAggregateDefinition<T> of(String name, Class<T> resultType, Class<?>... argumentTypes) {
        return new ImmutableQueryAggregateDefinition<>(QueryFunctionDefinition.of(name, resultType, argumentTypes));
    }

    /**
     * @return the domain identity
     */
    String getName();

    /**
     * @return the aggregate result type
     */
    Class<T> getResultType();

    /**
     * @return the ordered immutable operand types
     */
    List<Class<?>> getArgumentTypes();
}
