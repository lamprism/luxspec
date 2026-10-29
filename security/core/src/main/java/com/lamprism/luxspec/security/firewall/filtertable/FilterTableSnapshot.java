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

import com.lamprism.luxspec.network.ip.IpAddress;
import com.lamprism.luxspec.network.ip.IpNetwork;
import com.lamprism.luxspec.security.authentication.SimpleSubject;
import com.lamprism.luxspec.security.firewall.FirewallRequest;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable indexed filter-table state.
 *
 * <p>Subject entries have precedence over address entries. Address entries use longest-prefix
 * matching. Expired entries are skipped, allowing a broader active address entry to apply.</p>
 *
 * @author RollW
 */
public final class FilterTableSnapshot {
    private final Map<FilterTarget, FilterEntry> entriesByTarget;
    private final List<FilterEntry> entries;
    private final Map<SimpleSubject, FilterEntry> subjectEntries;
    private final List<AddressEntry> addressEntries;

    /**
     * Creates an immutable filter-table snapshot.
     *
     * @param entries the table entries
     */
    public FilterTableSnapshot(Collection<? extends FilterEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        LinkedHashMap<FilterTarget, FilterEntry> indexedEntries = new LinkedHashMap<>();
        for (FilterEntry entry : entries) {
            FilterEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
            FilterEntry previous = indexedEntries.put(nonNullEntry.target(), nonNullEntry);
            if (previous != null) {
                throw new IllegalArgumentException("entries must not contain duplicate targets");
            }
        }
        entriesByTarget = Collections.unmodifiableMap(indexedEntries);
        this.entries = List.copyOf(indexedEntries.values());
        subjectEntries = createSubjectEntries(this.entries);
        addressEntries = createAddressEntries(this.entries);
    }

    /**
     * Returns an empty snapshot.
     *
     * @return the empty snapshot
     */
    public static FilterTableSnapshot empty() {
        return new FilterTableSnapshot(List.of());
    }

    /**
     * Returns immutable configured entries.
     *
     * @return the entries
     */
    public List<FilterEntry> getEntries() {
        return entries;
    }

    /**
     * Finds an entry by its exact target identity without applying expiration.
     *
     * @param target the target to find
     * @return the configured entry, or {@code null}
     */
    public @Nullable FilterEntry get(FilterTarget target) {
        return entriesByTarget.get(Objects.requireNonNull(target, "target"));
    }

    /**
     * Finds the active entry that applies to one request.
     *
     * @param request the request facts
     * @param now     the expiration evaluation instant
     * @return the active matching entry, or {@code null}
     */
    public @Nullable FilterEntry findMatch(FirewallRequest request, Instant now) {
        FirewallRequest nonNullRequest = Objects.requireNonNull(request, "request");
        Instant nonNullNow = Objects.requireNonNull(now, "now");
        FilterEntry subjectMatch = findSubjectMatch(nonNullRequest, nonNullNow);
        if (subjectMatch != null) {
            return subjectMatch;
        }
        return findAddressMatch(nonNullRequest, nonNullNow);
    }

    FilterTableSnapshot withEntry(FilterEntry entry) {
        FilterEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
        LinkedHashMap<FilterTarget, FilterEntry> replacement = new LinkedHashMap<>(entriesByTarget);
        replacement.put(nonNullEntry.target(), nonNullEntry);
        return new FilterTableSnapshot(replacement.values());
    }

    FilterTableSnapshot withoutTarget(FilterTarget target) {
        FilterTarget nonNullTarget = Objects.requireNonNull(target, "target");
        if (!entriesByTarget.containsKey(nonNullTarget)) {
            return this;
        }
        LinkedHashMap<FilterTarget, FilterEntry> replacement = new LinkedHashMap<>(entriesByTarget);
        replacement.remove(nonNullTarget);
        return new FilterTableSnapshot(replacement.values());
    }

    private @Nullable FilterEntry findSubjectMatch(FirewallRequest request, Instant now) {
        if (request.getSubject() == null) {
            return null;
        }
        SimpleSubject subject = SimpleSubject.from(request.getSubject());
        FilterEntry entry = subjectEntries.get(subject);
        if (entry == null || !entry.isActiveAt(now)) {
            return null;
        }
        return entry;
    }

    private @Nullable FilterEntry findAddressMatch(FirewallRequest request, Instant now) {
        IpAddress requestAddress = request.getClientAddress();
        if (addressEntries.isEmpty() || requestAddress == null) {
            return null;
        }
        for (AddressEntry addressEntry : addressEntries) {
            if (!addressEntry.matches(requestAddress)) {
                continue;
            }
            if (addressEntry.entry().isActiveAt(now)) {
                return addressEntry.entry();
            }
        }
        return null;
    }

    private static Map<SimpleSubject, FilterEntry> createSubjectEntries(List<FilterEntry> entries) {
        Map<SimpleSubject, FilterEntry> result = new LinkedHashMap<>();
        for (FilterEntry entry : entries) {
            if (entry.target() instanceof FilterTarget.SubjectTarget subjectTarget) {
                result.put(subjectTarget.subject(), entry);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<AddressEntry> createAddressEntries(List<FilterEntry> entries) {
        List<AddressEntry> result = new ArrayList<>();
        for (FilterEntry entry : entries) {
            if (entry.target() instanceof FilterTarget.ClientAddressTarget addressTarget) {
                IpNetwork network = addressTarget.address();
                result.add(new AddressEntry(network, entry));
            }
        }
        result.sort(Comparator.comparingInt(AddressEntry::specificity).reversed());
        return List.copyOf(result);
    }

    private static final class AddressEntry {
        private final IpNetwork network;
        private final FilterEntry entry;

        private AddressEntry(IpNetwork network, FilterEntry entry) {
            this.network = network;
            this.entry = entry;
        }

        private int specificity() {
            return network.prefixLength();
        }

        private FilterEntry entry() {
            return entry;
        }

        private boolean matches(IpAddress requestAddress) {
            return network.contains(requestAddress);
        }
    }
}
