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
 * Defines the types and arity of an application-owned query operation.
 *
 * <p>The name is a domain identity, never an SQL fragment. Implementations must be immutable
 * and use stable equality and hash codes while registered with schemas and executors.</p>
 *
 * @param <T> the operated value type
 * @param <A> the type of each argument
 * @author RollW
 */
public interface QueryOperatorDefinition<T, A> extends QueryOperator {
    /**
     * Creates an immutable operation identity and signature.
     *
     * @param name             the domain operation name
     * @param operandType      the non-primitive operand type
     * @param argumentType     the non-primitive argument type
     * @param minimumArguments the minimum accepted argument count
     * @param maximumArguments the maximum accepted argument count
     * @param <T>              the operand type
     * @param <A>              the argument type
     * @return the operation definition
     */
    static <T, A> QueryOperatorDefinition<T, A> of(
            String name, Class<T> operandType, Class<A> argumentType, int minimumArguments, int maximumArguments
    ) {
        return new ImmutableQueryOperatorDefinition<>(name, operandType, argumentType, minimumArguments, maximumArguments);
    }

    /**
     * @return the domain operation name
     */
    String getName();

    /**
     * @return the operand type
     */
    Class<T> getOperandType();

    /**
     * @return the type of each argument
     */
    Class<A> getArgumentType();

    /**
     * @return the minimum accepted argument count
     */
    int getMinimumArguments();

    /**
     * @return the maximum accepted argument count
     */
    int getMaximumArguments();
}
