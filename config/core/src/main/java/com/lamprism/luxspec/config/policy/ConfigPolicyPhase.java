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

package com.lamprism.luxspec.config.policy;

/**
 * Identifies when a policy context is evaluated in the configuration pipeline.
 *
 * @author RollW
 */
public enum ConfigPolicyPhase {
    /**
     * A definition-level fallback or Source mutation operation is being evaluated.
     */
    OPERATION,
    /**
     * A Source candidate is being evaluated before its raw entry is read.
     */
    BEFORE_SOURCE_READ,
    /**
     * A Source candidate is being evaluated after its raw entry has been read.
     */
    AFTER_SOURCE_READ
}
