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

package com.lamprism.luxspec.message.spring;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpringMessageResolverTest {
    @Test
    void resolvesMessagesUsingExplicitLocaleAndArguments() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("welcome", Locale.US, "Welcome, {0}.");
        messageSource.addMessage("welcome", Locale.FRANCE, "Bonjour, {0}.");
        SpringMessageResolver resolver = new SpringMessageResolver(messageSource, "[message unavailable]");

        assertEquals("Welcome, Ada.", resolver.resolve("welcome", Locale.US, "Ada"));
        assertEquals("Bonjour, Ada.", resolver.resolve("welcome", Locale.FRANCE, "Ada"));
    }

    @Test
    void returnsTheConfiguredFallbackForAMissingKey() {
        SpringMessageResolver resolver = new SpringMessageResolver(
                new StaticMessageSource(),
                "[message unavailable]"
        );

        assertEquals(
                "[message unavailable]",
                resolver.resolve("internal.secret.failure", Locale.US)
        );
    }

    @Test
    void rejectsInvalidResolverInputs() {
        StaticMessageSource messageSource = new StaticMessageSource();

        assertThrows(
                IllegalArgumentException.class,
                () -> new SpringMessageResolver(messageSource, " ")
        );
        SpringMessageResolver resolver = new SpringMessageResolver(messageSource, "[message unavailable]");
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(" ", Locale.US));
        assertThrows(NullPointerException.class, () -> resolver.resolve("key", null));
    }
}
