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

package com.lamprism.luxspec.audit.integration;

import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditMetadata;
import com.lamprism.luxspec.audit.publish.AuditMetadataProvider;
import com.lamprism.luxspec.context.ExecutionContext;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.security.authentication.Authentication;
import java.util.Objects;

/**
 * Captures correlation and security identity from a configured context storage.
 *
 * <p>The provider does not select a storage strategy. When the storage has no current value, it
 * returns unknown actor metadata and no correlation identifier.</p>
 *
 * @author RollW
 */
public final class ExecutionContextAuditMetadataProvider implements AuditMetadataProvider {
    private final ExecutionContextStorage storage;
    private final AuditActorMapper actorMapper;

    /**
     * Creates a provider backed by one configured context storage.
     *
     * @param storage the context storage used for metadata capture
     */
    public ExecutionContextAuditMetadataProvider(ExecutionContextStorage storage) {
        this(storage, AuditActorMapper.standard());
    }

    /**
     * Creates a provider with an application-selected subject mapping.
     *
     * @param storage     the context storage used for metadata capture
     * @param actorMapper the authenticated subject mapping
     */
    public ExecutionContextAuditMetadataProvider(ExecutionContextStorage storage, AuditActorMapper actorMapper) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.actorMapper = Objects.requireNonNull(actorMapper, "actorMapper");
    }

    @Override
    public AuditMetadata capture() {
        ExecutionContext context = storage.current().orElseGet(ExecutionContext::empty);
        Authentication authentication = context.get(Authentication.CONTEXT_KEY).orElse(null);
        AuditActor actor = authentication == null ? AuditActor.unknown()
                : Objects.requireNonNull(actorMapper.map(authentication.subject()), "audit actor");
        return new AuditMetadata(
                actor,
                context.get(ExecutionContext.CORRELATION_ID).orElse(null),
                AuditFieldSet.empty()
        );
    }

}
