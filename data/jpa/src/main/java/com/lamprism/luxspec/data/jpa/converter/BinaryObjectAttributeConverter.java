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

package com.lamprism.luxspec.data.jpa.converter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import jakarta.persistence.AttributeConverter;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.cbor.CBORMapper;

import java.util.Arrays;

/**
 * Converts typed objects to versioned CBOR data compressed with Zstandard.
 *
 * <p>Concrete entity converters should extend this class and declare their own JPA
 * {@code @Converter}. Stored values begin with a four-byte format header.</p>
 *
 * @param <T> the converted object type
 * @author RollW
 */
public abstract class BinaryObjectAttributeConverter<T> implements AttributeConverter<T, byte[]> {
    private static final int HEADER_LENGTH = 4;

    private static final CBORMapper CBOR_MAPPER = CBORMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(
                    JsonInclude.Include.NON_NULL))
            .build();

    /**
     * Creates a converter using the library's immutable CBOR mapper.
     */
    protected BinaryObjectAttributeConverter() {
    }

    @Override
    public @Nullable byte[] convertToDatabaseColumn(@Nullable T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] serialized = CBOR_MAPPER.writeValueAsBytes(attribute);
            byte[] compressed = Zstd.compress(serialized);
            byte[] result = Arrays.copyOf(Version.V1.header, HEADER_LENGTH + compressed.length);
            System.arraycopy(compressed, 0, result, HEADER_LENGTH, compressed.length);
            return result;
        } catch (JacksonException | ZstdException exception) {
            throw new BinaryObjectConversionException(
                    BinaryObjectConversionException.Reason.SERIALIZATION_FAILED,
                    exception
            );
        }
    }

    @Override
    public @Nullable T convertToEntityAttribute(@Nullable byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData.length == 0) {
            throw new BinaryObjectConversionException(BinaryObjectConversionException.Reason.EMPTY_VALUE);
        }
        if (dbData.length < HEADER_LENGTH) {
            throw new BinaryObjectConversionException(BinaryObjectConversionException.Reason.INCOMPLETE_HEADER);
        }

        byte[] header = Arrays.copyOf(dbData, HEADER_LENGTH);
        Version version = Version.fromHeader(header);
        if (version == null) {
            throw new BinaryObjectConversionException(BinaryObjectConversionException.Reason.UNSUPPORTED_FORMAT);
        }

        byte[] compressedData = Arrays.copyOfRange(dbData, HEADER_LENGTH, dbData.length);
        byte[] decompressedData = decompress(compressedData);
        try {
            return CBOR_MAPPER.readValue(decompressedData, getValueType());
        } catch (JacksonException exception) {
            throw new BinaryObjectConversionException(
                    BinaryObjectConversionException.Reason.DESERIALIZATION_FAILED,
                    exception
            );
        }
    }

    private static byte[] decompress(byte[] compressedData) {
        try {
            return Zstd.decompress(compressedData);
        } catch (ZstdException exception) {
            throw new BinaryObjectConversionException(
                    BinaryObjectConversionException.Reason.DECOMPRESSION_FAILED,
                    exception
            );
        }
    }

    /**
     * Returns the target class used for deserialization.
     *
     * @return the target class
     */
    protected abstract Class<T> getValueType();

    /**
     * Identifies the binary storage format version.
     */
    protected enum Version {
        /**
         * The initial CBOR and Zstandard storage format.
         */
        V1(new byte[]{0x00, 0x00, 0x00, 0x01});

        private final byte[] header;

        Version(byte[] header) {
            this.header = header;
        }

        private static @Nullable Version fromHeader(byte[] header) {
            for (Version version : values()) {
                if (Arrays.equals(version.header, header)) {
                    return version;
                }
            }
            return null;
        }
    }
}
