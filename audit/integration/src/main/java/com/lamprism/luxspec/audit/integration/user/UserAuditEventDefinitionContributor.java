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

import com.lamprism.luxspec.audit.publish.AuditEventDefinitionContributor;
import com.lamprism.luxspec.audit.publish.AuditEventDefinitionRegistrar;

import java.util.Objects;

/**
 * Contributes audit definitions owned by user lifecycle integration.
 *
 * @author RollW
 */
public final class UserAuditEventDefinitionContributor implements AuditEventDefinitionContributor {
    @Override
    public void contribute(AuditEventDefinitionRegistrar registrar) {
        AuditEventDefinitionRegistrar nonNullRegistrar = Objects.requireNonNull(registrar, "registrar");
        nonNullRegistrar.register(UserRegisteredAuditTranslator.definition());
        nonNullRegistrar.register(UserRenamedAuditTranslator.definition());
        nonNullRegistrar.register(UserEmailChangedAuditTranslator.definition());
        nonNullRegistrar.register(UserRolesChangedAuditTranslator.definition());
        nonNullRegistrar.register(UserStatusChangedAuditTranslator.definition());
        nonNullRegistrar.register(UserPasswordChangedAuditTranslator.definition());
    }
}
