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

package com.lamprism.luxspec.audit.store;

import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditEventId;
import com.lamprism.luxspec.audit.publish.AuditSink;
import com.lamprism.luxspec.audit.query.AuditQuerySchema;
import com.lamprism.luxspec.audit.query.AuditReader;
import com.lamprism.luxspec.data.pagination.QueryResult;
import com.lamprism.luxspec.data.pagination.QueryWindow;
import com.lamprism.luxspec.data.query.InMemoryQueryExecutor;
import com.lamprism.luxspec.data.query.QueryCriteria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe in-memory audit store that can act as both a sink and a reader.
 *
 * <p>Entries are retained in publication order and event IDs are idempotent. Reaccepting the same
 * event ID with the same entry is harmless; reusing an ID for different content is rejected.</p>
 *
 * @author RollW
 */
public class InMemoryAuditStore implements AuditSink, AuditReader {
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();
    private final Map<AuditEventId, AuditEntry> entriesById = new LinkedHashMap<>();
    private final InMemoryQueryExecutor<AuditEntry> queryExecutor;

    /**
     * Creates an empty in-memory audit store.
     */
    public InMemoryAuditStore() {
        this.queryExecutor = InMemoryQueryExecutor.builder(this::snapshot)
                .field(AuditQuerySchema.ID, AuditEntry::id, Comparator.comparing(AuditEventId::value))
                .field(AuditQuerySchema.EVENT_NAME, AuditEntry::eventName)
                .field(AuditQuerySchema.OCCURRED_AT, AuditEntry::occurredAt)
                .field(AuditQuerySchema.ACTOR_KIND, entry -> entry.metadata().actor().kind())
                .field(AuditQuerySchema.ACTOR_ID, entry -> entry.metadata().actor().id())
                .field(AuditQuerySchema.CORRELATION_ID, entry -> entry.metadata().correlationId())
                .field(AuditQuerySchema.ACTION, AuditEntry::action)
                .field(AuditQuerySchema.OUTCOME, AuditEntry::outcome)
                .field(AuditQuerySchema.RESOURCE_TYPE, entry -> {
                    if (entry.resource() == null) {
                        return null;
                    }
                    return entry.resource().resourceType().getName();
                })
                .field(AuditQuerySchema.RESOURCE, AuditEntry::resource)
                .build();
    }

    @Override
    public void accept(AuditEntry entry) {
        AuditEntry nonNullEntry = Objects.requireNonNull(entry, "entry");
        stateLock.writeLock().lock();
        try {
            AuditEntry previous = entriesById.putIfAbsent(nonNullEntry.id(), nonNullEntry);
            if (previous != null && !previous.equals(nonNullEntry)) {
                throw new IllegalStateException("Audit event ID is already associated with another entry");
            }
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public QueryResult<AuditEntry> browse(QueryCriteria criteria, QueryWindow window) {
        QueryCriteria nonNullCriteria = Objects.requireNonNull(criteria, "criteria");
        AuditQuerySchema.validate(nonNullCriteria);
        return queryExecutor.query(nonNullCriteria, Objects.requireNonNull(window, "window"));
    }

    /**
     * Returns an immutable snapshot in publication order.
     *
     * @return the retained audit entries
     */
    public List<AuditEntry> entries() {
        return snapshot();
    }

    /**
     * Returns the number of retained entries.
     *
     * @return the entry count
     */
    public int size() {
        stateLock.readLock().lock();
        try {
            return entriesById.size();
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Clears all retained entries.
     */
    public void clear() {
        stateLock.writeLock().lock();
        try {
            entriesById.clear();
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    private List<AuditEntry> snapshot() {
        stateLock.readLock().lock();
        try {
            return List.copyOf(new ArrayList<>(entriesById.values()));
        } finally {
            stateLock.readLock().unlock();
        }
    }
}
