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

import java.util.List;
import java.util.Objects;

/**
 * Identifies an application-owned query function and its typed signature.
 *
 * <p>The name is a domain identity, not a database function or SQL fragment. Each query schema
 * and executor must explicitly register the definition before it can be used.</p>
 *
 * @param <T> the function result type
 * @author RollW
 */
public final class QueryFunctionDefinition<T> {
    private static final Validator<String> NAME_VALIDATOR = Validator.nonBlank("Query function name")
            .and(Validator.noWhitespace("Query function name"))
            .and(Validator.noControlCharacters("Query function name"));

    private final String name;
    private final Class<T> resultType;
    private final List<Class<?>> argumentTypes;

    private QueryFunctionDefinition(String name, Class<T> resultType, List<Class<?>> argumentTypes) {
        this.name = name;
        this.resultType = resultType;
        this.argumentTypes = argumentTypes;
    }

    /**
     * Creates a typed function identity.
     *
     * @param name          the domain-owned function name
     * @param resultType    the non-primitive result type
     * @param argumentTypes the ordered non-primitive argument types
     * @param <T>           the function result type
     * @return the immutable function definition
     */
    public static <T> QueryFunctionDefinition<T> of(String name, Class<T> resultType, Class<?>... argumentTypes) {
        String functionName = Objects.requireNonNull(name, "name");
        NAME_VALIDATOR.validate(functionName);
        Class<T> result = requireReferenceType(resultType);
        List<Class<?>> arguments = List.of(Objects.requireNonNull(argumentTypes, "argumentTypes"));
        for (Class<?> argument : arguments) {
            requireReferenceType(argument);
        }
        return new QueryFunctionDefinition<>(functionName, result, arguments);
    }

    /**
     * @return the domain function identity
     */
    public String getName() {
        return name;
    }

    /**
     * @return the result type
     */
    public Class<T> getResultType() {
        return resultType;
    }

    /**
     * @return the ordered immutable argument types
     */
    public List<Class<?>> getArgumentTypes() {
        return argumentTypes;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof QueryFunctionDefinition<?> definition)) {
            return false;
        }
        return name.equals(definition.name) && resultType.equals(definition.resultType)
                && argumentTypes.equals(definition.argumentTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, resultType, argumentTypes);
    }

    private static <V> Class<V> requireReferenceType(Class<V> type) {
        Class<V> value = Objects.requireNonNull(type, "type");
        if (value.isPrimitive()) {
            throw new IllegalArgumentException("Query function types must use boxed classes");
        }
        return value;
    }
}
