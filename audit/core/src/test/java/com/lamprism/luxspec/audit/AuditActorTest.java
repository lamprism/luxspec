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

package com.lamprism.luxspec.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditActorTest {
    @Test
    void acceptsApplicationDefinedActorKinds() {
        AuditActor.Kind first = AuditActor.Kind.of("TENANT");
        AuditActor.Kind second = AuditActor.Kind.of("TENANT");

        assertEquals("TENANT", first.value());
        assertEquals(first, second);
        assertEquals(
                AuditActor.of(first, "tenant-42"),
                AuditActor.of(second, "tenant-42")
        );
    }
}
