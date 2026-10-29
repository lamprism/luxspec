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

package com.lamprism.luxspec.config.cache;

import com.lamprism.luxspec.config.ConfigBinding;

/**
 * Selects which bindings a caching Provider may retain.
 *
 * <p>This is Provider assembly configuration, not ConfigSpec metadata.</p>
 *
 * @author RollW
 */
@FunctionalInterface
public interface ConfigCacheRule {
    /**
     * Reports whether a binding may be cached.
     *
     * @param binding the concrete binding
     * @return {@code true} when the Provider may cache its result
     */
    boolean shouldCache(ConfigBinding<?> binding);

    /**
     * Returns a rule that permits every binding.
     *
     * @return the allow-all rule
     */
    static ConfigCacheRule always() {
        return binding -> true;
    }

    /**
     * Returns a rule that disables caching for every binding.
     *
     * @return the deny-all rule
     */
    static ConfigCacheRule never() {
        return binding -> false;
    }

    /**
     * Returns the conservative default that excludes sensitive definitions.
     *
     * @return the non-sensitive-only rule
     */
    static ConfigCacheRule nonSensitiveOnly() {
        return binding -> !binding.getSpec().isSensitive();
    }
}
