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

/**
 * Provides an optional JPA adapter for source-partitioned configuration storage.
 *
 * <p>Each source-local configuration key is stored in one row. Scalar and list values are kept in a
 * structured payload, while tombstones clear the payload columns. The package never enables entity
 * scanning, Hibernate DDL, or database migrations. Applications explicitly include {@code
 * META-INF/luxspec/liquibase/config/changelog.yaml} from their own Liquibase migration master before
 * using the mapping.</p>
 */
@NullMarked
package com.lamprism.luxspec.config.persistence;

import org.jspecify.annotations.NullMarked;
