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
import com.lamprism.luxspec.data.query.QueryComplexityBudget;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QuerySchema;
import com.lamprism.luxspec.resource.ResourceReference;

import java.time.Instant;
import java.util.Set;

/**
 * Defines the standard provider-independent query fields and schema for audit entries.
 *
 * <p>The standard schema permits equality and membership filters for identity
 * and classification fields. {@code occurredAt} additionally permits ordered
 * comparisons and is orderable. Custom fields are not included in this schema.
 *
 * @author RollW
 */
public final class AuditQuerySchema {
    /**
     * Stable source event identity.
     */
    public static final QueryField<AuditEventId> ID = QueryField.of("id", AuditEventId.class);
    /**
     * Stable audit event name.
     */
    public static final QueryField<String> EVENT_NAME = QueryField.of("eventName", String.class);
    /**
     * Time at which the audited operation occurred.
     */
    public static final QueryField<Instant> OCCURRED_AT = QueryField.of("occurredAt", Instant.class);
    /**
     * Provider-independent actor classification.
     */
    public static final QueryField<AuditActor.Kind> ACTOR_KIND = QueryField.of("actorKind", AuditActor.Kind.class);
    /**
     * Optional actor identifier.
     */
    public static final QueryField<String> ACTOR_ID = QueryField.of("actorId", String.class);
    /**
     * Optional cross-operation correlation identifier.
     */
    public static final QueryField<CorrelationId> CORRELATION_ID = QueryField.of(
            "correlationId",
            CorrelationId.class
    );
    /**
     * Semantic audit action.
     */
    public static final QueryField<AuditAction> ACTION = QueryField.of("action", AuditAction.class);
    /**
     * Audited operation outcome.
     */
    public static final QueryField<AuditOutcome> OUTCOME = QueryField.of("outcome", AuditOutcome.class);
    /**
     * Optional affected resource type name.
     */
    public static final QueryField<String> RESOURCE_TYPE = QueryField.of("resourceType", String.class);
    /**
     * Optional affected resource reference.
     */
    @SuppressWarnings("rawtypes")
    public static final QueryField<ResourceReference> RESOURCE = QueryField.of(
            "resource",
            ResourceReference.class
    );

    private static final QuerySchema STANDARD = createStandard();

    private AuditQuerySchema() {
    }

    /**
     * Returns the immutable schema for the standard audit fields.
     *
     * @return the standard audit query schema
     */
    public static QuerySchema standard() {
        return STANDARD;
    }

    /**
     * Validates criteria with the default shared complexity budget.
     *
     * @param criteria the audit query criteria
     */
    public static void validate(QueryCriteria criteria) {
        STANDARD.validate(criteria, QueryComplexityBudget.defaults());
    }

    private static QuerySchema createStandard() {
        QuerySchema.Builder builder = QuerySchema.builder();
        builder.add(AuditQuerySchema.ID, Set.of(QueryOperator.EQUAL, QueryOperator.IN), true);
        addEquality(builder, AuditQuerySchema.EVENT_NAME);
        addEquality(builder, AuditQuerySchema.ACTOR_KIND);
        addEquality(builder, AuditQuerySchema.ACTOR_ID);
        addEquality(builder, AuditQuerySchema.CORRELATION_ID);
        addEquality(builder, AuditQuerySchema.ACTION);
        addEquality(builder, AuditQuerySchema.OUTCOME);
        addEquality(builder, AuditQuerySchema.RESOURCE_TYPE);
        addEquality(builder, AuditQuerySchema.RESOURCE);
        builder.add(
                AuditQuerySchema.OCCURRED_AT,
                Set.of(
                        QueryOperator.EQUAL,
                        QueryOperator.GREATER_THAN,
                        QueryOperator.GREATER_THAN_OR_EQUAL,
                        QueryOperator.LESS_THAN,
                        QueryOperator.LESS_THAN_OR_EQUAL
                ),
                true
        );
        return builder.build();
    }

    private static <T> void addEquality(QuerySchema.Builder builder, QueryField<T> field) {
        builder.add(field, Set.of(QueryOperator.EQUAL, QueryOperator.IN));
    }
}
