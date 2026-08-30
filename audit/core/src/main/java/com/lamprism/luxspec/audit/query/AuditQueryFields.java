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

package com.lamprism.luxspec.audit.query;

import com.lamprism.luxspec.audit.AuditAction;
import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.audit.AuditEventId;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.context.CorrelationId;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.resource.ResourceReference;

import java.time.Instant;

/**
 * Standard typed fields available to audit readers.
 *
 * <p>Field names are stable provider-independent query keys. Custom audit
 * fields are not added here automatically; applications must add their own
 * {@link QueryField} definitions to an explicitly assembled query schema.
 *
 * @author RollW
 */
public final class AuditQueryFields {
    /**
     * Stable source event identity.
     */
    public static final QueryField<AuditEventId> ID = QueryField.of("id", AuditEventId.class);
    /** Stable audit event name. */
    public static final QueryField<String> EVENT_NAME = QueryField.of("eventName", String.class);
    /** Time at which the audited operation occurred. */
    public static final QueryField<Instant> OCCURRED_AT = QueryField.of("occurredAt", Instant.class);
    /** Provider-independent actor classification. */
    public static final QueryField<AuditActor.Kind> ACTOR_KIND = QueryField.of("actorKind", AuditActor.Kind.class);
    /** Optional actor identifier. */
    public static final QueryField<String> ACTOR_ID = QueryField.of("actorId", String.class);
    /** Optional cross-operation correlation identifier. */
    public static final QueryField<CorrelationId> CORRELATION_ID = QueryField.of("correlationId", CorrelationId.class);
    /** Semantic audit action. */
    public static final QueryField<AuditAction> ACTION = QueryField.of("action", AuditAction.class);
    /** Audited operation outcome. */
    public static final QueryField<AuditOutcome> OUTCOME = QueryField.of("outcome", AuditOutcome.class);
    /** Optional affected resource type name. */
    public static final QueryField<String> RESOURCE_TYPE = QueryField.of("resourceType", String.class);
    /** Optional affected resource reference. */
    @SuppressWarnings("rawtypes")
    public static final QueryField<ResourceReference> RESOURCE = QueryField.of("resource", ResourceReference.class);

    private AuditQueryFields() {
    }
}
