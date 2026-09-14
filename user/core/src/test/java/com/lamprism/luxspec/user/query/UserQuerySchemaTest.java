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

package com.lamprism.luxspec.user.query;

import com.lamprism.luxspec.data.query.ComparisonCondition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryComplexityBudget;
import com.lamprism.luxspec.data.query.QueryCondition;
import com.lamprism.luxspec.data.query.QueryCriteria;
import com.lamprism.luxspec.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserQuerySchemaTest {
    @Test
    void acceptsTypedFieldsSupportedByTheUserSchema() {
        QueryCriteria criteria = new QueryCriteria(
                ComparisonCondition.greaterThan(UserQuerySchema.REGISTERED_AT, Instant.parse("2026-01-01T00:00:00Z")),
                List.of(OrderBy.descending(UserQuerySchema.UPDATED_AT))
        );

        assertDoesNotThrow(() -> UserQuerySchema.standard().validate(criteria, QueryComplexityBudget.defaults()));
    }

    @Test
    void rejectsOrderingAndComparisonsThatDoNotBelongToTheUserSchema() {
        QueryCriteria statusComparison = new QueryCriteria(
                ComparisonCondition.greaterThan(UserQuerySchema.STATUS, UserStatus.ACTIVE),
                List.of()
        );
        QueryCriteria statusOrdering = new QueryCriteria(
                QueryCondition.equal(UserQuerySchema.STATUS, UserStatus.ACTIVE),
                List.of(OrderBy.ascending(UserQuerySchema.STATUS))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> UserQuerySchema.standard().validate(statusComparison, QueryComplexityBudget.defaults())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> UserQuerySchema.standard().validate(statusOrdering, QueryComplexityBudget.defaults())
        );
    }
}
