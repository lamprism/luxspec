package com.lamprism.luxspec.security.authorization;

import java.util.Objects;

final class ActionNames {
    private ActionNames() {
    }

    static String normalize(String value) {
        Objects.requireNonNull(value, "name");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Action name must not be empty");
        }
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = true;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '-' || character == '_' || Character.isWhitespace(character)) {
                if (!previousSeparator) {
                    result.append('_');
                    previousSeparator = true;
                }
                continue;
            }
            if ((character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')) {
                result.append(Character.toUpperCase(character));
                previousSeparator = false;
                continue;
            }
            if (character >= '0' && character <= '9') {
                result.append(character);
                previousSeparator = false;
                continue;
            }
            throw new IllegalArgumentException("Action name contains an unsupported character");
        }
        if (result.isEmpty() || result.charAt(0) == '_' || result.charAt(result.length() - 1) == '_') {
            throw new IllegalArgumentException("Action name contains an empty segment");
        }
        return result.toString();
    }
}
