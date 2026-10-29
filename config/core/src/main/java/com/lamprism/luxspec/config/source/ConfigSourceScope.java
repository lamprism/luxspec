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

package com.lamprism.luxspec.config.source;

/**
 * Identifies the lifecycle boundary at which a configuration source is available.
 *
 * <p>A bootstrap source is available before runtime infrastructure is created. A runtime source
 * may depend on that infrastructure and must not be used to resolve bootstrap settings.</p>
 *
 * @author RollW
 */
public enum ConfigSourceScope {
    /**
     * Available before runtime infrastructure is created.
     */
    BOOTSTRAP,
    /**
     * Available after runtime infrastructure has been created.
     */
    RUNTIME
}
