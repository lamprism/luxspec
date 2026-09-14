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

package com.lamprism.luxspec.audit.query;

import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.QueryCriteria;

/**
 * Provider-independent audit query role.
 *
 * <p>The reader reuses the shared query criteria and result-window contracts.
 * It is a read capability, not an authorization boundary; host applications
 * own authorization and tenant restrictions.
 *
 * @author RollW
 */
public interface AuditReader {
    /**
     * Reads audit entries matching validated provider-independent criteria.
     *
     * <p>The implementation must apply its supported audit query schema and
     * complexity limits before executing the query.</p>
     *
     * @param criteria the structured audit filters and ordering
     * @param window   the requested result window
     * @return the matching complete, page, or slice result
     */
    QueryResult<AuditEntry> browse(QueryCriteria criteria, QueryWindow window);
}
