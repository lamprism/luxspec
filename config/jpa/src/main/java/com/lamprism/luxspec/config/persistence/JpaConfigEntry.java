package com.lamprism.luxspec.config.persistence;

import com.lamprism.luxspec.config.ConfigEntry;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSourceId;
import com.lamprism.luxspec.config.RawConfigValue;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Internal JPA mapping for one scalar, list, or mask configuration entry.
 *
 * <p>The owning application applies this package's Liquibase changelog before using the mapping.</p>
 */
@Entity
@Table(name = "luxspec_config_entry")
public class JpaConfigEntry {
    private enum RawKind {
        SCALAR,
        LIST
    }

    @EmbeddedId
    private JpaConfigEntryId id;
    @Column(name = "masked", nullable = false)
    private boolean masked;
    @Enumerated(EnumType.STRING)
    @Column(name = "raw_kind", length = 16)
    private @Nullable RawKind rawKind;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "luxspec_config_entry_value",
            joinColumns = {
                    @JoinColumn(name = "source_id", referencedColumnName = "source_id", nullable = false),
                    @JoinColumn(name = "config_key", referencedColumnName = "config_key", nullable = false)
            }
    )
    @OrderColumn(name = "position")
    @Lob
    @Column(name = "raw_value", nullable = false)
    private List<String> rawValues = new ArrayList<>();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    /**
     * Creates an empty instance for JPA materialization.
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

    static JpaConfigEntry createMasked(ConfigSourceId sourceId, ConfigKey key, Instant updatedAt) {
        JpaConfigEntry entry = new JpaConfigEntry();
        entry.id = JpaConfigEntryId.of(sourceId, key);
        entry.mask(updatedAt);
        return entry;
    }

    void replace(RawConfigValue rawValue, Instant updatedAt) {
        RawConfigValue nonNullRawValue = Objects.requireNonNull(rawValue, "rawValue");
        this.masked = false;
        this.rawKind = switch (nonNullRawValue.getKind()) {
            case SCALAR -> RawKind.SCALAR;
            case LIST -> RawKind.LIST;
        };
        this.rawValues.clear();
        if (nonNullRawValue.getKind() == RawConfigValue.Kind.SCALAR) {
            rawValues.add(nonNullRawValue.requireScalar());
        } else {
            rawValues.addAll(nonNullRawValue.requireList());
        }
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    void mask(Instant updatedAt) {
        this.masked = true;
        this.rawKind = null;
        this.rawValues.clear();
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    ConfigEntry toConfigEntry() {
        if (masked) {
            return ConfigEntry.masked();
        }
        if (rawKind == null) {
            return ConfigEntry.invalid("Database entry has no raw value kind");
        }
        if (rawKind == RawKind.SCALAR) {
            if (rawValues.size() != 1) {
                return ConfigEntry.invalid("Database scalar entry has an invalid value count");
            }
            return ConfigEntry.present(rawValues.get(0));
        }
        return ConfigEntry.present(rawValues);
    }
}
