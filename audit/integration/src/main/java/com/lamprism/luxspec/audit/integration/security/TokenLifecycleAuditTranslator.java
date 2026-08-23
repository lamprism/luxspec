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
import com.lamprism.luxspec.security.token.TokenLifecycleEvent;

import java.util.Locale;

/**
 * Translates token issuance, refresh, rejection, and revocation outcomes
 * without retaining token values or digests.
 *
 * @author RollW
 */
public class TokenLifecycleAuditTranslator implements AuditEventTranslator<TokenLifecycleEvent> {
    @Override
    public AuditEntryContent translate(AuditEnvelope<TokenLifecycleEvent> envelope) {
        TokenLifecycleEvent event = envelope.event();
        AuditFieldSet.Builder fields = AuditFieldSet.builder()
                .put(SecurityAuditFields.SECURITY_TOKEN_OPERATION, event.getOperation().name())
                .put(SecurityAuditFields.SECURITY_TOKEN_RESULT, event.getResult().name())
                .put(SecurityAuditFields.SECURITY_DURATION_MILLIS, event.getDuration().toMillis());
        if (!event.getTokenKinds().isEmpty()) {
            fields.put(
                    SecurityAuditFields.SECURITY_TOKEN_KINDS,
                    String.join(",", event.getTokenKinds().stream().sorted().toList())
            );
        }
        SubjectAuditProjection.putSubject(fields, event.getSubject());
        if (event.getErrorCode() != null) {
            fields.put(SecurityAuditFields.SECURITY_REASON_CODE, event.getErrorCode().getCode());
        }
        AuditOutcome outcome = switch (event.getResult()) {
            case SUCCESS -> AuditOutcome.SUCCESS;
            case REJECTED -> AuditOutcome.DENIED;
            case FAILED -> AuditOutcome.FAILURE;
        };
        String action = "security.token." + event.getOperation().name().toLowerCase(Locale.ROOT);
        return AuditEntryContent.of(
                event.getOccurredAt(),
                AuditAction.of(action),
                outcome,
                null,
                fields.build()
        );
    }
}
