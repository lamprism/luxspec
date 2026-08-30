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

package com.lamprism.luxspec.core.autoconfigure;

import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.ThreadLocalExecutionContextStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Supplies the default thread-bound execution-context storage.
 *
 * <p>The auto-configuration is separate so applications using another execution model can replace
 * it with an {@link ExecutionContextStorage} bean or exclude only this default.</p>
 *
 * @author RollW
 */
@AutoConfiguration
public class LuxspecExecutionContextStorageAutoConfiguration {
    /**
     * Creates the default thread-bound execution-context storage.
     *
     * @return the execution-context storage
     */
    @Bean
    @ConditionalOnMissingBean(ExecutionContextStorage.class)
    public ExecutionContextStorage luxspecExecutionContextStorage() {
        return new ThreadLocalExecutionContextStorage();
    }
}
