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

import com.lamprism.luxspec.audit.AuditActor;
import com.lamprism.luxspec.security.authentication.Subject;

import java.util.Locale;
import java.util.Objects;

/**
 * Maps an authenticated subject to the actor recorded in an audit entry.
 *
 * @author RollW
 */
@FunctionalInterface
public interface AuditActorMapper {
    /**
     * Maps the standard subject types and retains the subject ID for unknown types.
     *
     * @return the standard actor mapper
     */
    static AuditActorMapper standard() {
        return subject -> {
            Subject value = Objects.requireNonNull(subject, "subject");
            AuditActor.Kind kind = switch (value.getType().toLowerCase(Locale.ROOT)) {
                case "user" -> AuditActor.Kind.USER;
                case "service" -> AuditActor.Kind.SERVICE;
                case "system" -> AuditActor.Kind.SYSTEM;
                case "anonymous" -> AuditActor.Kind.ANONYMOUS;
                default -> AuditActor.Kind.UNKNOWN;
            };
            return AuditActor.of(kind, value.getId());
        };
    }

    /**
     * Maps one authenticated subject.
     *
     * @param subject the authenticated subject
     * @return the actor to record
     */
    AuditActor map(Subject subject);
}
