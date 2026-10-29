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

package com.lamprism.luxspec.config.source;

import java.util.List;
import java.util.Objects;

/**
 * Represents an immutable ordered list of raw configuration values.
 *
 * @author RollW
 */
public final class RawListValue implements RawConfigValue {
    private final List<RawConfigValue> elements;

    RawListValue(List<? extends RawConfigValue> elements) {
        this.elements = List.copyOf(Objects.requireNonNull(elements, "elements"));
    }

    @Override
    public Kind getKind() {
        return Kind.LIST;
    }

    @Override
    public List<RawConfigValue> requireList() {
        return elements;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RawListValue value)) {
            return false;
        }
        return elements.equals(value.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Kind.LIST, elements);
    }
}
