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

package com.lamprism.luxspec.config.runtime;

import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.policy.ConfigPolicyOperation;
import com.lamprism.luxspec.config.source.ConfigSourceId;
import org.jspecify.annotations.Nullable;

/**
 * Indicates that a Source rejected or failed a typed write.
 *
 * <p>The exception deliberately omits the raw and typed value. A write may contain sensitive
 * configuration data. The original provider failure is retained as the cause for trusted
 * diagnostics, but it is not intended for direct user-facing output.</p>
 *
 * @author RollW
 */
public final class ConfigWriteException extends RuntimeException {
    /**
     * Creates a write failure without an underlying cause.
     *
     * @param key       the configuration key
     * @param sourceId  the source instance ID
     * @param operation the failed mutation operation
     */
    public ConfigWriteException(ConfigKey key, ConfigSourceId sourceId, ConfigPolicyOperation operation) {
        this(key, sourceId, operation, null);
    }

    /**
     * Creates a write failure while retaining the provider failure for internal diagnostics.
     *
     * <p>The message still omits raw and typed values because configuration writes may contain
     * sensitive data.</p>
     *
     * @param key       the configuration key
     * @param sourceId  the source instance ID
     * @param operation the failed mutation operation
     * @param cause     the provider failure, when available
     */
    public ConfigWriteException(
            ConfigKey key,
            ConfigSourceId sourceId,
            ConfigPolicyOperation operation,
            @Nullable Throwable cause
    ) {
        super(
                "Configuration write failed for key: " + key.getValue()
                        + " in source: " + sourceId.getValue()
                        + " during operation: " + operation,
                cause
        );
    }
}
