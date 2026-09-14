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

/**
 * Declares a long-task timer.
 *
 * @author RollW
 */
public final class LongTaskTimerSpec extends AbstractMetricSpec<LongTaskTimer> {
    private LongTaskTimerSpec(Builder builder) {
        super(builder.name(), builder.kind(), builder.dimensions(), builder.description(), builder.baseUnit(), builder.cardinalityPolicy());
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Builds a long-task timer specification.
     */
    public static final class Builder extends BuilderSupport<LongTaskTimer, Builder> {
        private Builder(String name) {
            super(name, MetricKind.LONG_TASK_TIMER);
        }

        public LongTaskTimerSpec build() {
            return new LongTaskTimerSpec(this);
        }
    }
}
