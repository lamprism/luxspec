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
import com.lamprism.luxspec.security.firewall.FirewallRuleFailureEvent;

/**
 * Translates an unexpected firewall rule failure.
 *
 * @author RollW
 */
public class FirewallRuleFailureAuditTranslator implements AuditEventTranslator<FirewallRuleFailureEvent> {
    /**
     * Creates the failed firewall rule definition owned by this translator.
     *
     * @return the failed firewall rule definition
     */
    public static AuditEventDefinition<FirewallRuleFailureEvent> definition() {
        return AuditEventDefinition.of(
                SecurityAudit.FIREWALL_RULE_FAILED,
                FirewallRuleFailureEvent.class,
                new FirewallRuleFailureAuditTranslator()
        );
    }

    @Override
    public AuditEntryContent translate(AuditEnvelope<FirewallRuleFailureEvent> envelope) {
        FirewallRuleFailureEvent event = envelope.event();
        AuditFieldSet fields = AuditFieldSet.builder()
                .put(SecurityAudit.SECURITY_RULE_TYPE, event.getRuleType())
                .put(SecurityAudit.SECURITY_REASON_CODE, event.getReasonCode().getCode())
                .build();
        return AuditEntryContent.of(
                event.getOccurredAt(),
                AuditAction.of("security.firewall.evaluate"),
                AuditOutcome.FAILURE,
                null,
                fields
        );
    }
}
