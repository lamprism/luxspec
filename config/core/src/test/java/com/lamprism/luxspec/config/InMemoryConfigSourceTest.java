package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.source.ConfigEntry;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import com.lamprism.luxspec.config.source.ConfigSourceScope;
import com.lamprism.luxspec.config.source.InMemoryConfigSource;
import com.lamprism.luxspec.config.source.RawConfigValue;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InMemoryConfigSourceTest {
    private static final ConfigKey KEY = ConfigKey.of("sample.value");

    @Test
    void storesAndRemovesRawEntries() {
        InMemoryConfigSource source = InMemoryConfigSource.empty(
                ConfigSourceId.of("memory"),
                ConfigSourceScope.RUNTIME
        );

        assertSame(ConfigEntry.absent(), source.get(KEY));

        source.set(KEY, RawConfigValue.integer(42));
        assertEquals(ConfigEntry.State.PRESENT, source.get(KEY).getState());
        assertEquals(42L, source.get(KEY).requireRawValue().requireInteger());

        source.remove(KEY);
        assertSame(ConfigEntry.absent(), source.get(KEY));
    }

    @Test
    void supportsTombstonesWithoutExposingTheMutableMap() {
        InMemoryConfigSource source = new InMemoryConfigSource(
                ConfigSourceId.of("memory"),
                ConfigSourceScope.BOOTSTRAP,
                Map.of(ConfigKey.of("placeholder"), ConfigEntry.absent()),
                Map.of("origin", "test")
        );

        source.writeTombstone(KEY);

        assertEquals(ConfigEntry.State.TOMBSTONE, source.get(KEY).getState());
        assertEquals("test", source.getAttributes().get("origin"));
    }
}
