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

package com.lamprism.luxspec.observability.metric;

import java.util.Objects;

/**
 * Common metric activation policies.
 *
 * @author RollW
 */
public final class MetricActivations {
    private MetricActivations() {
    }

    public static MetricActivation all() {
        return spec -> true;
    }

    public static MetricActivation byName(MetricName name) {
        MetricName nonNullName = Objects.requireNonNull(name, "name");
        return spec -> spec.getName().equals(nonNullName);
    }
}
