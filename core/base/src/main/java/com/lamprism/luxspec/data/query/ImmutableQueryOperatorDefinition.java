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

import com.lamprism.luxspec.validation.Validator;

import java.util.Objects;

/**
 * Immutable standard representation of a custom query operation signature.
 *
 * @param <T> the operand type
 * @param <A> the argument type
 * @author RollW
 */
final class ImmutableQueryOperatorDefinition<T, A> implements QueryOperatorDefinition<T, A> {
    private final String name;
    private final Class<T> operandType;
    private final Class<A> argumentType;
    private final int minimumArguments;
    private final int maximumArguments;

    ImmutableQueryOperatorDefinition(
            String name, Class<T> operandType, Class<A> argumentType, int minimumArguments, int maximumArguments
    ) {
        this.name = Objects.requireNonNull(name, "name");
        Validator.nonBlank("Query operator name")
                .and(Validator.noWhitespace("Query operator name"))
                .and(Validator.noControlCharacters("Query operator name"))
                .validate(name);
        this.operandType = requireReferenceType(operandType);
        this.argumentType = requireReferenceType(argumentType);
        if (minimumArguments < 0 || maximumArguments < minimumArguments) {
            throw new IllegalArgumentException("Invalid query operator argument count");
        }
        this.minimumArguments = minimumArguments;
        this.maximumArguments = maximumArguments;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getOperandType() {
        return operandType;
    }

    @Override
    public Class<A> getArgumentType() {
        return argumentType;
    }

    @Override
    public int getMinimumArguments() {
        return minimumArguments;
    }

    @Override
    public int getMaximumArguments() {
        return maximumArguments;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ImmutableQueryOperatorDefinition<?, ?> definition)) {
            return false;
        }
        return name.equals(definition.name) && operandType.equals(definition.operandType)
                && argumentType.equals(definition.argumentType) && minimumArguments == definition.minimumArguments
                && maximumArguments == definition.maximumArguments;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, operandType, argumentType, minimumArguments, maximumArguments);
    }

    private static <V> Class<V> requireReferenceType(Class<V> type) {
        Class<V> value = Objects.requireNonNull(type, "type");
        if (value.isPrimitive()) {
            throw new IllegalArgumentException("Query operator types must use boxed classes");
        }
        return value;
    }
}
