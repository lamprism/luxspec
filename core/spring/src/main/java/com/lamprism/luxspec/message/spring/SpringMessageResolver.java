package com.lamprism.luxspec.message.spring;

import com.lamprism.luxspec.message.MessageResolver;
import java.util.Locale;
import java.util.Objects;
import org.springframework.context.MessageSource;

/**
 * Resolves messages through a Spring {@link MessageSource}.
 *
 * <p>A non-blank fallback is required so a missing message never exposes a provider exception or
 * an internal diagnostic to the caller.</p>
 *
 * @author RollW
 */
public final class SpringMessageResolver implements MessageResolver {
    private final MessageSource messageSource;
    private final String missingMessage;

    /**
     * Creates a resolver with an explicit safe fallback for missing keys.
     *
     * @param messageSource the Spring message provider
     * @param missingMessage the non-blank fallback returned when a key is not found
     */
    public SpringMessageResolver(MessageSource messageSource, String missingMessage) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource");
        this.missingMessage = requireMissingMessage(missingMessage);
    }

    @Override
    public String resolve(String key, Locale locale, Object... arguments) {
        String nonBlankKey = requireKey(key);
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        Object[] nonNullArguments = Objects.requireNonNull(arguments, "arguments");
        return messageSource.getMessage(nonBlankKey, nonNullArguments, missingMessage, nonNullLocale);
    }

    private static String requireKey(String key) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return key;
    }

    private static String requireMissingMessage(String missingMessage) {
        Objects.requireNonNull(missingMessage, "missingMessage");
        if (missingMessage.isBlank()) {
            throw new IllegalArgumentException("missingMessage must not be blank");
        }
        return missingMessage;
    }
}
