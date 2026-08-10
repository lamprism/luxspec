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

package com.lamprism.luxspec.audit.integration;

import com.lamprism.luxspec.data.query.QueryField;

/**
 * Typed custom fields used by the standard Luxspec audit translations.
 *
 * <p>The fields are part of the integration schema. Translators must not put
 * raw configuration values, credentials, password representations, or
 * authorization grants into these fields.</p>
 *
 * @author RollW
 */
public final class LuxspecAuditFields {
    public static final QueryField<String> CONFIG_KEY = QueryField.of("config.key", String.class);
    public static final QueryField<String> CONFIG_SOURCE = QueryField.of("config.source", String.class);
    public static final QueryField<String> CONFIG_CHANGE_TYPE = QueryField.of("config.changeType", String.class);
    public static final QueryField<Boolean> CONFIG_SENSITIVE = QueryField.of("config.sensitive", Boolean.class);
    public static final QueryField<String> CONFIG_PREVIOUS_STATE = QueryField.of("config.previousState", String.class);
    public static final QueryField<String> CONFIG_CURRENT_STATE = QueryField.of("config.currentState", String.class);
    public static final QueryField<String> CONFIG_PREVIOUS_ORIGIN = QueryField.of("config.previousOrigin", String.class);
    public static final QueryField<String> CONFIG_CURRENT_ORIGIN = QueryField.of("config.currentOrigin", String.class);
    public static final QueryField<String> SECURITY_ACTION = QueryField.of("security.action", String.class);
    public static final QueryField<String> SECURITY_SUBJECT_TYPE = QueryField.of("security.subjectType", String.class);
    public static final QueryField<String> SECURITY_SUBJECT_ID = QueryField.of("security.subjectId", String.class);
    public static final QueryField<String> SECURITY_REASON_CODE = QueryField.of("security.reasonCode", String.class);
    public static final QueryField<Long> SECURITY_DURATION_MILLIS = QueryField.of("security.durationMillis", Long.class);
    public static final QueryField<String> SECURITY_RULE_TYPE = QueryField.of("security.ruleType", String.class);
    public static final QueryField<String> SECURITY_CREDENTIAL_TYPE = QueryField.of("security.credentialType", String.class);
    public static final QueryField<String> SECURITY_TOKEN_OPERATION = QueryField.of("security.tokenOperation", String.class);
    public static final QueryField<String> SECURITY_TOKEN_RESULT = QueryField.of("security.tokenResult", String.class);
    public static final QueryField<String> SECURITY_TOKEN_KINDS = QueryField.of("security.tokenKinds", String.class);
    public static final QueryField<Long> SECURITY_RETRY_AFTER_MILLIS = QueryField.of("security.retryAfterMillis", Long.class);
    public static final QueryField<Long> USER_ID = QueryField.of("user.id", Long.class);
    public static final QueryField<String> USER_ROLE = QueryField.of("user.role", String.class);
    public static final QueryField<String> USER_ROLES = QueryField.of("user.roles", String.class);
    public static final QueryField<String> USER_ROLE_CHANGE = QueryField.of("user.roleChange", String.class);
    public static final QueryField<String> USER_STATUS = QueryField.of("user.status", String.class);
    public static final QueryField<Boolean> USER_EMAIL_PRESENT = QueryField.of("user.emailPresent", Boolean.class);

    private LuxspecAuditFields() {
    }
}
