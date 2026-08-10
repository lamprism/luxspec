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

import com.lamprism.luxspec.data.query.QueryComplexityLimits;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QuerySchema;

import java.util.Set;

/**
 * Role-specific query schema for standard audit fields.
 *
 * <p>The default schema permits equality and membership filters for identity
 * and classification fields. {@code occurredAt} additionally permits ordered
 * comparisons and is orderable, while custom fields and opaque details require
 * explicit application registration.
 *
 * @author RollW
 */
public final class AuditQuerySchema {
    private final QuerySchema schema;

    private AuditQuerySchema(QuerySchema schema) {
        this.schema = schema;
    }

    public static AuditQuerySchema defaults() {
        QuerySchema.Builder builder = QuerySchema.builder();
        builder.add(AuditQueryFields.ID, Set.of(QueryOperator.EQUAL, QueryOperator.IN), true);
        addEquality(builder, AuditQueryFields.EVENT_NAME);
        addEquality(builder, AuditQueryFields.ACTOR_KIND);
        addEquality(builder, AuditQueryFields.ACTOR_ID);
        addEquality(builder, AuditQueryFields.CORRELATION_ID);
        addEquality(builder, AuditQueryFields.ACTION);
        addEquality(builder, AuditQueryFields.OUTCOME);
        addEquality(builder, AuditQueryFields.RESOURCE_TYPE);
        addEquality(builder, AuditQueryFields.RESOURCE);
        builder.add(
                AuditQueryFields.OCCURRED_AT,
                Set.of(
                        QueryOperator.EQUAL,
                        QueryOperator.GREATER_THAN,
                        QueryOperator.GREATER_THAN_OR_EQUAL,
                        QueryOperator.LESS_THAN,
                        QueryOperator.LESS_THAN_OR_EQUAL
                ),
                true
        );
        return new AuditQuerySchema(builder.build());
    }

    public void validate(QueryCriteria criteria) {
        schema.validate(criteria, QueryComplexityLimits.defaults());
    }

    public QuerySchema schema() {
        return schema;
    }

    private static <T> void addEquality(QuerySchema.Builder builder, QueryField<T> field) {
        builder.add(field, Set.of(QueryOperator.EQUAL, QueryOperator.IN));
    }
}
