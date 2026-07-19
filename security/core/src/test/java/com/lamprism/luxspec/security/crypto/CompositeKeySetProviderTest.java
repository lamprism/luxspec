package com.lamprism.luxspec.security.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class CompositeKeySetProviderTest {
    @Test
    void combinesOneSigningProviderWithVerificationOnlyProviders() {
        Key activeKey = key("active-key-material");
        Key verificationKey = key("verification-key-material");
        KeySetProvider signingProvider = keySetName -> KeySet.withActiveKey(
                "active",
                List.of(new KeyEntry("active", activeKey, activeKey))
        );
        KeySetProvider verificationProvider = keySetName -> KeySet.forVerification(
                List.of(new KeyEntry("remote", verificationKey))
        );

        KeySet keySet = new CompositeKeySetProvider(List.of(signingProvider, verificationProvider)).get("access");

        assertEquals("active", keySet.getActiveKey().orElseThrow().getId());
        assertSame(verificationKey, keySet.findById("remote").orElseThrow().getVerificationKey());
    }

    @Test
    void rejectsDuplicateKeyIdsAcrossProviders() {
        Key firstKey = key("first-key-material");
        Key secondKey = key("second-key-material");
        KeySetProvider firstProvider = keySetName -> KeySet.withActiveKey(
                "shared",
                List.of(new KeyEntry("shared", firstKey, firstKey))
        );
        KeySetProvider secondProvider = keySetName -> KeySet.forVerification(
                List.of(new KeyEntry("shared", secondKey))
        );
        CompositeKeySetProvider provider = new CompositeKeySetProvider(List.of(firstProvider, secondProvider));

        assertThrows(IllegalArgumentException.class, () -> provider.get("access"));
    }

    @Test
    void rejectsMultipleActiveSigningKeys() {
        Key firstKey = key("first-active-key-material");
        Key secondKey = key("second-active-key-material");
        KeySetProvider firstProvider = keySetName -> KeySet.withActiveKey(
                "first",
                List.of(new KeyEntry("first", firstKey, firstKey))
        );
        KeySetProvider secondProvider = keySetName -> KeySet.withActiveKey(
                "second",
                List.of(new KeyEntry("second", secondKey, secondKey))
        );
        CompositeKeySetProvider provider = new CompositeKeySetProvider(List.of(firstProvider, secondProvider));

        assertThrows(IllegalArgumentException.class, () -> provider.get("access"));
    }

    private static Key key(String value) {
        return new SecretKeySpec(value.getBytes(StandardCharsets.US_ASCII), "HmacSHA256");
    }
}
