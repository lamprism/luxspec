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

package com.lamprism.luxspec.security.crypto;

import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author RollW
 */
class FixedKeySetProviderTest {
    @Test
    void returnsTheConfiguredSnapshotAndOriginalKeyObjects() {
        SecretKeySpec key = key("fixed-signing-key-material");
        KeySet keySet = KeySet.withActiveKey(
                "active",
                List.of(new KeyEntry("active", key, key))
        );
        FixedKeySetProvider provider = new FixedKeySetProvider(Map.of("access", keySet));

        KeySet resolved = provider.get("access");

        assertSame(keySet, resolved);
        assertSame(key, resolved.getActiveKey().getSigningKey());
        assertSame(key, resolved.getActiveKey().getVerificationKey());
    }

    @Test
    void rejectsAnUndefinedReference() {
        FixedKeySetProvider provider = new FixedKeySetProvider(Map.of(
                "access", KeySet.forVerification(List.of(new KeyEntry("verify", key("verify-key"))))
        ));

        assertThrows(IllegalArgumentException.class, () -> provider.get("missing"));
    }

    @Test
    void exposesNullForVerificationOnlyState() {
        KeyEntry entry = new KeyEntry("verify", key("verify-key"));
        KeySet keySet = KeySet.forVerification(List.of(entry));

        assertNull(keySet.getActiveKey());
        assertNull(entry.getSigningKey());
    }

    @Test
    void rejectsBlankReferences() {
        KeySet keySet = KeySet.forVerification(List.of(new KeyEntry("verify", key("verify-key"))));

        assertThrows(
                IllegalArgumentException.class,
                () -> new FixedKeySetProvider(Map.of(" ", keySet))
        );
        FixedKeySetProvider provider = new FixedKeySetProvider(Map.of("access", keySet));
        assertThrows(IllegalArgumentException.class, () -> provider.get(" "));
    }

    @Test
    void rejectsAnEmptyProvider() {
        assertThrows(IllegalArgumentException.class, () -> new FixedKeySetProvider(Map.of()));
    }

    private static SecretKeySpec key(String value) {
        return new SecretKeySpec(value.getBytes(StandardCharsets.US_ASCII), "HmacSHA256");
    }
}
