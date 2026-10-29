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

/**
 * Selects the outcome of a completed HTTP server observation.
 *
 * @author RollW
 */
@FunctionalInterface
public interface HttpObservationOutcomePolicy {
    /**
     * Marks only exceptions, async errors, and timeouts as errors.
     *
     * @return the exception-only policy
     */
    static HttpObservationOutcomePolicy exceptionsOnly() {
        return (status, failure) -> failure ? ObservationOutcome.ERROR : ObservationOutcome.SUCCESS;
    }

    /**
     * Marks HTTP client and server error statuses as errors in addition to failures.
     *
     * @return the HTTP error-status policy
     */
    static HttpObservationOutcomePolicy clientAndServerErrors() {
        return (status, failure) -> failure || status >= 400
                ? ObservationOutcome.ERROR : ObservationOutcome.SUCCESS;
    }

    /**
     * Selects the observation outcome.
     *
     * @param status  the final effective HTTP status
     * @param failure whether an exception, async error, or timeout occurred
     * @return the selected outcome
     */
    ObservationOutcome resolve(int status, boolean failure);
}
