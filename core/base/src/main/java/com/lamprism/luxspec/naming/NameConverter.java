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

package com.lamprism.luxspec.naming;

import java.util.Objects;

/**
 * Converts a name according to an application-defined rule.
 *
 * <p>The rule is not limited to case conversion. Implementations may normalize
 * separators, apply aliases, add prefixes, or perform any other deterministic
 * name transformation. Use {@link CaseFormat} when a built-in case conversion
 * is sufficient.
 *
 * @author RollW
 */
@FunctionalInterface
public interface NameConverter {
    /**
     * Returns a converter that preserves names.
     *
     * @return the identity converter
     */
    static NameConverter identity() {
        return name -> Objects.requireNonNull(name, "name");
    }

    /**
     * Converts one name.
     *
     * @param name the source name
     * @return the converted name
     */
    String convert(String name);

    /**
     * Composes this converter with another converter.
     *
     * @param next the converter applied after this converter
     * @return the composed converter
     */
    default NameConverter andThen(NameConverter next) {
        NameConverter nonNullNext = Objects.requireNonNull(next, "next");
        return name -> nonNullNext.convert(convert(name));
    }
}
