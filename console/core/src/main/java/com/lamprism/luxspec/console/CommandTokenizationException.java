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

package com.lamprism.luxspec.console;

/**
 * Signals malformed quoting or escaping in a Shell command line.
 *
 * @author RollW
 */
public final class CommandTokenizationException extends Exception {
    /**
     * Zero-based character position where tokenization failed.
     */
    private final int position;

    /**
     * Creates a command line tokenization failure.
     *
     * @param message  failure message
     * @param position zero-based character position of the failure
     */
    public CommandTokenizationException(String message, int position) {
        super(message);
        if (position < 0) {
            throw new IllegalArgumentException("Token position cannot be negative");
        }
        this.position = position;
    }

    /**
     * @return the zero-based character position of the malformed input
     */
    public int getPosition() {
        return position;
    }
}
