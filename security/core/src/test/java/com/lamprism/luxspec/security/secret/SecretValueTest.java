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

package com.lamprism.luxspec.security.secret;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretValueTest {
    @Test
    void copiesAndMasksSecretMaterial() {
        byte[] source = "secret-value".getBytes(StandardCharsets.US_ASCII);
        SecretValue value = SecretValue.of(source);
        source[0] = 'X';

        assertArrayEquals("secret-value".getBytes(StandardCharsets.US_ASCII), value.copy());
        assertEquals("SecretValue[REDACTED]", value.toString());
        assertFalse(value.isDestroyed());
    }

    @Test
    void destroysMaterialAndRejectsLaterReads() {
        SecretValue value = SecretValue.of("secret-value", StandardCharsets.US_ASCII);

        value.close();

        assertTrue(value.isDestroyed());
        assertThrows(IllegalStateException.class, value::copy);
        value.destroy();
    }

    @Test
    void validatesReferencesWithoutInspectingSecretMaterial() {
        SecretReference reference = SecretReference.of("vault://application/signing-key");

        assertEquals("vault://application/signing-key", reference.value());
        assertEquals(reference, SecretReference.of(reference.getValue()));
        assertThrows(IllegalArgumentException.class, () -> SecretReference.of("\n"));
    }
}
