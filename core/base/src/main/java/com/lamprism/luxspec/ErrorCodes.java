package com.lamprism.luxspec;

import java.util.Objects;

/**
 * Creates validated error-code values for application-defined errors.
 *
 * @author RollW
 */
public final class ErrorCodes {
    private ErrorCodes() {
    }

    /**
     * Creates a canonical application-defined error code.
     *
     * @param code the lowercase colon-separated error code
     * @return the validated error code
     */
    public static ErrorCode of(String code) {
        Objects.requireNonNull(code, "code");
        requireCanonical(code);
        return new Value(code);
    }

    private static void requireCanonical(String code) {
        if (code.isEmpty()) {
            throw new IllegalArgumentException("code must not be empty");
        }
        boolean segmentStart = true;
        for (int index = 0; index < code.length(); index++) {
            char character = code.charAt(index);
            if (character == ':') {
                if (segmentStart) {
                    throw new IllegalArgumentException("code contains an empty segment");
                }
                segmentStart = true;
                continue;
            }
            if ((character < 'a' || character > 'z') && character != '-' && (character < '0' || character > '9')) {
                throw new IllegalArgumentException("code contains an unsupported character");
            }
            segmentStart = false;
        }
        if (segmentStart) {
            throw new IllegalArgumentException("code must not end with a separator");
        }
    }

    private static final class Value implements ErrorCode {
        private final String code;

        private Value(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }
    }
}
