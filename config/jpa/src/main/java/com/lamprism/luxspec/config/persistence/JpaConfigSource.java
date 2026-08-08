package com.lamprism.luxspec.config.persistence;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.config.source.TombstoneConfigSource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

/**
 * Stores provider-neutral configuration entries through one JPA repository.
 *
 * @author RollW
 */
public class JpaConfigSource implements TombstoneConfigSource {
    private final ConfigSourceId id;
    private final JpaConfigEntryRepository repository;
    private final Clock clock;

    /**
     * Creates one database-backed source with an explicit source instance ID.
     *
     * @param id         the configured source instance ID
     * @param repository the internal entry repository
     * @param clock      the mutation time source
     */
    public JpaConfigSource(ConfigSourceId id, JpaConfigEntryRepository repository, Clock clock) {
        this.id = Objects.requireNonNull(id, "id");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public ConfigSourceId getId() {
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigEntry get(ConfigKey key) {
        ConfigKey nonNullKey = Objects.requireNonNull(key, "key");
        return repository.findById(JpaConfigEntryId.of(id, nonNullKey))
                .map(JpaConfigEntry::toConfigEntry)
                .orElseGet(ConfigEntry::absent);
    }

    @Override
    @Transactional
    public void set(ConfigKey key, RawConfigValue rawValue) {
        ConfigKey nonNullKey = Objects.requireNonNull(key, "key");
        RawConfigValue nonNullRawValue = Objects.requireNonNull(rawValue, "rawValue");
        JpaConfigEntryId entryId = JpaConfigEntryId.of(id, nonNullKey);
        JpaConfigEntry entry = repository.findById(entryId).orElse(null);
        if (entry == null) {
            repository.save(JpaConfigEntry.create(id, nonNullKey, nonNullRawValue, clock.instant()));
            return;
        }
        entry.replace(nonNullRawValue, clock.instant());
    }

    @Override
    @Transactional
    public void remove(ConfigKey key) {
        ConfigKey nonNullKey = Objects.requireNonNull(key, "key");
        repository.deleteById(JpaConfigEntryId.of(id, nonNullKey));
    }

    @Override
    @Transactional
    public void writeTombstone(ConfigKey key) {
        ConfigKey nonNullKey = Objects.requireNonNull(key, "key");
        JpaConfigEntryId entryId = JpaConfigEntryId.of(id, nonNullKey);
        JpaConfigEntry entry = repository.findById(entryId).orElse(null);
        if (entry == null) {
            repository.save(JpaConfigEntry.createTombstone(id, nonNullKey, clock.instant()));
            return;
        }
        entry.writeTombstone(clock.instant());
    }
}
