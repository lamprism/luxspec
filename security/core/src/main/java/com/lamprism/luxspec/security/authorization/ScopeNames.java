package com.lamprism.luxspec.security.authorization;

final class ScopeNames {
    private ScopeNames() {
    }

    static void requireCanonical(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Scope name must not be empty");
        }
        boolean segmentStart = true;
        boolean previousHyphen = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == ':') {
                if (segmentStart || previousHyphen) {
                    throw new IllegalArgumentException("Scope name contains an invalid segment");
                }
                segmentStart = true;
                continue;
            }
            if (segmentStart) {
                if (character < 'a' || character > 'z') {
                    throw new IllegalArgumentException("Scope segments must start with a lowercase letter");
                }
                segmentStart = false;
                continue;
            }
            if (character == '-') {
                if (previousHyphen) {
                    throw new IllegalArgumentException("Scope name must not contain consecutive hyphens");
                }
                previousHyphen = true;
                continue;
            }
            if ((character < 'a' || character > 'z') && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("Scope name contains an unsupported character");
            }
            previousHyphen = false;
        }
        if (segmentStart || previousHyphen) {
            throw new IllegalArgumentException("Scope name must not end with a separator or hyphen");
        }
    }
}
