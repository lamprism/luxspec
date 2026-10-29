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

package com.lamprism.luxspec.config.persistence;

import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Internal persistence access for configuration entries owned by one JPA source.
 *
 * @author RollW
 */
public interface JpaConfigEntryRepository extends Repository<JpaConfigEntry, JpaConfigEntryId> {
    /**
     * Finds one entry by its source-partitioned configuration identity.
     *
     * @param id the source-partitioned entry identity
     * @return the optional persisted entry
     */
    Optional<JpaConfigEntry> findById(JpaConfigEntryId id);

    /**
     * Persists an added configuration entry.
     *
     * @param entry the entry to persist
     * @return the persisted entry
     */
    JpaConfigEntry save(JpaConfigEntry entry);

    /**
     * Deletes one entry by its complete configuration key.
     *
     * @param id the source-partitioned entry identity
     */
    void deleteById(JpaConfigEntryId id);
}
