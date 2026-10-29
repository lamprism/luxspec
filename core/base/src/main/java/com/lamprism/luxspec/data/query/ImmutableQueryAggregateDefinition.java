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
import java.util.Objects;

/**
 * Immutable aggregate identity backed by a typed signature.
 *
 * @param <T> the aggregate result type
 * @author RollW
 */
final class ImmutableQueryAggregateDefinition<T> implements QueryAggregateDefinition<T> {
    private final QueryFunctionDefinition<T> signature;

    ImmutableQueryAggregateDefinition(QueryFunctionDefinition<T> signature) {
        this.signature = Objects.requireNonNull(signature, "signature");
    }

    @Override
    public String getName() {
        return signature.getName();
    }

    @Override
    public Class<T> getResultType() {
        return signature.getResultType();
    }

    @Override
    public List<Class<?>> getArgumentTypes() {
        return signature.getArgumentTypes();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ImmutableQueryAggregateDefinition<?> definition && signature.equals(definition.signature);
    }

    @Override
    public int hashCode() {
        return signature.hashCode();
    }
}
