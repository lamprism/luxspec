package com.lamprism.luxspec.config.catalog;

import com.lamprism.luxspec.config.ConfigBinding;
import com.lamprism.luxspec.config.ConfigKey;
import com.lamprism.luxspec.config.ConfigSpec;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Provides read-only access to registered configuration definitions and reverse lookup.
 *
 * @author RollW
 */
public interface ConfigCatalog {
    /**
     * Returns an immutable snapshot of the definitions registered in this catalog.
     *
     * <p>The default is empty so existing custom catalog implementations remain source compatible.
     * Implementations that support registration should override this method.</p>
     *
     * @return the registered configuration definitions in registration order
     */
    default List<ConfigSpec<?>> getDefinitions() {
        return List.of();
    }

    /**
     * Resolves one complete key to a registered binding.
     *
     * @param key the complete configuration key
     * @return the matching binding, or {@code null} when no definition is registered
     */
    @Nullable ConfigBinding<?> resolve(ConfigKey key);
}
