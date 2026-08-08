package com.lamprism.luxspec.config;

import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.config.source.RawListValue;
import com.lamprism.luxspec.config.source.RawScalarValue;
import com.lamprism.luxspec.naming.CaseFormat;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCodecsTest {
    @Test
    void rawValuesAreSealedByStructuralKind() {
        assertTrue(RawConfigValue.string("value") instanceof RawScalarValue);
        assertTrue(RawConfigValue.list(List.of("value")) instanceof RawListValue);
    }

    @Test
    void listCodecPreservesElementKindsAndBoundaries() {
        ConfigCodec<List<String>> codec = ConfigCodecs.list(ConfigCodecs.string());
        List<String> values = List.of("one,two", "three");

        RawConfigValue encoded = codec.encode(values);

        assertEquals(RawConfigValue.Kind.LIST, encoded.getKind());
        assertEquals(values, encoded.requireList().stream().map(RawConfigValue::requireString).toList());
        assertEquals(values, codec.decode(encoded));
    }

    @Test
    void scalarCodecRejectsAListValue() {
        assertThrows(
                IllegalStateException.class,
                () -> ConfigCodecs.integer().decode(RawConfigValue.list(List.of("5")))
        );
    }

    @Test
    void integerCodecAcceptsNativeNumbersAndStringNumbers() {
        ConfigCodec<Integer> codec = ConfigCodecs.integer();

        assertEquals(1000, codec.decode(RawConfigValue.integer(1000)));
        assertEquals(1000, codec.decode(RawConfigValue.string("1000")));
    }

    @Test
    void codecsCanInterpretDifferentRawScalarKindsWithoutKnowingTheSource() {
        ConfigCodec<Long> sizeCodec = new ConfigCodec<>() {
            @Override
            public Long decode(@NonNull RawConfigValue rawValue) {
                return switch (rawValue.getScalarKind()) {
                    case INTEGER -> rawValue.requireInteger();
                    case STRING -> decodeSize(rawValue.requireString());
                    case BOOLEAN, DECIMAL -> throw new IllegalArgumentException("Unsupported size scalar");
                };
            }

            @Override
            public RawConfigValue encode(@NonNull Long value) {
                return RawConfigValue.integer(value);
            }

            private long decodeSize(String value) {
                if ("1KB".equals(value)) {
                    return 1024;
                }
                throw new IllegalArgumentException("Unsupported size text");
            }
        };

        assertEquals(1000L, sizeCodec.decode(RawConfigValue.integer(1000)));
        assertEquals(1024L, sizeCodec.decode(RawConfigValue.string("1KB")));
    }

    @Test
    void integerCodecRejectsDecimalSourceValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigCodecs.integer().decode(RawConfigValue.decimal(new BigDecimal("1000.0")))
        );
    }

    @Test
    void durationCodecAcceptsShortOperationalUnitsAndEncodesIso8601() {
        ConfigCodec<Duration> codec = ConfigCodecs.duration();

        assertEquals(Duration.ofMinutes(5), codec.decode(RawConfigValue.string("5m")));
        assertEquals("PT5M", codec.encode(Duration.ofMinutes(5)).requireString());
    }

    @Test
    void enumCodecUsesLowerHyphenByDefault() {
        ConfigCodec<AccessMode> codec = ConfigCodecs.enumValue(AccessMode.class);

        assertEquals("read-only", codec.encode(AccessMode.READ_ONLY).requireString());
        assertEquals(AccessMode.READ_ONLY, codec.decode(RawConfigValue.string("read-only")));
        assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode(RawConfigValue.string("READ_ONLY"))
        );
    }

    @Test
    void enumCodecCanUseExplicitConstants() {
        ConfigCodec<AccessMode> codec = ConfigCodecs.enumValue(AccessMode.values());

        assertEquals("read-only", codec.encode(AccessMode.READ_ONLY).requireString());
        assertEquals(AccessMode.READ_ONLY, codec.decode(RawConfigValue.string("read-only")));
    }

    @Test
    void enumCodecAcceptsAnExplicitCaseFormat() {
        ConfigCodec<AccessMode> codec = ConfigCodecs.enumValue(
                AccessMode.class,
                CaseFormat.LOWER_CAMEL
        );

        assertEquals("readOnly", codec.encode(AccessMode.READ_ONLY).requireString());
        assertEquals(AccessMode.READ_ONLY, codec.decode(RawConfigValue.string("readOnly")));
    }

    @Test
    void enumCodecRejectsAmbiguousConvertedNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigCodecs.enumValue(AccessMode.class, name -> "mode")
        );
    }

    @Test
    void serializedCodecDelegatesTextConversion() {
        ConfigCodec<URI> codec = ConfigCodecs.serialized(URI::create, URI::toString);
        URI value = URI.create("https://example.com/items?limit=10");

        assertEquals(value, codec.decode(codec.encode(value)));
        assertEquals(value.toString(), codec.encode(value).requireString());
    }

    private enum AccessMode {
        READ_ONLY,
        READ_WRITE
    }
}
