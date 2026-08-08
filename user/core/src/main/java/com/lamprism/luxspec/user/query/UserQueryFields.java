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
import com.lamprism.luxspec.user.UserStatus;

import java.time.Instant;

/**
 * Holds typed field identities supported by ordinary user browsing.
 *
 * @author RollW
 */
public final class UserQueryFields {
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

    private UserQueryFields() {
    }
}
