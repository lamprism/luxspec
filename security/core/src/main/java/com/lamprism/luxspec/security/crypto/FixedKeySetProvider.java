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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provides fixed key-set snapshots from already parsed JCA key objects.
 *
 * <p>This provider is the direct-injection path for callers that already own key objects. It uses
 * the same {@link KeySetProvider} contract as configuration and external source providers.</p>
 *
 * @author RollW
 */
public final class FixedKeySetProvider implements KeySetProvider {
    private final Map<String, KeySet> keySets;

    /**
     * Creates a provider from named immutable key-set snapshots.
     *
     * <p>The provider copies the map structure but does not copy, transform, or destroy the key
     * objects held by the snapshots.</p>
     *
     * @param keySets the non-empty map of provider-owned references to key sets
     */
    public FixedKeySetProvider(Map<String, ? extends KeySet> keySets) {
        Objects.requireNonNull(keySets, "keySets");
        if (keySets.isEmpty()) {
            throw new IllegalArgumentException("At least one key set is required");
        }
        Map<String, KeySet> copiedKeySets = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends KeySet> entry : keySets.entrySet()) {
            String reference = requireReference(entry.getKey());
            KeySet keySet = Objects.requireNonNull(entry.getValue(), "keySet");
            copiedKeySets.put(reference, keySet);
        }
        this.keySets = Map.copyOf(copiedKeySets);
    }

    /**
     * Returns the fixed key-set snapshot for a provider-owned reference.
     *
     * @param keySetReference the provider-owned key-set reference
     * @return the configured key-set snapshot
     * @throws IllegalArgumentException when the reference is blank or undefined
     */
    @Override
    public KeySet get(String keySetReference) {
        String reference = requireReference(keySetReference);
        KeySet keySet = keySets.get(reference);
        if (keySet == null) {
            throw new IllegalArgumentException("Key set is not defined: " + reference);
        }
        return keySet;
    }

    private static String requireReference(String value) {
        String reference = Objects.requireNonNull(value, "keySetReference");
        if (reference.isBlank()) {
            throw new IllegalArgumentException("keySetReference must not be blank");
        }
        return reference;
    }
}
