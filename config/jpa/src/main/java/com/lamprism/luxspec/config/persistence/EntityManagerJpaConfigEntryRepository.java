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

import jakarta.persistence.EntityManager;

import java.util.Objects;
import java.util.Optional;

/**
 * EntityManager-backed Config entry repository used when an application does not provide a repository.
 *
 * <p>The repository deliberately handles only the exact source-partitioned entry operations required
 * by {@link JpaConfigSource}. It does not discover entities or inspect Java members through reflection.
 *
 * @author RollW
 */
public class EntityManagerJpaConfigEntryRepository implements JpaConfigEntryRepository {
    private final EntityManager entityManager;

    /**
     * Creates a repository over an application-managed entity manager.
     *
     * @param entityManager the entity manager used for entry operations
     */
    public EntityManagerJpaConfigEntryRepository(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public Optional<JpaConfigEntry> findById(JpaConfigEntryId id) {
        JpaConfigEntryId nonNullId = Objects.requireNonNull(id, "id");
        return Optional.ofNullable(entityManager.find(JpaConfigEntry.class, nonNullId));
    }

    @Override
    public JpaConfigEntry save(JpaConfigEntry entry) {
        JpaConfigEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
        entityManager.persist(nonNullEntry);
        return nonNullEntry;
    }

    @Override
    public void deleteById(JpaConfigEntryId id) {
        JpaConfigEntryId nonNullId = Objects.requireNonNull(id, "id");
        JpaConfigEntry entry = entityManager.find(JpaConfigEntry.class, nonNullId);
        if (entry != null) {
            entityManager.remove(entry);
        }
    }
}
