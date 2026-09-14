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

package com.lamprism.luxspec.data.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryConditionTest {
    private static final QueryField<String> STATUS = QueryField.of("status", String.class);
    private static final QueryField<Integer> PRIORITY = QueryField.of("priority", Integer.class);

    @Test
    void preservesTheFieldValueTypeForMembershipValues() {
        QueryCondition<String> condition = QueryCondition.in(STATUS, List.of("active", "disabled"));

        assertEquals(STATUS, condition.getField());
        assertEquals(List.of("active", "disabled"), condition.getValues());
    }

    @Test
    void rejectsOperatorsThatAreNotAllowedByTheSchema() {
        QuerySchema schema = QuerySchema.builder()
                .add(STATUS, Set.of(QueryOperator.EQUAL), true)
                .add(PRIORITY, Set.of(QueryOperator.GREATER_THAN))
                .build();
        QueryCriteria criteria = new QueryCriteria(QueryCondition.in(STATUS, List.of("active")), List.of());

        assertThrows(IllegalArgumentException.class, () -> schema.validate(criteria, QueryComplexityBudget.defaults()));
    }

    @Test
    void validatesLogicalAndStructuralLimits() {
        QuerySchema schema = QuerySchema.builder()
                .add(STATUS, Set.of(QueryOperator.EQUAL, QueryOperator.IN), true)
                .add(PRIORITY, Set.of(QueryOperator.GREATER_THAN))
                .build();
        QueryExpression expression = QueryExpression.or(List.of(
                QueryCondition.equal(STATUS, "active"),
                ComparisonCondition.greaterThan(PRIORITY, 3)
        ));
        QueryCriteria criteria = new QueryCriteria(expression, List.of(OrderBy.ascending(STATUS)));

        schema.validate(criteria, new QueryComplexityBudget(2, 3, 2, 2, 16));
        assertThrows(
                IllegalArgumentException.class,
                () -> schema.validate(criteria, new QueryComplexityBudget(2, 3, 1, 2, 16))
        );
    }

    @Test
    void rejectsInvalidLikeEscapesAndKeepsCriteriaImmutable() {
        QueryCriteria criteria = new QueryCriteria(QueryCondition.equal(STATUS, "active"), List.of(OrderBy.ascending(STATUS)));

        assertThrows(IllegalArgumentException.class, () -> LikeCondition.of(STATUS, "value\\x"));
        assertThrows(UnsupportedOperationException.class, () -> criteria.getOrders().add(OrderBy.descending(STATUS)));
    }
}
