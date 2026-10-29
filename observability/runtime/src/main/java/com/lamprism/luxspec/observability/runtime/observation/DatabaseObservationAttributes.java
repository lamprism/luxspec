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

import com.lamprism.luxspec.observability.observation.ObservationAttributeClassification;
import com.lamprism.luxspec.observability.observation.ObservationAttributeSpec;

/**
 * Bounded database observation attributes that exclude SQL and application values.
 *
 * @author RollW
 */
public final class DatabaseObservationAttributes {
    public static final ObservationAttributeSpec<Operation> OPERATION = ObservationAttributeSpec.enumValue(
            "database.operation.kind", Operation.class, ObservationAttributeClassification.LOW
    );

    /**
     * The type of observed database boundary operation.
     */
    public enum Operation {
        CONNECT,
        QUERY,
        UPDATE,
        BATCH,
        EXECUTE
    }

    private DatabaseObservationAttributes() {
    }
}
