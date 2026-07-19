package com.lamprism.luxspec.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

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

        assertThrows(IllegalArgumentException.class, () -> schema.validate(criteria, QueryComplexityLimits.defaults()));
    }

    @Test
    void validatesLogicalAndStructuralLimits() {
        QuerySchema schema = QuerySchema.builder()
                .add(STATUS, Set.of(QueryOperator.EQUAL, QueryOperator.IN), true)
                .add(PRIORITY, Set.of(QueryOperator.GREATER_THAN))
                .build();
        QueryExpression expression = QueryExpressions.or(List.of(
                QueryCondition.equal(STATUS, "active"),
                ComparisonCondition.greaterThan(PRIORITY, 3)
        ));
        QueryCriteria criteria = new QueryCriteria(expression, List.of(OrderBy.ascending(STATUS)));

        schema.validate(criteria, new QueryComplexityLimits(2, 3, 2, 2, 16));
        assertThrows(
                IllegalArgumentException.class,
                () -> schema.validate(criteria, new QueryComplexityLimits(2, 3, 1, 2, 16))
        );
    }

    @Test
    void rejectsInvalidLikeEscapesAndKeepsCriteriaImmutable() {
        QueryCriteria criteria = new QueryCriteria(QueryCondition.equal(STATUS, "active"), List.of(OrderBy.ascending(STATUS)));

        assertThrows(IllegalArgumentException.class, () -> LikeCondition.of(STATUS, "value\\x"));
        assertThrows(UnsupportedOperationException.class, () -> criteria.getOrders().add(OrderBy.descending(STATUS)));
    }
}
