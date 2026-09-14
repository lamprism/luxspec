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

package com.lamprism.luxspec.message.toml;

import com.lamprism.luxspec.message.MessageResource;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TomlMessageResolverTest {
    @Test
    void resolvesTableKeysAcrossExactLanguageAndFallbackLocales() {
        TomlMessageResolver resolver = TomlMessageResolver.builder(Locale.ENGLISH, "[message unavailable]")
                .file(Locale.ENGLISH, resource("messages/en.toml"))
                .file(Locale.CHINESE, resource("messages/zh.toml"))
                .file(Locale.SIMPLIFIED_CHINESE, resource("messages/zh-CN.toml"))
                .build();

        assertEquals("Simplified denial", resolver.resolve("error.denied", Locale.SIMPLIFIED_CHINESE));
        assertEquals("Chinese denial", resolver.resolve("error.denied", Locale.TRADITIONAL_CHINESE));
        assertEquals("Denied", resolver.resolve("error.denied", Locale.GERMANY));
        assertEquals(
                "Welcome, Ada.",
                resolver.resolve(MessageResource.of("app", "welcome"), Locale.US, "Ada")
        );
    }

    @Test
    void returnsTheConfiguredFallbackForUnknownKeys() {
        TomlMessageResolver resolver = TomlMessageResolver.builder(Locale.ENGLISH, "[message unavailable]")
                .file(Locale.ENGLISH, resource("messages/en.toml"))
                .build();

        assertEquals("[message unavailable]", resolver.resolve("error.missing", Locale.US));
    }

    @Test
    void rejectsInvalidMessageDocuments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> TomlMessageResolver.builder(Locale.ENGLISH, "[message unavailable]")
                        .file(Locale.ENGLISH, resource("messages/invalid.toml"))
                        .build()
        );
    }

    @Test
    void rejectsDuplicateLocaleFiles() {
        TomlMessageResolver.Builder builder = TomlMessageResolver.builder(Locale.ENGLISH, "[message unavailable]")
                .file(Locale.ENGLISH, resource("messages/en.toml"));

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.file(Locale.ENGLISH, resource("messages/zh.toml"))
        );
    }

    private static Path resource(String name) {
        try {
            String resourceName = "com/lamprism/luxspec/message/toml/" + name;
            return Path.of(TomlMessageResolverTest.class.getClassLoader().getResource(resourceName).toURI());
        } catch (Exception exception) {
            throw new IllegalStateException("Test resource is unavailable: " + name, exception);
        }
    }
}
