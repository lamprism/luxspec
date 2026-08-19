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

package com.lamprism.luxspec.audit.integration.security;

import com.lamprism.luxspec.data.query.QueryField;

/**
 * Typed fields emitted by security audit translations.
 *
 * <p>Translations must not publish credentials, grants, or token material.</p>
 *
 * @author RollW
 */
public final class SecurityAuditFields {
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

    private SecurityAuditFields() {
    }
}
