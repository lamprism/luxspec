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

package com.lamprism.luxspec.audit.alert;

import com.lamprism.luxspec.audit.AuditEntry;
import com.lamprism.luxspec.audit.AuditOutcome;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Selects and projects an application-owned alert from an accepted audit entry.
 *
 * <p>Implementations must be thread-safe. Alert data should contain only information needed by
 * the destination; the application owns alert content, correlation, aggregation, and deduplication.</p>
 *
 * @param <A> the application alert type
 * @author RollW
 */
@FunctionalInterface
public interface AuditAlertPolicy<A> {
    /**
     * Selects outcomes and projects only the information needed by the alert destination.
     *
     * @param outcomes   the selected audit outcomes
     * @param projection the application alert projection
     * @param <A>        the application alert type
     * @return the alert policy
     */
    static <A> AuditAlertPolicy<A> forOutcomes(
            Set<AuditOutcome> outcomes,
            Function<? super AuditEntry, ? extends A> projection
    ) {
        Set<AuditOutcome> selected = Set.copyOf(outcomes);
        Function<? super AuditEntry, ? extends A> mapper = Objects.requireNonNull(projection, "projection");
        return entry -> selected.contains(entry.outcome())
                ? Optional.of(mapper.apply(entry)) : Optional.empty();
    }

    /**
     * Evaluates one accepted audit entry.
     *
     * @param entry the accepted entry
     * @return an alert to deliver, or empty
     */
    Optional<A> evaluate(AuditEntry entry);
}
