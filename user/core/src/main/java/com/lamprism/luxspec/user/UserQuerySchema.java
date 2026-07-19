package com.lamprism.luxspec.user;

import com.lamprism.luxspec.data.QueryOperator;
import com.lamprism.luxspec.data.QuerySchema;
import java.util.Set;

/**
 * Declares ordinary user browsing fields, operators, and ordering capabilities.
 *
 * @author RollW
 */
public final class UserQuerySchema {
    private static final Set<QueryOperator> EQUALITY_OPERATORS = Set.of(
            QueryOperator.EQUAL,
            QueryOperator.NOT_EQUAL,
            QueryOperator.IN,
            QueryOperator.NOT_IN
    );
    private static final Set<QueryOperator> TEXT_OPERATORS = Set.of(
            QueryOperator.EQUAL,
            QueryOperator.NOT_EQUAL,
            QueryOperator.LIKE,
            QueryOperator.IN,
            QueryOperator.NOT_IN
    );
    private static final Set<QueryOperator> ORDERED_OPERATORS = Set.of(
            QueryOperator.EQUAL,
            QueryOperator.NOT_EQUAL,
            QueryOperator.GREATER_THAN,
            QueryOperator.GREATER_THAN_OR_EQUAL,
            QueryOperator.LESS_THAN,
            QueryOperator.LESS_THAN_OR_EQUAL,
            QueryOperator.IN,
            QueryOperator.NOT_IN
    );
    private static final QuerySchema STANDARD = QuerySchema.builder()
            .add(UserQueryFields.ID, ORDERED_OPERATORS, true)
            .add(UserQueryFields.USERNAME, TEXT_OPERATORS, true)
            .add(UserQueryFields.EMAIL, TEXT_OPERATORS, true)
            .add(UserQueryFields.STATUS, EQUALITY_OPERATORS)
            .add(UserQueryFields.REGISTERED_AT, ORDERED_OPERATORS, true)
            .add(UserQueryFields.UPDATED_AT, ORDERED_OPERATORS, true)
            .build();

    private UserQuerySchema() {
    }

    /**
     * Returns the standard schema for ordinary user browsing.
     *
     * @return the immutable user query schema
     */
    public static QuerySchema standard() {
        return STANDARD;
    }
}
