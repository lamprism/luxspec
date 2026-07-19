package com.lamprism.luxspec.message.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

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
