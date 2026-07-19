package com.lamprism.luxspec.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigCodecsTest {
    @Test
    void listCodecPreservesElementBoundaries() {
        ConfigCodec<List<String>> codec = ConfigCodecs.list(ConfigCodecs.string());
        List<String> values = List.of("one,two", "three");

        RawConfigValue encoded = codec.encode(values);

        assertEquals(RawConfigValue.Kind.LIST, encoded.getKind());
        assertEquals(values, encoded.requireList());
        assertEquals(values, codec.decode(encoded));
    }

    @Test
    void scalarCodecRejectsAListValue() {
        assertThrows(IllegalStateException.class, () -> ConfigCodecs.integer().decode(RawConfigValue.list(List.of("5"))));
    }

    @Test
    void durationCodecAcceptsShortOperationalUnitsAndEncodesIso8601() {
        ConfigCodec<Duration> codec = ConfigCodecs.duration();

        assertEquals(Duration.ofMinutes(5), codec.decode(RawConfigValue.scalar("5m")));
        assertEquals("PT5M", codec.encode(Duration.ofMinutes(5)).requireScalar());
    }
}
