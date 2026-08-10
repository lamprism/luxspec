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
import com.lamprism.luxspec.audit.integration.LuxspecAuditFields;
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.security.firewall.FirewallRuleDeniedEvent;

/**
 * Translates an ordinary firewall denial.
 *
 * @author RollW
 */
public final class FirewallRuleDeniedAuditTranslator implements AuditEventTranslator<FirewallRuleDeniedEvent> {
    @Override
    public AuditEntryContent translate(AuditEnvelope<FirewallRuleDeniedEvent> envelope) {
        FirewallRuleDeniedEvent event = envelope.event();
        AuditFieldSet.Builder fields = AuditFieldSet.builder()
                .put(LuxspecAuditFields.SECURITY_RULE_TYPE, event.getRuleType())
                .put(LuxspecAuditFields.SECURITY_REASON_CODE, event.getReasonCode().getCode());
        if (event.getRetryAfter() != null) {
            fields.put(LuxspecAuditFields.SECURITY_RETRY_AFTER_MILLIS, event.getRetryAfter().toMillis());
        }
        return AuditEntryContent.of(
                AuditAction.of("security.firewall.evaluate"),
                AuditOutcome.DENIED,
                null,
                fields.build(),
                null
        );
    }
}
