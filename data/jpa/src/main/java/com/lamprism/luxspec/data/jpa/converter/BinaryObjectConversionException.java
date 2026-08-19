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

package com.lamprism.luxspec.data.jpa.converter;

import jakarta.persistence.PersistenceException;

import java.util.Objects;

/**
 * Indicates that a versioned binary object cannot be safely converted.
 *
 * @author RollW
 */
public final class BinaryObjectConversionException extends PersistenceException {
    private final Reason reason;

    /**
     * Creates a conversion failure without an implementation cause.
     *
     * @param reason the stable conversion failure category
     */
    public BinaryObjectConversionException(Reason reason) {
        super(message(reason));
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /**
     * Creates a conversion failure with an implementation cause.
     *
     * @param reason the stable conversion failure category
     * @param cause  the underlying conversion failure
     */
    public BinaryObjectConversionException(Reason reason, Throwable cause) {
        super(message(reason), Objects.requireNonNull(cause, "cause"));
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /**
     * Returns the stable conversion failure category.
     *
     * @return the conversion failure category
     */
    public Reason getReason() {
        return reason;
    }

    private static String message(Reason reason) {
        return "Binary object conversion failed: " + Objects.requireNonNull(reason, "reason").name();
    }

    /**
     * Classifies binary object conversion failures without exposing stored content.
     */
    public enum Reason {
        EMPTY_VALUE,
        INCOMPLETE_HEADER,
        UNSUPPORTED_FORMAT,
        DECOMPRESSION_FAILED,
        DESERIALIZATION_FAILED,
        SERIALIZATION_FAILED
    }
}
