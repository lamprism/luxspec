package com.lamprism.luxspec.message;

import java.util.Locale;

/**
 * Resolves localized messages without coupling callers to a message provider.
 *
 * @author RollW
 */
public interface MessageResolver {
    /**
     * Resolves a message for an explicit locale.
     *
     * @param key the non-blank message key
     * @param locale the locale used for resolution
     * @param arguments the values used by message placeholders
     * @return the resolved message or the provider's configured safe fallback
     */
    String resolve(String key, Locale locale, Object... arguments);
}
