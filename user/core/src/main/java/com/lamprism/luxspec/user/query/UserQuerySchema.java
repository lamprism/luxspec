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

import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.data.query.QueryOperator;
import com.lamprism.luxspec.data.query.QuerySchema;
import com.lamprism.luxspec.user.UserStatus;

import java.time.Instant;
import java.util.Set;

/**
 * Declares ordinary user browsing fields, operators, and ordering capabilities.
 *
 * @author RollW
 */
public final class UserQuerySchema {
    /**
     * The generated account identifier.
     */
    public static final QueryField<Long> ID = QueryField.of("id", Long.class);
    /**
     * The unique account name.
     */
    public static final QueryField<String> USERNAME = QueryField.of("username", String.class);
    /**
     * The optional account email address.
     */
    public static final QueryField<String> EMAIL = QueryField.of("email", String.class);
    /**
     * The mutually exclusive account lifecycle status.
     */
    public static final QueryField<UserStatus> STATUS = QueryField.of("status", UserStatus.class);
    /**
     * The account registration time.
     */
    public static final QueryField<Instant> REGISTERED_AT = QueryField.of("registered-at", Instant.class);
    /**
     * The most recent account update time.
     */
    public static final QueryField<Instant> UPDATED_AT = QueryField.of("updated-at", Instant.class);

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
            .add(UserQuerySchema.ID, ORDERED_OPERATORS, true)
            .add(UserQuerySchema.USERNAME, TEXT_OPERATORS, true)
            .add(UserQuerySchema.EMAIL, TEXT_OPERATORS, true)
            .add(UserQuerySchema.STATUS, EQUALITY_OPERATORS)
            .add(UserQuerySchema.REGISTERED_AT, ORDERED_OPERATORS, true)
            .add(UserQuerySchema.UPDATED_AT, ORDERED_OPERATORS, true)
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
