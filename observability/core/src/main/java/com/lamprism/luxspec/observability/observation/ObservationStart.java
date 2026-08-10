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

package com.lamprism.luxspec.observability.observation;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable complete input for a complex Observation start.
 *
 * <p>A root observation cannot have a parent. Attributes and links are copied
 * into immutable collections when the start input is built.
 *
 * @author RollW
 */
public final class ObservationStart {
    private final ObservationSpec spec;
    private final ObservationAttributeSet attributes;
    private final @Nullable Observation parent;
    private final boolean root;
    private final List<ObservationLink> links;

    private ObservationStart(Builder builder) {
        spec = builder.spec;
        attributes = builder.attributes;
        parent = builder.parent;
        root = builder.root;
        links = List.copyOf(builder.links);
    }

    public static Builder builder(ObservationSpec spec) {
        return new Builder(spec);
    }

    public ObservationSpec spec() {
        return spec;
    }

    public ObservationSpec getSpec() {
        return spec;
    }

    public ObservationAttributeSet attributes() {
        return attributes;
    }

    public ObservationAttributeSet getAttributes() {
        return attributes;
    }

    public @Nullable Observation parent() {
        return parent;
    }

    public @Nullable Observation getParent() {
        return parent;
    }

    public boolean isRoot() {
        return root;
    }

    public List<ObservationLink> links() {
        return links;
    }

    /**
     * Builds a complete immutable start input.
     */
    public static final class Builder {
        private final ObservationSpec spec;
        private ObservationAttributeSet attributes = ObservationAttributeSet.empty();
        private @Nullable Observation parent;
        private boolean root;
        private final List<ObservationLink> links = new ArrayList<>();

        private Builder(ObservationSpec spec) {
            this.spec = Objects.requireNonNull(spec, "spec");
        }

        public Builder attributes(ObservationAttributeSet attributes) {
            this.attributes = Objects.requireNonNull(attributes, "attributes");
            return this;
        }

        public Builder parent(@Nullable Observation parent) {
            if (root && parent != null) {
                throw new IllegalStateException("A root observation cannot have a parent");
            }
            this.parent = parent;
            return this;
        }

        public Builder root() {
            if (parent != null) {
                throw new IllegalStateException("A root observation cannot have a parent");
            }
            root = true;
            return this;
        }

        public Builder link(ObservationLink link) {
            links.add(Objects.requireNonNull(link, "link"));
            return this;
        }

        public ObservationStart build() {
            return new ObservationStart(this);
        }
    }
}
