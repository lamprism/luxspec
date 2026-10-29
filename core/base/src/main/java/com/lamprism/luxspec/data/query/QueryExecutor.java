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

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;

/**
 * Executes provider-neutral query criteria and plans against one owned data source.
 *
 * @param <T> the query result item type
 *
 * @author RollW
 */
public interface QueryExecutor<T> {
    /**
     * Executes criteria using the requested result window.
     *
     * <p>Callers must apply the role-specific {@link QuerySchema} and
     * {@link QueryComplexityBudget} before execution.</p>
     *
     * @param criteria the structured filters and ordering
     * @param window   the requested result window
     * @return the complete, page, or slice result
     */
    QueryResult<T> query(QueryCriteria criteria, QueryWindow window);

    /**
     * Executes one projection, grouping, aggregate, or time-series plan.
     *
     * <p>Providers that only support ordinary criteria may keep the default unsupported
     * behavior. Providers exposing the advanced query model override this method on the same
     * executor contract.</p>
     *
     * @param plan   the validated typed plan
     * @param window the requested result window
     * @param <R>    the plan result type
     * @return the plan query result
     * @throws UnsupportedOperationException when the provider does not support query plans
     */
    default <R> QueryResult<R> query(QueryPlan<R> plan, QueryWindow window) {
        throw new UnsupportedOperationException("Query plans are not supported");
    }
}
