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

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.RawConfigValue;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Internal JPA mapping for one scalar, list, or tombstone configuration entry.
 *
 * <p>The owning application applies this package's Liquibase changelog before using the mapping.</p>
 *
 * @author RollW
 */
@Entity
@Table(name = "luxspec_config_entry")
public class JpaConfigEntry {
    @EmbeddedId
    private JpaConfigEntryId id;

    @Column(name = "tombstone", nullable = false)
    private boolean tombstone;

    @Lob
    @Column(name = "payload")
    @Convert(converter = JpaConfigValueAttributeConverter.class)
    @Nullable
    private RawConfigValue payload;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    /**
     * Creates an empty entry for JPA materialization.
     */
    protected JpaConfigEntry() {
    }

    static JpaConfigEntry create(
            ConfigSourceId sourceId,
            ConfigKey key,
            RawConfigValue rawValue,
            Instant updatedAt
    ) {
        JpaConfigEntry entry = new JpaConfigEntry();
        entry.id = JpaConfigEntryId.of(sourceId, key);
        entry.replace(rawValue, updatedAt);
        return entry;
    }

    static JpaConfigEntry createTombstone(ConfigSourceId sourceId, ConfigKey key, Instant updatedAt) {
        JpaConfigEntry entry = new JpaConfigEntry();
        entry.id = JpaConfigEntryId.of(sourceId, key);
        entry.writeTombstone(updatedAt);
        return entry;
    }

    void replace(RawConfigValue rawValue, Instant updatedAt) {
        RawConfigValue nonNullRawValue = Objects.requireNonNull(rawValue, "rawValue");
        this.tombstone = false;
        this.payload = nonNullRawValue;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    void writeTombstone(Instant updatedAt) {
        this.tombstone = true;
        this.payload = null;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    ConfigEntry toConfigEntry() {
        if (tombstone) {
            return ConfigEntry.tombstone();
        }
        if (payload == null) {
            return ConfigEntry.invalid("Database entry has no configuration value");
        }
        return ConfigEntry.present(payload);
    }
}
