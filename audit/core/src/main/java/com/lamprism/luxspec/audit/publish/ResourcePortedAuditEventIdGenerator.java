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

package com.lamprism.luxspec.audit.publish;

import com.lamprism.luxspec.audit.AuditEventId;
import com.lamprism.luxspec.resource.ResourceIdGenerator;
import com.lamprism.luxspec.resource.ResourceType;

/**
 * Generates audit event IDs using a resource ID generator.
 *
 * @author RollW
 */
public class ResourcePortedAuditEventIdGenerator implements AuditEventIdGenerator {
    private static final ResourceType<String> AUDIT_EVENT = ResourceType.of("AUDIT_EVENT", String.class);

    private final ResourceIdGenerator<String> resourceIdGenerator;

    public ResourcePortedAuditEventIdGenerator(ResourceIdGenerator<String> resourceIdGenerator) {
        this.resourceIdGenerator = resourceIdGenerator;
    }

    @Override
    public AuditEventId nextId(String eventName) {

        String eventId = resourceIdGenerator.nextId(AUDIT_EVENT);
        return AuditEventId.of(eventId);
    }
}
