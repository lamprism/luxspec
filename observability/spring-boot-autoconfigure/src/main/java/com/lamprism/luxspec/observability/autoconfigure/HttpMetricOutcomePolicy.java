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

package com.lamprism.luxspec.observability.autoconfigure;

import com.lamprism.luxspec.observability.observation.ObservationOutcome;

import java.util.Objects;

/**
 * Selects a bounded outcome label for a completed HTTP request metric.
 *
 * @author RollW
 */
@FunctionalInterface
public interface HttpMetricOutcomePolicy {
    /**
     * Groups final status codes by their HTTP class.
     *
     * @return the standard status grouping policy
     */
    static HttpMetricOutcomePolicy statusClass() {
        return (status, failure) -> {
            if (status < 100 || status >= 600) {
                return HttpMetricOutcome.UNKNOWN;
            }
            if (status < 200) {
                return HttpMetricOutcome.INFORMATIONAL;
            }
            if (status < 300) {
                return HttpMetricOutcome.SUCCESS;
            }
            if (status < 400) {
                return HttpMetricOutcome.REDIRECTION;
            }
            return status < 500 ? HttpMetricOutcome.CLIENT_ERROR : HttpMetricOutcome.SERVER_ERROR;
        };
    }

    /**
     * Uses an observation policy to classify success and failure while keeping bounded labels.
     *
     * @param policy the observation outcome policy
     * @return a metric policy derived from the observation classification
     */
    static HttpMetricOutcomePolicy fromObservation(HttpObservationOutcomePolicy policy) {
        HttpObservationOutcomePolicy selected = Objects.requireNonNull(policy, "policy");
        return (status, failure) -> {
            ObservationOutcome outcome = Objects.requireNonNull(selected.resolve(status, failure), "observation outcome");
            if (outcome == ObservationOutcome.SUCCESS) {
                return HttpMetricOutcome.SUCCESS;
            }
            if (outcome == ObservationOutcome.ERROR) {
                return status >= 400 && status < 500 ? HttpMetricOutcome.CLIENT_ERROR : HttpMetricOutcome.SERVER_ERROR;
            }
            return HttpMetricOutcome.UNKNOWN;
        };
    }

    /**
     * Resolves the metric label from the final status and observed failure signals.
     *
     * @param status  the final effective status
     * @param failure whether an exception, async error, or timeout was observed
     * @return the bounded outcome
     */
    HttpMetricOutcome resolve(int status, boolean failure);
}
