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

import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QuerySchema;

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
