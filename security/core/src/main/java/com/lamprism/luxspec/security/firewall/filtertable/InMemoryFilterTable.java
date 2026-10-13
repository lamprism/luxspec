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

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

/**
 * Thread-safe in-memory filter table with immutable atomic snapshots.
 *
 * @author RollW
 */
public final class InMemoryFilterTable implements FilterTable {
    private final AtomicReference<FilterTableSnapshot> currentSnapshot;

    /**
     * Creates an empty filter table.
     */
    public InMemoryFilterTable() {
        this(FilterTableSnapshot.empty());
    }

    /**
     * Creates a table with initial entries.
     *
     * @param entries the initial entries
     */
    public InMemoryFilterTable(Collection<? extends FilterEntry> entries) {
        this(new FilterTableSnapshot(entries));
    }

    private InMemoryFilterTable(FilterTableSnapshot snapshot) {
        currentSnapshot = new AtomicReference<>(Objects.requireNonNull(snapshot, "snapshot"));
    }

    @Override
    public FilterTableSnapshot snapshot() {
        return currentSnapshot.get();
    }

    @Override
    public void put(FilterEntry entry) {
        FilterEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
        update(snapshot -> snapshot.withEntry(nonNullEntry));
    }

    @Override
    public boolean remove(FilterTarget target) {
        FilterTarget nonNullTarget = Objects.requireNonNull(target, "target");
        while (true) {
            FilterTableSnapshot current = currentSnapshot.get();
            FilterTableSnapshot replacement = current.withoutTarget(nonNullTarget);
            if (current == replacement) {
                return false;
            }
            if (currentSnapshot.compareAndSet(current, replacement)) {
                return true;
            }
        }
    }

    @Override
    public void replaceAll(Collection<? extends FilterEntry> entries) {
        FilterTableSnapshot replacement = new FilterTableSnapshot(entries);
        update(snapshot -> replacement);
    }

    @Override
    public void clear() {
        update(snapshot -> FilterTableSnapshot.empty());
    }

    private void update(UnaryOperator<FilterTableSnapshot> updater) {
        while (true) {
            FilterTableSnapshot current = currentSnapshot.get();
            FilterTableSnapshot replacement = Objects.requireNonNull(updater.apply(current), "snapshot");
            if (currentSnapshot.compareAndSet(current, replacement)) {
                return;
            }
        }
    }
}
