package com.lamprism.luxspec.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lamprism.luxspec.data.ComparisonCondition;
import com.lamprism.luxspec.data.OrderBy;
import com.lamprism.luxspec.data.QueryComplexityLimits;
import com.lamprism.luxspec.data.QueryCondition;
import com.lamprism.luxspec.data.QueryCriteria;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserQuerySchemaTest {
    @Test
    void acceptsTypedFieldsSupportedByTheUserSchema() {
        QueryCriteria criteria = new QueryCriteria(
                ComparisonCondition.greaterThan(UserQueryFields.REGISTERED_AT, Instant.parse("2026-01-01T00:00:00Z")),
                List.of(OrderBy.descending(UserQueryFields.UPDATED_AT))
        );

        assertDoesNotThrow(() -> UserQuerySchema.standard().validate(criteria, QueryComplexityLimits.defaults()));
    }

    @Test
    void rejectsOrderingAndComparisonsThatDoNotBelongToTheUserSchema() {
        QueryCriteria statusComparison = new QueryCriteria(
                ComparisonCondition.greaterThan(UserQueryFields.STATUS, UserStatus.ACTIVE),
                List.of()
        );
        QueryCriteria statusOrdering = new QueryCriteria(
                QueryCondition.equal(UserQueryFields.STATUS, UserStatus.ACTIVE),
                List.of(OrderBy.ascending(UserQueryFields.STATUS))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> UserQuerySchema.standard().validate(statusComparison, QueryComplexityLimits.defaults())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> UserQuerySchema.standard().validate(statusOrdering, QueryComplexityLimits.defaults())
        );
    }
}
