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
import com.lamprism.luxspec.context.ExecutionContextKeys;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.security.authentication.Authentication;
import com.lamprism.luxspec.security.authentication.SecurityContextKeys;
import com.lamprism.luxspec.security.authentication.Subject;

import java.util.Locale;
import java.util.Objects;

/**
 * Captures correlation and security identity from an explicitly selected context storage.
 *
 * <p>The provider does not select a storage strategy. When the storage has no current value, it
 * returns unknown actor metadata and no correlation identifier.</p>
 *
 * @author RollW
 */
public final class ExecutionContextAuditMetadataProvider implements AuditMetadataProvider {
    private final ExecutionContextStorage storage;

    /**
     * Creates a provider backed by one explicitly selected context storage.
     *
     * @param storage the context storage used for metadata capture
     */
    public ExecutionContextAuditMetadataProvider(ExecutionContextStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public AuditMetadata capture() {
        ExecutionContext context = storage.current().orElseGet(ExecutionContext::empty);
        Authentication authentication = context.get(SecurityContextKeys.AUTHENTICATION).orElse(null);
        AuditActor actor = authentication == null ? AuditActor.unknown() : actor(authentication);
        return new AuditMetadata(
                actor,
                context.get(ExecutionContextKeys.CORRELATION_ID).orElse(null),
                AuditFieldSet.empty()
        );
    }

    private static AuditActor actor(Authentication authentication) {
        Subject subject = authentication.subject();
        return AuditActor.of(kind(subject.getType()), subject.getId());
    }

    private static AuditActor.Kind kind(String subjectType) {
        return switch (subjectType.toLowerCase(Locale.ROOT)) {
            case "user" -> AuditActor.Kind.USER;
            case "service" -> AuditActor.Kind.SERVICE;
            case "system" -> AuditActor.Kind.SYSTEM;
            case "anonymous" -> AuditActor.Kind.ANONYMOUS;
            default -> AuditActor.Kind.UNKNOWN;
        };
    }
}
