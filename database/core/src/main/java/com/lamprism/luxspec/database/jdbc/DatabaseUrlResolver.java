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

/**
 * Resolves provider-neutral database settings into JDBC connection details.
 *
 * @author RollW
 */
public interface DatabaseUrlResolver {
    /**
     * Resolves one validated database configuration.
     *
     * @param settings the startup database settings
     * @return JDBC URL, driver identity, and driver properties
     */
    JdbcConnectionDetail resolve(DatabaseConfig settings);
}
