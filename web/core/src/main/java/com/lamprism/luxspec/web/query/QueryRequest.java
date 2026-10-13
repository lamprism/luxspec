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

package com.lamprism.luxspec.web.query;

import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.QueryCriteria;

import java.util.Objects;

/**
 * A validated HTTP query mapped to the provider-independent query model.
 *
 * @author RollW
 */
public final class QueryRequest {
    private final QueryCriteria criteria;
    private final QueryWindow window;

    /**
     * Creates a validated query request.
     *
     * @param criteria the typed query criteria
     * @param window   the bounded result window
     */
    public QueryRequest(QueryCriteria criteria, QueryWindow window) {
        this.criteria = Objects.requireNonNull(criteria, "criteria");
        this.window = Objects.requireNonNull(window, "window");
    }

    /**
     * Returns the typed criteria.
     *
     * @return the criteria
     */
    public QueryCriteria getCriteria() {
        return criteria;
    }

    /**
     * Returns the bounded window.
     *
     * @return the window
     */
    public QueryWindow getWindow() {
        return window;
    }
}
