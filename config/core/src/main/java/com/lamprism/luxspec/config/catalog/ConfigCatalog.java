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
