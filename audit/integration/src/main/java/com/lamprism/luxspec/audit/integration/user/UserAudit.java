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

package com.lamprism.luxspec.audit.integration.user;

import com.lamprism.luxspec.data.query.QueryField;

/**
 * Stable user audit identifiers and fields.
 *
 * <p>Fields never publish names, email addresses, or password representations.</p>
 *
 * @author RollW
 */
public final class UserAudit {
    /**
     * A user account was registered.
     */
    public static final String REGISTERED = "user.registered";
    /**
     * A user's display or account name changed.
     */
    public static final String RENAMED = "user.renamed";
    /**
     * A user's email address changed.
     */
    public static final String EMAIL_CHANGED = "user.email.changed";
    /**
     * A user's assigned roles changed.
     */
    public static final String ROLES_CHANGED = "user.roles.changed";
    /**
     * A user's lifecycle status changed.
     */
    public static final String STATUS_CHANGED = "user.status.changed";
    /**
     * A user's password changed without exposing password material.
     */
    public static final String PASSWORD_CHANGED = "user.password.changed";

    public static final QueryField<Long> USER_ID = QueryField.of("user.id", Long.class);
    public static final QueryField<String> USER_ROLE = QueryField.of("user.role", String.class);
    public static final QueryField<String> USER_ROLES = QueryField.of("user.roles", String.class);
    public static final QueryField<String> USER_ROLE_CHANGE = QueryField.of("user.roleChange", String.class);
    public static final QueryField<String> USER_STATUS = QueryField.of("user.status", String.class);
    public static final QueryField<Boolean> USER_EMAIL_PRESENT = QueryField.of(
            "user.emailPresent",
            Boolean.class
    );

    private UserAudit() {
    }
}
