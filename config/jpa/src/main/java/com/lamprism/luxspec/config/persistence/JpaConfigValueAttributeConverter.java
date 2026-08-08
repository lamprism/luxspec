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

package com.lamprism.luxspec.config.persistence;

import com.lamprism.luxspec.config.source.RawConfigValue;
import com.lamprism.luxspec.config.source.RawListValue;
import com.lamprism.luxspec.config.source.RawScalarValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.PersistenceException;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts Config raw values to and from the internal JSON payload stored by Config JPA.
 *
 * <p>Scalar representations are stored as text so integer range and decimal scale are not changed
 * by a database JSON or JDBC implementation. Invalid database payloads become {@code null}; the
 * owning entity can then expose an invalid {@code ConfigEntry} without aborting entity loading.</p>
 *
 * @author RollW
 */
@Converter
public final class JpaConfigValueAttributeConverter implements AttributeConverter<RawConfigValue, String> {
    private static final String KIND_FIELD = "kind";
    private static final String VALUE_FIELD = "value";
    private static final String VALUES_FIELD = "values";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable RawConfigValue attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(toNode(attribute));
        } catch (JacksonException exception) {
            throw new PersistenceException("Could not encode the Config value", exception);
        }
    }

    @Override
    public @Nullable RawConfigValue convertToEntityAttribute(@Nullable String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return fromNode(JSON_MAPPER.readTree(dbData));
        } catch (JacksonException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static ObjectNode toNode(RawConfigValue rawValue) {
        if (rawValue instanceof RawListValue listValue) {
            return listNode(listValue);
        }
        if (rawValue instanceof RawScalarValue scalarValue) {
            return scalarNode(scalarValue);
        }
        throw new IllegalArgumentException("Unsupported raw configuration value type: "
                + rawValue.getClass().getName());
    }

    private static ObjectNode listNode(RawListValue rawValue) {
        ObjectNode node = JSON_MAPPER.createObjectNode();
        node.put(KIND_FIELD, RawConfigValue.Kind.LIST.name());
        ArrayNode values = node.putArray(VALUES_FIELD);
        for (RawConfigValue element : rawValue.requireList()) {
            values.add(toNode(element));
        }
        return node;
    }

    private static ObjectNode scalarNode(RawScalarValue rawValue) {
        ObjectNode node = JSON_MAPPER.createObjectNode();
        node.put(KIND_FIELD, rawValue.getScalarKind().name());
        node.put(VALUE_FIELD, scalarText(rawValue));
        return node;
    }

    private static RawConfigValue fromNode(JsonNode node) {
        ObjectNode objectNode = objectNode(node);
        String kind = requiredText(objectNode, KIND_FIELD);
        if (RawConfigValue.Kind.LIST.name().equals(kind)) {
            return listValue(objectNode);
        }
        return scalarValue(objectNode, scalarKind(kind));
    }

    private static RawConfigValue listValue(ObjectNode node) {
        JsonNode valuesNode = node.get(VALUES_FIELD);
        if (valuesNode == null || !valuesNode.isArray()) {
            throw invalidPayload("List value has no values array");
        }
        List<RawConfigValue> values = new ArrayList<>();
        for (JsonNode valueNode : valuesNode) {
            values.add(fromNode(valueNode));
        }
        return RawConfigValue.list(values);
    }

    private static RawConfigValue scalarValue(ObjectNode node, RawConfigValue.ScalarKind kind) {
        String value = requiredText(node, VALUE_FIELD);
        return switch (kind) {
            case STRING -> RawConfigValue.string(value);
            case BOOLEAN -> RawConfigValue.booleanValue(parseBoolean(value));
            case INTEGER -> RawConfigValue.integer(parseLong(value));
            case DECIMAL -> RawConfigValue.decimal(parseDecimal(value));
        };
    }

    private static String scalarText(RawScalarValue rawValue) {
        return switch (rawValue.getScalarKind()) {
            case STRING -> rawValue.requireString();
            case BOOLEAN -> Boolean.toString(rawValue.requireBoolean());
            case INTEGER -> Long.toString(rawValue.requireInteger());
            case DECIMAL -> rawValue.requireDecimal().toString();
        };
    }

    private static ObjectNode objectNode(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            return objectNode;
        }
        throw invalidPayload("Value must be a JSON object");
    }

    private static String requiredText(ObjectNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || !field.isString()) {
            throw invalidPayload("Value field is missing or not a string: " + fieldName);
        }
        return field.stringValue();
    }

    private static RawConfigValue.ScalarKind scalarKind(String value) {
        try {
            return RawConfigValue.ScalarKind.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalidPayload("Unsupported value kind: " + value, exception);
        }
    }

    private static boolean parseBoolean(String value) {
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw invalidPayload("Boolean value is invalid");
        };
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw invalidPayload("Integer value is invalid", exception);
        }
    }

    private static BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw invalidPayload("Decimal value is invalid", exception);
        }
    }

    private static IllegalArgumentException invalidPayload(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException invalidPayload(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }
}
