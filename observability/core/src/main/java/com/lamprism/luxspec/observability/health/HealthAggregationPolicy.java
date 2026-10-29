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

package com.lamprism.luxspec.observability.health;

import java.util.Map;

/**
 * Selects the aggregate status for a health group and its named contributor results.
 *
 * @author RollW
 */
@FunctionalInterface
public interface HealthAggregationPolicy {
    /**
     * Uses the most severe result and treats an empty group as UP.
     *
     * @return the standard aggregation policy
     */
    static HealthAggregationPolicy worstStatus() {
        return (group, results) -> {
            HealthStatus status = HealthStatus.UP;
            for (HealthResult result : results.values()) {
                if (severity(result.getStatus()) > severity(status)) {
                    status = result.getStatus();
                }
            }
            return status;
        };
    }

    /**
     * Selects a status from an immutable snapshot of named results.
     *
     * @param group   the evaluated group
     * @param results the contributor results, including an empty group when applicable
     * @return the aggregate status
     */
    HealthStatus aggregate(HealthGroup group, Map<String, HealthResult> results);

    private static int severity(HealthStatus status) {
        return switch (status) {
            case UP -> 0;
            case UNKNOWN -> 1;
            case OUT_OF_SERVICE -> 2;
            case DOWN -> 3;
        };
    }
}
