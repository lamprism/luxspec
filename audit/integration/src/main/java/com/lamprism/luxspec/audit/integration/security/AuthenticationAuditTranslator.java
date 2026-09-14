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

import com.lamprism.luxspec.audit.AuditAction;
import com.lamprism.luxspec.audit.AuditEntryContent;
import com.lamprism.luxspec.audit.AuditEnvelope;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.publish.AuditEventDefinition;
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.security.authentication.AuthenticationEvent;

/**
 * Translates authentication attempts without retaining credentials.
 *
 * @author RollW
 */
public class AuthenticationAuditTranslator implements AuditEventTranslator<AuthenticationEvent> {
    /**
     * Creates the authentication definition owned by this translator.
     *
     * @return the authentication definition
     */
    public static AuditEventDefinition<AuthenticationEvent> definition() {
        return AuditEventDefinition.of(
                SecurityAudit.AUTHENTICATION,
                AuthenticationEvent.class,
                new AuthenticationAuditTranslator()
        );
    }

    @Override
    public AuditEntryContent translate(AuditEnvelope<AuthenticationEvent> envelope) {
        AuthenticationEvent event = envelope.event();
        AuditFieldSet.Builder fields = AuditFieldSet.builder()
                .put(SecurityAudit.SECURITY_CREDENTIAL_TYPE, event.getCredentialType())
                .put(SecurityAudit.SECURITY_DURATION_MILLIS, event.getDuration().toMillis());
        SubjectAuditProjection.putSubject(fields, event.getSubject());
        if (event.getErrorCode() != null) {
            fields.put(SecurityAudit.SECURITY_REASON_CODE, event.getErrorCode().getCode());
        }
        return AuditEntryContent.of(
                event.getOccurredAt(),
                AuditAction.of("security.authentication"),
                event.isSuccessful() ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE,
                null,
                fields.build()
        );
    }
}
