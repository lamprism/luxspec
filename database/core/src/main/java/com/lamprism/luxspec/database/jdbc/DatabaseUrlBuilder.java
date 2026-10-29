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

package com.lamprism.luxspec.database.jdbc;

import com.lamprism.luxspec.database.DatabaseConfig;
import com.lamprism.luxspec.database.DatabaseType;

/**
 * Builds JDBC connection details for one database type.
 *
 * <p>Implementations own dialect-specific URL, driver-property, and SSL policy. The builder
 * contract keeps those differences out of the resolver and allows an adapter to add another
 * database type without modifying the built-in builders.</p>
 *
 * @author RollW
 */
public interface DatabaseUrlBuilder {
    /**
     * Returns the database type handled by this builder.
     *
     * @return the supported database type
     */
    DatabaseType getDatabaseType();

    /**
     * Builds one connection detail.
     *
     * @param settings validated database settings
     * @return the JDBC connection detail
     */
    JdbcConnectionDetail build(DatabaseConfig settings);
}
