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
import com.lamprism.luxspec.audit.AuditFieldSet;
import com.lamprism.luxspec.audit.AuditOutcome;
import com.lamprism.luxspec.audit.integration.LuxspecAuditFields;
import com.lamprism.luxspec.resource.ResourceReference;
import com.lamprism.luxspec.resource.ResourceType;
import com.lamprism.luxspec.user.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared safe user resource and role projections for user audit translators.
 *
 * @author RollW
 */
final class UserAuditTranslationSupport {
    private static final ResourceType<Long> USER = ResourceType.of("user", Long.class);

    private UserAuditTranslationSupport() {
    }

    static AuditEntryContent userEntry(String action, long userId, AuditFieldSet fields) {
        return AuditEntryContent.of(
                AuditAction.of(action),
                AuditOutcome.SUCCESS,
                new ResourceReference<>(USER, userId),
                fields,
                null
        );
    }

    static AuditFieldSet userIdFields(long userId) {
        return AuditFieldSet.builder()
                .put(LuxspecAuditFields.USER_ID, userId)
                .build();
    }

    static String roleNames(Iterable<Role> roles) {
        List<String> names = new ArrayList<>();
        for (Role role : roles) {
            names.add(role.name());
        }
        names.sort(String::compareTo);
        return String.join(",", names);
    }
}
