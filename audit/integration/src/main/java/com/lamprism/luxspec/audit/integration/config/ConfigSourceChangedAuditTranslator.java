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
import com.lamprism.luxspec.audit.integration.LuxspecAuditFields;
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.config.event.ConfigSourceChangedEvent;

import java.util.Locale;

/**
 * Translates source-level configuration mutations without exposing values.
 *
 * @author RollW
 */
public final class ConfigSourceChangedAuditTranslator implements AuditEventTranslator<ConfigSourceChangedEvent> {
    @Override
    public AuditEntryContent translate(AuditEnvelope<ConfigSourceChangedEvent> envelope) {
        ConfigSourceChangedEvent event = envelope.event();
        String action = "config.source." + event.getChangeType().name().toLowerCase(Locale.ROOT);
        AuditFieldSet fields = AuditFieldSet.builder()
                .put(LuxspecAuditFields.CONFIG_KEY, event.getKey().getValue())
                .put(LuxspecAuditFields.CONFIG_SOURCE, event.getSourceId().getValue())
                .put(LuxspecAuditFields.CONFIG_CHANGE_TYPE, event.getChangeType().name())
                .build();
        return AuditEntryContent.of(
                AuditAction.of(action),
                AuditOutcome.SUCCESS,
                ConfigAuditTranslationSupport.configuration(event.getKey()),
                fields,
                null
        );
    }
}
