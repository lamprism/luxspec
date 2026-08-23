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

import com.lamprism.luxspec.context.spring.SpringExecutionContextTaskDecorator;
import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventDispatcherImpl;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.resource.ResourceIdGenerator;
import com.lamprism.luxspec.resource.UuidResourceIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

/**
 * Supplies the core Spring adapter defaults.
 *
 * @author RollW
 */
@AutoConfiguration
@ConditionalOnClass(TaskDecorator.class)
public class LuxspecCoreAutoConfiguration {
    /**
     * Creates the execution-context task decorator when the application has not supplied one.
     *
     * @return the context-propagating task decorator
     */
    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator luxspecTaskDecorator() {
        return new SpringExecutionContextTaskDecorator();
    }

    /**
     * Creates the default UUID resource ID generator when the application has not supplied one.
     *
     * @return the UUID resource ID generator
     */
    @Bean
    @ConditionalOnMissingBean(ResourceIdGenerator.class)
    public ResourceIdGenerator<String> luxspecResourceIdGenerator() {
        return new UuidResourceIdGenerator();
    }

    /**
     * Creates the default synchronous event dispatcher when no event publisher
     * or dispatcher is supplied.
     *
     * @return the core event dispatcher
     */
    @Bean
    @ConditionalOnMissingBean({EventDispatcher.class, EventPublisher.class})
    public EventDispatcher luxspecEventDispatcher() {
        return new EventDispatcherImpl((event, listener, failure) -> {
            throw new IllegalStateException(
                    "Event listener failed for: " + event.getClass().getName(),
                    failure
            );
        });
    }
}
