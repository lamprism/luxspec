package com.lamprism.luxspec.config.runtime;

import com.lamprism.luxspec.LuxspecException;
import com.lamprism.luxspec.config.ConfigErrorCode;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.policy.ConfigPolicyOperation;
import com.lamprism.luxspec.config.source.ConfigSourceId;

/**
 * Indicates that a Source rejected or failed a typed write.
 *
 * <p>The exception deliberately omits the raw and typed value. A write may contain sensitive
 * configuration data, and the adapter failure is not part of the public error contract.</p>
 *
 * @author RollW
 */
public final class ConfigWriteException extends LuxspecException {
    public ConfigWriteException(ConfigKey key, ConfigSourceId sourceId, ConfigPolicyOperation operation) {
        super(
                ConfigErrorCode.SOURCE_WRITE_FAILED,
                "Configuration write failed for key: " + key.getValue()
                        + " in source: " + sourceId.getValue()
                        + " during operation: " + operation
        );
    }
}
