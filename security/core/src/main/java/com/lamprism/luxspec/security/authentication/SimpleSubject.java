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

package com.lamprism.luxspec.security.authentication;

import java.util.Objects;

/**
 * A generic immutable subject identified only by stable type and ID values.
 *
 * @param type the stable subject type
 * @param id   the stable identifier within the subject type
 * @author RollW
 */
public record SimpleSubject(String type, String id) implements Subject {
    /**
     * Creates a validated simple subject.
     *
     * @param type the stable subject type
     * @param id   the stable identifier within the subject type
     */
    public SimpleSubject {
        type = requireText(type, "type");
        id = requireText(id, "id");
    }

    /**
     * Copies a subject into its generic immutable identity form.
     *
     * @param subject the source subject
     * @return the generic subject identity
     */
    public static SimpleSubject from(Subject subject) {
        Subject nonNullSubject = Objects.requireNonNull(subject, "subject");
        return new SimpleSubject(nonNullSubject.getType(), nonNullSubject.getId());
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getId() {
        return id;
    }

    private static String requireText(String value, String name) {
        String nonNullValue = Objects.requireNonNull(value, name);
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return nonNullValue;
    }
}
