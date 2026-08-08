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
