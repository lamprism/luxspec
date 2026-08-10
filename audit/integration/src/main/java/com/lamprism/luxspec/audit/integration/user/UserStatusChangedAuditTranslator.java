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

package com.lamprism.luxspec.audit.integration.user;

import com.lamprism.luxspec.audit.AuditEntryContent;
import com.lamprism.luxspec.audit.AuditEnvelope;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.integration.LuxspecAuditFields;
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.user.lifecycle.UserStatusChangedEvent;

/**
 * Translates a user status mutation.
 *
 * @author RollW
 */
public final class UserStatusChangedAuditTranslator implements AuditEventTranslator<UserStatusChangedEvent> {
    @Override
    public AuditEntryContent translate(AuditEnvelope<UserStatusChangedEvent> envelope) {
        UserStatusChangedEvent event = envelope.event();
        AuditFieldSet fields = AuditFieldSet.builder()
                .put(LuxspecAuditFields.USER_ID, event.getUserId())
                .put(LuxspecAuditFields.USER_STATUS, event.getStatus().name())
                .build();
        return UserAuditTranslationSupport.userEntry("user.status.change", event.getUserId(), fields);
    }
}
