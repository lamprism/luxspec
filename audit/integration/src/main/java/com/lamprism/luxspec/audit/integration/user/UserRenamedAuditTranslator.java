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

import com.lamprism.luxspec.audit.AuditAction;
import com.lamprism.luxspec.audit.AuditEntryContent;
import com.lamprism.luxspec.audit.AuditEnvelope;
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.publish.AuditEventDefinition;
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.user.lifecycle.UserRenamedEvent;
import com.lamprism.luxspec.user.resource.UserResourceTypes;

/**
 * Translates a user rename without exposing the new username.
 *
 * @author RollW
 */
public class UserRenamedAuditTranslator implements AuditEventTranslator<UserRenamedEvent> {
    /**
     * Creates the user rename definition owned by this translator.
     *
     * @return the user rename definition
     */
    public static AuditEventDefinition<UserRenamedEvent> definition() {
        return AuditEventDefinition.of(
                UserAuditEventNames.RENAMED,
                UserRenamedEvent.class,
                new UserRenamedAuditTranslator()
        );
    }

    @Override
    public AuditEntryContent translate(AuditEnvelope<UserRenamedEvent> envelope) {
        UserRenamedEvent event = envelope.event();
        AuditFieldSet fields = AuditFieldSet.builder()
                .put(UserAuditFields.USER_ID, event.getUserId())
                .build();
        return AuditEntryContent.of(
                event.getOccurredAt(),
                AuditAction.of("user.rename"),
                AuditOutcome.SUCCESS,
                new ResourceReference<>(UserResourceTypes.USER, event.getUserId()),
                fields
        );
    }
}
