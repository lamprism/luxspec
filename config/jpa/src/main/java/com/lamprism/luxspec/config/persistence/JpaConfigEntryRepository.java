package com.lamprism.luxspec.config.persistence;

import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Internal persistence access for configuration entries owned by one JPA source.
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
