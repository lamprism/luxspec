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
import com.lamprism.luxspec.audit.publish.AuditEventTranslator;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.user.lifecycle.UserRolesChangedEvent;
import com.lamprism.luxspec.user.resource.UserResourceTypes;

/**
 * Translates one user role grant or revocation.
 *
 * @author RollW
 */
public class UserRolesChangedAuditTranslator implements AuditEventTranslator<UserRolesChangedEvent> {
    @Override
    public AuditEntryContent translate(AuditEnvelope<UserRolesChangedEvent> envelope) {
        UserRolesChangedEvent event = envelope.event();
        AuditFieldSet fields = AuditFieldSet.builder()
                .put(UserAuditFields.USER_ID, event.getUserId())
                .put(UserAuditFields.USER_ROLE, event.getRole().name())
                .put(UserAuditFields.USER_ROLE_CHANGE, event.getChangeType().name())
                .build();
        String action = event.getChangeType() == UserRolesChangedEvent.ChangeType.GRANTED
                ? "user.role.grant"
                : "user.role.revoke";
        return AuditEntryContent.of(
                event.getOccurredAt(),
                AuditAction.of(action),
                AuditOutcome.SUCCESS,
                new ResourceReference<>(UserResourceTypes.USER, event.getUserId()),
                fields
        );
    }
}
