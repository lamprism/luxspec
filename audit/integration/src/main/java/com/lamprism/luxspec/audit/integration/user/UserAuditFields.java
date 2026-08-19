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
 * Typed fields emitted by user lifecycle audit translations.
 *
 * <p>Translations must not publish names, email addresses, or password representations.</p>
 *
 * @author RollW
 */
public final class UserAuditFields {
    public static final QueryField<Long> USER_ID = QueryField.of("user.id", Long.class);
    public static final QueryField<String> USER_ROLE = QueryField.of("user.role", String.class);
    public static final QueryField<String> USER_ROLES = QueryField.of("user.roles", String.class);
    public static final QueryField<String> USER_ROLE_CHANGE = QueryField.of("user.roleChange", String.class);
    public static final QueryField<String> USER_STATUS = QueryField.of("user.status", String.class);
    public static final QueryField<Boolean> USER_EMAIL_PRESENT = QueryField.of("user.emailPresent", Boolean.class);

    private UserAuditFields() {
    }
}
