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
 * Reduces application-defined aggregate operands in memory.
 *
 * <p>Each row contains the ordered operand values, including nulls. Implementations choose null
 * and empty-group semantics and must agree with equivalent provider adapters. They must be safe
 * for concurrent use when the executor is shared.</p>
 *
 * @param <T> the aggregate result type
 * @author RollW
 */
@FunctionalInterface
public interface InMemoryQueryAggregate<T> {
    /**
     * Evaluates one group.
     *
     * @param rows immutable rows containing immutable ordered operand values; row order is unspecified
     * @return the aggregate value, or null when the aggregate has no value
     */
    @Nullable T evaluate(List<List<@Nullable Object>> rows);
}
