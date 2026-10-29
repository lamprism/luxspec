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

import com.lamprism.luxspec.config.ConfigKey;

/**
 * Indicates that a configuration operation was rejected by its policy.
 *
 * @author RollW
 */
public final class ConfigPolicyException extends IllegalArgumentException {
    /**
     * Creates a policy violation for one binding and operation.
     *
     * @param key       the affected complete key
     * @param operation the rejected operation
     * @param decision  the policy decision
     */
    public ConfigPolicyException(
            ConfigKey key,
            ConfigPolicyOperation operation,
            ConfigPolicyDecision decision
    ) {
        super("Configuration policy rejected " + operation + " for key: " + key.getValue()
                + " with decision: " + decision);
    }
}
