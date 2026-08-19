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
import com.lamprism.luxspec.security.authentication.Subject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Projects an optional subject into the standard security audit fields.
 *
 * @author RollW
 */
final class SubjectAuditProjection {
    private SubjectAuditProjection() {
    }

    static void putSubject(AuditFieldSet.Builder fields, @Nullable Subject subject) {
        if (subject == null) {
            return;
        }
        fields.put(
                SecurityAuditFields.SECURITY_SUBJECT_TYPE,
                Objects.requireNonNull(subject.getType(), "subject.type")
        );
        fields.put(
                SecurityAuditFields.SECURITY_SUBJECT_ID,
                Objects.requireNonNull(subject.getId(), "subject.id")
        );
    }
}
