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

package com.lamprism.luxspec.user.query;

import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.resource.ResourceBrowser;
import com.lamprism.luxspec.user.User;

/**
 * Browses users through ordinary typed structured queries.
 *
 * @author RollW
 */
public interface UserBrowser extends ResourceBrowser<Long> {
    /**
     * Returns a user query result for validated criteria and an allowed window.
     *
     * @param criteria the typed user filters and ordering
     * @param window   the allowed result window
     * @return the user query result
     */
    @Override
    QueryResult<User> browse(QueryCriteria criteria, QueryWindow window);
}
