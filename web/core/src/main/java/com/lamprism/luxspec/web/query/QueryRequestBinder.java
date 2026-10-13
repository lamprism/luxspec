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

import com.lamprism.luxspec.data.query.QuerySchema;

import java.util.List;
import java.util.Map;

/**
 * Converts decoded HTTP query parameters into a typed Luxspec query request.
 *
 * <p>This is an extension boundary. {@link DefaultQueryRequestBinder} provides the standard
 * allowlisted binding format, while an application may implement this contract when its transport
 * naming, authorization, or query language needs a different binding policy.</p>
 *
 * @author RollW
 */
public interface QueryRequestBinder {
    /**
     * Creates the standard allowlisted binder.
     *
     * @param schema the server-owned query schema
     * @return the default binder builder
     */
    static DefaultQueryRequestBinder.Builder builder(QuerySchema schema) {
        return DefaultQueryRequestBinder.builder(schema);
    }

    /**
     * Binds decoded HTTP parameters.
     *
     * @param parameters parameter names mapped to decoded repeated values
     * @return the typed query request
     */
    QueryRequest bind(Map<String, ? extends List<String>> parameters);
}
