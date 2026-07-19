package com.lamprism.luxspec.user;

final class RoleNames {
    private RoleNames() {
    }

    static void requireCanonical(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Role name must not be empty");
        }
        boolean segmentStart = true;
        boolean previousHyphen = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '-') {
                if (segmentStart || previousHyphen) {
                    throw new IllegalArgumentException("Role name contains an invalid segment");
                }
                previousHyphen = true;
                continue;
            }
            if (segmentStart) {
                if (character < 'a' || character > 'z') {
                    throw new IllegalArgumentException("Role name must start with a lowercase letter");
                }
                segmentStart = false;
                continue;
            }
            if ((character < 'a' || character > 'z') && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("Role name contains an unsupported character");
            }
            previousHyphen = false;
        }
        if (previousHyphen) {
            throw new IllegalArgumentException("Role name must not end with a hyphen");
        }
    }
}
