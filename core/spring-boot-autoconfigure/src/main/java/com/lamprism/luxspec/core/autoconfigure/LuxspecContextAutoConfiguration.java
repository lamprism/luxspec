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

import com.lamprism.luxspec.context.CorrelationIdGenerator;
import com.lamprism.luxspec.context.ExecutionContextStorage;
import com.lamprism.luxspec.context.UuidCorrelationIdGenerator;
import com.lamprism.luxspec.context.spring.SpringExecutionContextTaskDecorator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

/**
 * Supplies the default correlation ID generator and Spring execution-context propagation.
 *
 * @author RollW
 */
@AutoConfiguration(after = LuxspecExecutionContextStorageAutoConfiguration.class)
public class LuxspecContextAutoConfiguration {
    /**
     * Creates the default UUID correlation ID generator.
     *
     * @return the correlation ID generator
     */
    @Bean
    @ConditionalOnMissingBean(CorrelationIdGenerator.class)
    public CorrelationIdGenerator luxspecCorrelationIdGenerator() {
        return new UuidCorrelationIdGenerator();
    }

    /**
     * Creates the Spring task decorator for the selected context storage.
     *
     * @param storage the selected context storage
     * @return the context task decorator
     */
    @Bean
    @ConditionalOnBean(ExecutionContextStorage.class)
    @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator luxspecExecutionContextTaskDecorator(ExecutionContextStorage storage) {
        return new SpringExecutionContextTaskDecorator(storage);
    }
}
