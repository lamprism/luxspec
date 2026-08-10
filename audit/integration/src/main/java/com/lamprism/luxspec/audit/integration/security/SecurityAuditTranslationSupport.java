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

import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.integration.LuxspecAuditFields;
import com.lamprism.luxspec.security.authentication.Subject;
import org.jspecify.annotations.Nullable;

/**
 * Shared safe subject projection for security audit translators.
 *
 * @author RollW
 */
final class SecurityAuditTranslationSupport {
    private SecurityAuditTranslationSupport() {
    }

    static void putSubject(AuditFieldSet.Builder fields, @Nullable Subject subject) {
        if (subject == null) {
            return;
        }
        String subjectType = subject.getType();
        if (!subjectType.isBlank()) {
            fields.put(LuxspecAuditFields.SECURITY_SUBJECT_TYPE, subjectType);
        }
        String subjectId = subject.getId();
        if (!subjectId.isBlank()) {
            fields.put(LuxspecAuditFields.SECURITY_SUBJECT_ID, subjectId);
        }
    }
}
