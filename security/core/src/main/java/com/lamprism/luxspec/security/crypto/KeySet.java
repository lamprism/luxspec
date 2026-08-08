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

package com.lamprism.luxspec.security.crypto;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds an immutable snapshot of identified verification keys and an optional active signing key.
 *
 * @author RollW
 */
public final class KeySet {
    private final Map<String, KeyEntry> entries;
    private final Optional<KeyEntry> activeKey;

    private KeySet(Map<String, KeyEntry> entries, Optional<KeyEntry> activeKey) {
        this.entries = Map.copyOf(entries);
        this.activeKey = Objects.requireNonNull(activeKey, "activeKey");
    }

    /**
     * Creates a verification-only key-set snapshot.
     *
     * @param entries the non-empty uniquely identified key entries
     * @return the immutable key-set snapshot
     */
    public static KeySet forVerification(Iterable<? extends KeyEntry> entries) {
        return create(entries, null);
    }

    /**
     * Creates a key-set snapshot with one active signing key.
     *
     * @param activeKeyId the ID of an entry that contains signing material
     * @param entries     the non-empty uniquely identified key entries
     * @return the immutable key-set snapshot
     */
    public static KeySet withActiveKey(String activeKeyId, Iterable<? extends KeyEntry> entries) {
        return create(entries, Objects.requireNonNull(activeKeyId, "activeKeyId"));
    }

    /**
     * Combines multiple source snapshots into one immutable snapshot.
     *
     * <p>Key IDs must be globally unique across the input snapshots. At most one input snapshot
     * may identify an active signing key.</p>
     *
     * @param keySets the non-empty source snapshots
     * @return the combined key-set snapshot
     */
    public static KeySet combine(Iterable<? extends KeySet> keySets) {
        Objects.requireNonNull(keySets, "keySets");
        Map<String, KeyEntry> combinedEntries = new LinkedHashMap<>();
        @Nullable String activeKeyId = null;
        boolean hasSnapshot = false;
        for (KeySet keySet : keySets) {
            KeySet nonNullKeySet = Objects.requireNonNull(keySet, "keySet");
            hasSnapshot = true;
            Optional<KeyEntry> candidateActiveKey = nonNullKeySet.getActiveKey();
            if (candidateActiveKey.isPresent()) {
                if (activeKeyId != null) {
                    throw new IllegalArgumentException("Multiple active signing keys are not supported");
                }
                activeKeyId = candidateActiveKey.orElseThrow().getId();
            }
            for (KeyEntry entry : nonNullKeySet.entries.values()) {
                if (combinedEntries.put(entry.getId(), entry) != null) {
                    throw new IllegalArgumentException("Duplicate key ID across key sets: " + entry.getId());
                }
            }
        }
        if (!hasSnapshot) {
            throw new IllegalArgumentException("At least one key set is required");
        }
        if (activeKeyId == null) {
            return forVerification(combinedEntries.values());
        }
        return withActiveKey(activeKeyId, combinedEntries.values());
    }

    /**
     * Returns the active signing key entry when this snapshot can sign.
     *
     * @return the optional active signing key entry
     */
    public Optional<KeyEntry> getActiveKey() {
        return activeKey;
    }

    /**
     * Finds a key entry by its stable key ID.
     *
     * @param keyId the key ID to find
     * @return the optional matching key entry
     */
    public Optional<KeyEntry> findById(String keyId) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(keyId, "keyId")));
    }

    private static KeySet create(Iterable<? extends KeyEntry> entries, @Nullable String activeKeyId) {
        Objects.requireNonNull(entries, "entries");
        Map<String, KeyEntry> indexedEntries = new LinkedHashMap<>();
        for (KeyEntry entry : entries) {
            KeyEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
            if (indexedEntries.put(nonNullEntry.getId(), nonNullEntry) != null) {
                throw new IllegalArgumentException("Duplicate key ID: " + nonNullEntry.getId());
            }
        }
        if (indexedEntries.isEmpty()) {
            throw new IllegalArgumentException("At least one key entry is required");
        }
        if (activeKeyId == null) {
            return new KeySet(indexedEntries, Optional.empty());
        }
        KeyEntry activeKey = indexedEntries.get(activeKeyId);
        if (activeKey == null) {
            throw new IllegalArgumentException("Active key ID is unknown");
        }
        if (activeKey.getSigningKey().isEmpty()) {
            throw new IllegalArgumentException("Active key must contain signing material");
        }
        return new KeySet(indexedEntries, Optional.of(activeKey));
    }
}
