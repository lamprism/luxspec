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

import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Reads cumulative count and duration values from an application-owned source.
 *
 * @author RollW
 */
public interface FunctionTimer extends Metric {
    @Nullable
    Long count();

    @Nullable
    Duration totalTime();
}
