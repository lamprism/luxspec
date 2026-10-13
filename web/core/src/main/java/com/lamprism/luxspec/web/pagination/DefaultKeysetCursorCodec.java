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

package com.lamprism.luxspec.web.pagination;

import com.lamprism.luxspec.data.pagination.KeysetPosition;
import com.lamprism.luxspec.data.query.OrderBy;
import com.lamprism.luxspec.data.query.QueryField;
import com.lamprism.luxspec.web.query.QueryParameterParser;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default signed keyset cursor implementation.
 *
 * <p>Field values use the existing {@link QueryParameterParser} contract. The binary envelope
 * contains no Java class names and never uses Java object
 * serialization. The signature is HMAC-SHA256 and covers the purpose, ordering, and values.</p>
 *
 * @author RollW
 */
public final class DefaultKeysetCursorCodec implements KeysetCursorCodec {
    private static final int MAGIC = 0x4c585343;
    private static final int VERSION = 1;
    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final int MAC_LENGTH = 32;
    private static final int MAX_TOKEN_LENGTH = 8192;
    private static final int MAX_VALUE_LENGTH = 2048;

    private final String purpose;
    private final SecretKey signingKey;
    private final Map<String, FieldCodec<?>> fieldCodecs;

    private DefaultKeysetCursorCodec(
            String purpose,
            SecretKey signingKey,
            Map<String, FieldCodec<?>> fieldCodecs
    ) {
        this.purpose = requirePurpose(purpose);
        this.signingKey = copyKey(signingKey);
        this.fieldCodecs = Map.copyOf(fieldCodecs);
    }

    /**
     * Starts a default signed cursor codec.
     *
     * @param purpose    endpoint-specific purpose
     * @param signingKey the application-managed signing key
     * @return the codec builder
     */
    public static Builder builder(String purpose, SecretKey signingKey) {
        return new Builder(purpose, signingKey);
    }

    @Override
    public String encode(List<? extends OrderBy<?>> orders, KeysetPosition position) {
        List<? extends OrderBy<?>> nonNullOrders = requireCompatibleOrders(orders);
        KeysetPosition nonNullPosition = Objects.requireNonNull(position, "position");
        if (nonNullPosition.values().size() != nonNullOrders.size()) {
            throw new IllegalArgumentException("Cursor values do not match query ordering");
        }
        byte[] encodedPayload = payload(nonNullOrders, nonNullPosition);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(encodedPayload);
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(encodedPayload));
        String token = encoded + "." + signature;
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new IllegalArgumentException("Encoded cursor exceeds the maximum length");
        }
        return token;
    }

    @Override
    public KeysetPosition decode(List<? extends OrderBy<?>> orders, String token) {
        List<? extends OrderBy<?>> nonNullOrders = requireCompatibleOrders(orders);
        String nonNullToken = Objects.requireNonNull(token, "token");
        if (nonNullToken.isBlank() || nonNullToken.length() > MAX_TOKEN_LENGTH) {
            throw invalidCursor();
        }
        int separator = nonNullToken.indexOf('.');
        if (separator < 1 || separator != nonNullToken.lastIndexOf('.')
                || separator == nonNullToken.length() - 1) {
            throw invalidCursor();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(nonNullToken.substring(0, separator));
            byte[] signature = Base64.getUrlDecoder().decode(nonNullToken.substring(separator + 1));
            if (signature.length != MAC_LENGTH
                    || !MessageDigest.isEqual(signature, sign(payload))) {
                throw invalidCursor();
            }
            return decodePayload(nonNullOrders, payload);
        } catch (IllegalArgumentException ignored) {
            throw invalidCursor();
        }
    }

    private byte[] payload(List<? extends OrderBy<?>> orders, KeysetPosition position) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeUTF(purpose);
            output.writeUTF(orderFingerprint(orders));
            output.writeInt(position.values().size());
            for (int index = 0; index < orders.size(); index++) {
                FieldCodec<?> fieldCodec = fieldCodecs.get(orders.get(index).getField().getName());
                String value = fieldCodec.encode(position.values().get(index));
                if (value.length() > MAX_VALUE_LENGTH) {
                    throw new IllegalArgumentException("Cursor value exceeds the maximum length");
                }
                output.writeUTF(value);
            }
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode cursor", exception);
        }
    }

    private KeysetPosition decodePayload(List<? extends OrderBy<?>> orders, byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw invalidCursor();
            }
            if (!purpose.equals(input.readUTF())
                    || !orderFingerprint(orders).equals(input.readUTF())) {
                throw invalidCursor();
            }
            int valueCount = input.readInt();
            if (valueCount != orders.size()) {
                throw invalidCursor();
            }
            List<Object> values = new ArrayList<>(valueCount);
            for (OrderBy<?> order : orders) {
                String raw = input.readUTF();
                if (raw.length() > MAX_VALUE_LENGTH) {
                    throw invalidCursor();
                }
                values.add(fieldCodecs.get(order.getField().getName()).decode(raw));
            }
            if (input.available() != 0) {
                throw invalidCursor();
            }
            return new KeysetPosition(values);
        } catch (IOException | RuntimeException ignored) {
            throw invalidCursor();
        }
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(signingKey);
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign cursor", exception);
        }
    }

    private List<? extends OrderBy<?>> requireCompatibleOrders(List<? extends OrderBy<?>> orders) {
        List<? extends OrderBy<?>> nonNullOrders = requireOrders(orders);
        for (OrderBy<?> order : nonNullOrders) {
            FieldCodec<?> fieldCodec = fieldCodecs.get(order.getField().getName());
            if (fieldCodec == null || !fieldCodec.valueType().equals(order.getField().getValueType())) {
                throw new IllegalArgumentException("Cursor field codec does not match query ordering");
            }
        }
        return nonNullOrders;
    }

    private static List<? extends OrderBy<?>> requireOrders(List<? extends OrderBy<?>> orders) {
        List<? extends OrderBy<?>> nonNullOrders = Objects.requireNonNull(orders, "orders");
        if (nonNullOrders.isEmpty()) {
            throw new IllegalArgumentException("Cursor ordering must not be empty");
        }
        for (OrderBy<?> order : nonNullOrders) {
            Objects.requireNonNull(order, "order");
        }
        return nonNullOrders;
    }

    private static String orderFingerprint(List<? extends OrderBy<?>> orders) {
        StringBuilder fingerprint = new StringBuilder();
        for (OrderBy<?> order : orders) {
            String fieldName = order.getField().getName();
            fingerprint.append(fieldName.length())
                    .append(':')
                    .append(fieldName)
                    .append(':')
                    .append(order.getDirection().name())
                    .append(';');
        }
        return fingerprint.toString();
    }

    private static String requirePurpose(String purpose) {
        String nonNullPurpose = Objects.requireNonNull(purpose, "purpose");
        if (nonNullPurpose.isBlank() || nonNullPurpose.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Cursor purpose must be non-blank and bounded");
        }
        return nonNullPurpose;
    }

    private static SecretKey copyKey(SecretKey signingKey) {
        SecretKey nonNullSigningKey = Objects.requireNonNull(signingKey, "signingKey");
        byte[] encoded = nonNullSigningKey.getEncoded();
        if (encoded == null || encoded.length == 0) {
            throw new IllegalArgumentException("Cursor signing key must expose encoded key material");
        }
        return new SecretKeySpec(encoded.clone(), MAC_ALGORITHM);
    }

    private static IllegalArgumentException invalidCursor() {
        return new IllegalArgumentException("Cursor is invalid");
    }

    /**
     * Builds the default signed cursor codec from existing query parameter parsers.
     */
    public static final class Builder {
        private final String purpose;
        private final SecretKey signingKey;
        private final Map<String, FieldCodec<?>> fieldCodecs = new LinkedHashMap<>();

        private Builder(String purpose, SecretKey signingKey) {
            this.purpose = requirePurpose(purpose);
            this.signingKey = Objects.requireNonNull(signingKey, "signingKey");
        }

        /**
         * Registers the existing query parameter parser and formatter for one ordered query field.
         *
         * @param field  the ordered query field
         * @param parser the query value parser and formatter
         * @param <T>    the field value type
         * @return this builder
         */
        public <T> Builder field(QueryField<T> field, QueryParameterParser<T> parser) {
            QueryField<T> nonNullField = Objects.requireNonNull(field, "field");
            QueryParameterParser<T> nonNullParser = Objects.requireNonNull(parser, "parser");
            FieldCodec<T> fieldCodec = new FieldCodec<>(nonNullField.getValueType(), nonNullParser);
            if (fieldCodecs.putIfAbsent(nonNullField.getName(), fieldCodec) != null) {
                throw new IllegalArgumentException("Cursor field codec is already registered: " + nonNullField.getName());
            }
            return this;
        }

        /**
         * Builds the immutable codec.
         *
         * @return the default signed cursor codec
         */
        public DefaultKeysetCursorCodec build() {
            if (fieldCodecs.isEmpty()) {
                throw new IllegalArgumentException("At least one cursor field codec is required");
            }
            return new DefaultKeysetCursorCodec(purpose, signingKey, fieldCodecs);
        }
    }

    private record FieldCodec<T>(Class<T> valueType, QueryParameterParser<T> parser) {
        private String encode(Object value) {
            Object nonNullValue = Objects.requireNonNull(value, "value");
            T typedValue;
            try {
                typedValue = valueType.cast(nonNullValue);
            } catch (ClassCastException exception) {
                throw new IllegalArgumentException("Cursor value does not match its query field", exception);
            }
            return Objects.requireNonNull(parser.format(typedValue), "encoded value");
        }

        private T decode(String value) {
            T decodedValue = Objects.requireNonNull(parser.parse(value), "decoded value");
            if (!valueType.isInstance(decodedValue)) {
                throw new IllegalArgumentException("Cursor codec returned an incompatible value");
            }
            return decodedValue;
        }
    }
}
