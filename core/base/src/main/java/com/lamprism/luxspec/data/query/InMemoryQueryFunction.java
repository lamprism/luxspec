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

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Evaluates a registered query function over in-memory argument values.
 *
 * @param <T> the function result type
 * @author RollW
 */
@FunctionalInterface
public interface InMemoryQueryFunction<T> {
    /**
     * Evaluates the ordered values using application-defined null semantics.
     *
     * @param arguments the immutable argument values, which may contain null
     * @return the result, which may be null
     */
    @Nullable T evaluate(List<@Nullable Object> arguments);
}
