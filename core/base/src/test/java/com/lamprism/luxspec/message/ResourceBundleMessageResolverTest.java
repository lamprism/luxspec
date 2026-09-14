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

package com.lamprism.luxspec.message;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceBundleMessageResolverTest {
    private static final String BUNDLE = "com.lamprism.luxspec.message.messages";

    @Test
    void resolvesDefaultAndExplicitMessageResources() {
        ResourceBundleMessageResolver resolver = new ResourceBundleMessageResolver(
                BUNDLE,
                "[message unavailable]"
        );

        assertEquals("Welcome, Ada.", resolver.resolve("welcome", Locale.US, "Ada"));
        assertEquals(
                "Bonjour, Ada.",
                resolver.resolve(MessageResource.of(BUNDLE, "welcome"), Locale.FRANCE, "Ada")
        );
    }

    @Test
    void returnsTheConfiguredFallbackForMissingResources() {
        ResourceBundleMessageResolver resolver = new ResourceBundleMessageResolver(
                BUNDLE,
                "[message unavailable]"
        );

        assertEquals("[message unavailable]", resolver.resolve("missing", Locale.US));
        assertEquals(
                "[message unavailable]",
                resolver.resolve(MessageResource.of("missing.bundle", "missing"), Locale.US)
        );
    }

    @Test
    void resolvesResourcesUsingStableNamespaces() {
        ResourceBundleMessageResolver resolver = new ResourceBundleMessageResolver(
                "[message unavailable]"
        );

        assertEquals(
                "Welcome, Ada.",
                resolver.resolve(MessageResource.of(BUNDLE, "welcome"), Locale.US, "Ada")
        );
    }
}
