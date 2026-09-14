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

package com.lamprism.luxspec.observability.micrometer;

import com.lamprism.luxspec.observability.observation.ObservationLink;
import io.micrometer.observation.Observation;

import java.util.List;
import java.util.Objects;

/**
 * Stores provider-neutral Luxspec links in a Micrometer observation context.
 *
 * <p>Micrometer Observation itself has no portable link operation. Keeping the immutable links in
 * the context preserves them for a tracing-specific handler without converting trace identities
 * into metric tags.</p>
 *
 * @author RollW
 */
public final class MicrometerObservationLinkContext {
    private static final Object KEY = MicrometerObservationLinkContext.class;

    private MicrometerObservationLinkContext() {
    }

    /**
     * Attaches links to a provider observation context.
     *
     * @param context the Micrometer context
     * @param links   the links to preserve
     */
    public static void put(Observation.Context context, List<ObservationLink> links) {
        Observation.Context nonNullContext = Objects.requireNonNull(context, "context");
        nonNullContext.put(KEY, List.copyOf(Objects.requireNonNull(links, "links")));
    }

    /**
     * Reads links previously attached by {@link #put(Observation.Context, List)}.
     *
     * @param context the Micrometer context
     * @return the immutable links, or an empty list when none were attached
     */
    @SuppressWarnings("unchecked")
    public static List<ObservationLink> get(Observation.ContextView context) {
        Observation.ContextView nonNullContext = Objects.requireNonNull(context, "context");
        Object value = nonNullContext.get(KEY);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalStateException("Micrometer observation link context contains an invalid value");
        }
        for (Object item : values) {
            if (!(item instanceof ObservationLink)) {
                throw new IllegalStateException("Micrometer observation link context contains an invalid link");
            }
        }
        return (List<ObservationLink>) values;
    }
}
