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

package com.lamprism.luxspec.security.firewall.filtertable;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Mutable filter-table boundary used by dynamic firewalls.
 *
 * <p>Implementations publish immutable snapshots. The boundary intentionally has no database
 * contract, so an application may choose memory or file storage without exposing a persistence
 * vendor to firewall consumers.</p>
 *
 * @author RollW
 */
public interface FilterTable {
    /**
     * Returns the current immutable table snapshot.
     *
     * @return the current snapshot
     */
    FilterTableSnapshot snapshot();

    /**
     * Returns all configured entries from the current snapshot.
     *
     * @return immutable entries
     */
    default List<FilterEntry> getEntries() {
        return snapshot().getEntries();
    }

    /**
     * Returns the configured entry for a target.
     *
     * @param target the target to find
     * @return the entry, or {@code null} when absent
     */
    default @Nullable FilterEntry get(FilterTarget target) {
        return snapshot().get(target);
    }

    /**
     * Inserts or replaces an entry by target.
     *
     * @param entry the entry to store
     */
    void put(FilterEntry entry);

    /**
     * Removes an entry by target.
     *
     * @param target the target to remove
     * @return {@code true} when an entry was removed
     */
    boolean remove(FilterTarget target);

    /**
     * Atomically replaces all entries.
     *
     * @param entries replacement entries
     */
    void replaceAll(Collection<? extends FilterEntry> entries);

    /**
     * Removes all entries.
     */
    void clear();
}
