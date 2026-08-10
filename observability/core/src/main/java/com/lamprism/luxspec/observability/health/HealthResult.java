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

import java.util.Objects;

/**
 * Immutable provider-independent health result.
 *
 * @author RollW
 */
public final class HealthResult {
    private final HealthStatus status;
    private final HealthDetailSet details;

    private HealthResult(HealthStatus status, HealthDetailSet details) {
        this.status = status;
        this.details = details;
    }

    public static HealthResult of(HealthStatus status) {
        return new HealthResult(Objects.requireNonNull(status, "status"), HealthDetailSet.empty());
    }

    public static HealthResult of(HealthStatus status, HealthDetailSet details) {
        return new HealthResult(
                Objects.requireNonNull(status, "status"),
                Objects.requireNonNull(details, "details")
        );
    }

    public HealthStatus status() {
        return status;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public HealthDetailSet details() {
        return details;
    }

    public HealthDetailSet getDetails() {
        return details;
    }
}
