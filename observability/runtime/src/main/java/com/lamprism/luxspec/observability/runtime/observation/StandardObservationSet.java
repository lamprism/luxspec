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

package com.lamprism.luxspec.observability.runtime.observation;

import com.lamprism.luxspec.observability.observation.ObservationRegistry;
import com.lamprism.luxspec.observability.observation.ObservationSpec;
import com.lamprism.luxspec.observability.runtime.ObservabilitySet;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Selectively registers the standard operation observations without enabling instrumentation by
 * itself.
 *
 * @author RollW
 */
public class StandardObservationSet implements ObservabilitySet {
    private final Set<Domain> domains;

    private StandardObservationSet(Set<Domain> domains) {
        this.domains = Set.copyOf(domains);
    }

    /**
     * Creates a builder with no domains selected.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a set containing every built-in domain.
     *
     * @return the complete standard observation set
     */
    public static StandardObservationSet all() {
        return builder().all().build();
    }

    @Override
    public void registerObservations(ObservationRegistry registry) {
        ObservationRegistry nonNullRegistry = Objects.requireNonNull(registry, "registry");
        for (Domain domain : domains) {
            for (ObservationSpec spec : domain.specs()) {
                nonNullRegistry.register(spec);
            }
        }
    }

    /**
     * Built-in operation domains.
     */
    public enum Domain {
        WEB,
        DATABASE,
        SECURITY,
        CONFIG,
        AUDIT,
        USER;

        private ObservationSpec[] specs() {
            return switch (this) {
                case WEB -> new ObservationSpec[]{StandardObservationCatalog.WEB_REQUEST};
                case DATABASE -> new ObservationSpec[]{StandardObservationCatalog.DATABASE_OPERATION};
                case SECURITY -> new ObservationSpec[]{
                        StandardObservationCatalog.SECURITY_AUTHENTICATION,
                        StandardObservationCatalog.SECURITY_AUTHORIZATION,
                        StandardObservationCatalog.SECURITY_TOKEN
                };
                case CONFIG -> new ObservationSpec[]{
                        StandardObservationCatalog.CONFIG_READ,
                        StandardObservationCatalog.CONFIG_WRITE
                };
                case AUDIT -> new ObservationSpec[]{StandardObservationCatalog.AUDIT_PUBLISH};
                case USER -> new ObservationSpec[]{
                        StandardObservationCatalog.USER_REGISTER,
                        StandardObservationCatalog.USER_AUTHENTICATION,
                        StandardObservationCatalog.USER_PASSWORD_CHANGE,
                        StandardObservationCatalog.USER_STATUS_CHANGE
                };
            };
        }
    }

    /**
     * Builds a selected standard observation set.
     */
    public static class Builder {
        private final EnumSet<Domain> domains = EnumSet.noneOf(Domain.class);

        /**
         * Adds one domain.
         *
         * @param domain the domain to select
         * @return this builder
         */
        public Builder domain(Domain domain) {
            domains.add(Objects.requireNonNull(domain, "domain"));
            return this;
        }

        /**
         * Adds every built-in domain.
         *
         * @return this builder
         */
        public Builder all() {
            domains.addAll(EnumSet.allOf(Domain.class));
            return this;
        }

        /**
         * Creates the immutable set.
         *
         * @return the selected set
         */
        public StandardObservationSet build() {
            return new StandardObservationSet(domains);
        }
    }
}
