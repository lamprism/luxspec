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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Checks typed operator signatures at condition construction.
 *
 * @author RollW
 */
final class QueryOperatorArguments {
    private QueryOperatorArguments() {
    }

    static <A> List<A> validate(
            Class<?> operandType, QueryOperatorDefinition<?, A> operator, Collection<? extends A> arguments
    ) {
        QueryOperatorDefinition<?, A> definition = Objects.requireNonNull(operator, "operator");
        Class<?> expectedOperand = Objects.requireNonNull(definition.getOperandType(), "operator operand type");
        Class<A> expectedArgument = Objects.requireNonNull(definition.getArgumentType(), "operator argument type");
        if (expectedOperand.isPrimitive() || expectedArgument.isPrimitive()
                || !expectedOperand.isAssignableFrom(operandType)) {
            throw new IllegalArgumentException("Query operator operand type does not match the expression");
        }
        List<A> values = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
        int minimum = definition.getMinimumArguments();
        int maximum = definition.getMaximumArguments();
        if (minimum < 0 || maximum < minimum || values.size() < minimum || values.size() > maximum) {
            throw new IllegalArgumentException("Query operator argument count does not match its signature");
        }
        for (A value : values) {
            if (!expectedArgument.isInstance(value)) {
                throw new IllegalArgumentException("Query operator argument type does not match its signature");
            }
        }
        return values;
    }
}
