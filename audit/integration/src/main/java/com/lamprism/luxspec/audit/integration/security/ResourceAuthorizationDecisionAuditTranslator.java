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
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.security.authorization.ResourceAuthorizationDecisionEvent;

/**
 * Translates resource authorization decisions while retaining only safe actor
 * and decision metadata.
 *
 * @author RollW
 */
public class ResourceAuthorizationDecisionAuditTranslator
        implements AuditEventTranslator<ResourceAuthorizationDecisionEvent<?>> {
    @Override
    public AuditEntryContent translate(
            AuditEnvelope<ResourceAuthorizationDecisionEvent<?>> envelope
    ) {
        ResourceAuthorizationDecisionEvent<?> event = envelope.event();
        AuditFieldSet.Builder fields = AuditFieldSet.builder()
                .put(SecurityAuditFields.SECURITY_ACTION, event.getAction().getName())
                .put(SecurityAuditFields.SECURITY_DURATION_MILLIS, event.getDuration().toMillis());
        SubjectAuditProjection.putSubject(fields, event.getAuthentication().subject());
        if (event.getDecision().getReasonCode() != null) {
            fields.put(SecurityAuditFields.SECURITY_REASON_CODE, event.getDecision().getReasonCode().getCode());
        }
        return AuditEntryContent.of(
                event.getOccurredAt(),
                AuditAction.of("security.resource.access"),
                event.getDecision().isAllowed() ? AuditOutcome.SUCCESS : AuditOutcome.DENIED,
                event.getReference(),
                fields.build()
        );
    }
}
