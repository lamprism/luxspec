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
import com.lamprism.luxspec.user.Role;
import com.lamprism.luxspec.user.User;
import com.lamprism.luxspec.user.lifecycle.UserRolesReplacedEvent;

/**
 * Translates an atomic user role-set replacement.
 *
 * @author RollW
 */
public final class UserRolesReplacedAuditTranslator implements AuditEventTranslator<UserRolesReplacedEvent> {
    /**
     * Returns the role replacement event definition.
     *
     * @return the audit event definition
     */
    public static AuditEventDefinition<UserRolesReplacedEvent> definition() {
        return AuditEventDefinition.of(
                UserAudit.ROLES_REPLACED, UserRolesReplacedEvent.class, new UserRolesReplacedAuditTranslator()
        );
    }

    @Override
    public AuditEntryContent translate(AuditEnvelope<UserRolesReplacedEvent> envelope) {
        UserRolesReplacedEvent event = envelope.event();
        AuditFieldSet fields = AuditFieldSet.builder()
                .put(UserAudit.USER_ID, event.userId())
                .put(UserAudit.USER_ROLES, String.join(",", event.roles().stream().map(Role::name).sorted().toList()))
                .build();
        return AuditEntryContent.of(
                event.occurredAt(), AuditAction.of("user.roles.replace"), AuditOutcome.SUCCESS,
                new ResourceReference<>(User.RESOURCE_TYPE, event.userId()), fields
        );
    }
}
