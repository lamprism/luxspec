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

package com.lamprism.luxspec.audit.integration.config;

import com.lamprism.luxspec.audit.AuditAction;
import com.lamprism.luxspec.audit.AuditEntryContent;
import com.lamprism.luxspec.audit.AuditEnvelope;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.config.event.ConfigChangedEvent;

/**
 * Translates effective configuration changes without exposing values.
 *
 * @author RollW
 */
public class ConfigChangedAuditTranslator implements AuditEventTranslator<ConfigChangedEvent> {
    @Override
    public AuditEntryContent translate(AuditEnvelope<ConfigChangedEvent> envelope) {
        ConfigChangedEvent event = envelope.event();
        AuditFieldSet fields = AuditFieldSet.builder()
                .put(ConfigAuditFields.CONFIG_KEY, event.getKey().getValue())
                .put(ConfigAuditFields.CONFIG_SENSITIVE, event.isSensitive())
                .put(ConfigAuditFields.CONFIG_PREVIOUS_STATE, event.getPreviousState().name())
                .put(ConfigAuditFields.CONFIG_CURRENT_STATE, event.getCurrentState().name())
                .put(ConfigAuditFields.CONFIG_PREVIOUS_ORIGIN,
                        ConfigAuditTranslationSupport.origin(event.getPreviousOrigin()))
                .put(ConfigAuditFields.CONFIG_CURRENT_ORIGIN,
                        ConfigAuditTranslationSupport.origin(event.getCurrentOrigin()))
                .build();
        return AuditEntryContent.of(
                envelope.publishedAt(),
                AuditAction.of("config.effective.change"),
                AuditOutcome.SUCCESS,
                ConfigAuditTranslationSupport.configuration(event.getKey()),
                fields
        );
    }
}
