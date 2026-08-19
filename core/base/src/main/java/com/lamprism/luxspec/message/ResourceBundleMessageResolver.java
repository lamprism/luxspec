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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Resolves messages from Java resource bundles.
 *
 * @author RollW
 */
public class ResourceBundleMessageResolver implements MessageResolver {
    private final String defaultNamespace;
    private final String missingMessage;

    /**
     * Creates a resolver for explicit message resources.
     *
     * <p>Key-only resolution uses the reserved {@code luxspec.default} namespace and is therefore
     * intended only when an application deliberately provides that bundle.</p>
     *
     * @param missingMessage the non-blank fallback returned for missing resources or keys
     */
    public ResourceBundleMessageResolver(String missingMessage) {
        this("luxspec.default", missingMessage);
    }

    /**
     * Creates a resolver with an explicit default namespace and safe missing-message fallback.
     *
     * @param defaultNamespace the namespace used by key-only resolution
     * @param missingMessage   the non-blank fallback returned for missing resources or keys
     */
    public ResourceBundleMessageResolver(String defaultNamespace, String missingMessage) {
        this.defaultNamespace = requireText(defaultNamespace, "defaultNamespace");
        this.missingMessage = requireText(missingMessage, "missingMessage");
    }

    @Override
    public String resolve(String key, Locale locale, Object... arguments) {
        return resolve(MessageResource.of(defaultNamespace, key), locale, arguments);
    }

    @Override
    public String resolve(MessageResource resource, Locale locale, Object... arguments) {
        MessageResource nonNullResource = Objects.requireNonNull(resource, "resource");
        Locale nonNullLocale = Objects.requireNonNull(locale, "locale");
        Object[] nonNullArguments = Objects.requireNonNull(arguments, "arguments");
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                    nonNullResource.namespace(),
                    nonNullLocale
            );
            String template = bundle.getString(nonNullResource.key());
            return new MessageFormat(template, nonNullLocale).format(nonNullArguments);
        } catch (MissingResourceException | IllegalArgumentException exception) {
            return missingMessage;
        }
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name).trim();
        if (nonNullValue.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
