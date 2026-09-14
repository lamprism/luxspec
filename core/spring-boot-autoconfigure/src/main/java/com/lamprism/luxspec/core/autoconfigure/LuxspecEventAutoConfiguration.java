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

import com.lamprism.luxspec.event.EventDispatcher;
import com.lamprism.luxspec.event.EventPublisher;
import com.lamprism.luxspec.event.SynchronousEventDispatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Supplies the default synchronous event dispatcher.
 *
 * @author RollW
 */
@AutoConfiguration
public class LuxspecEventAutoConfiguration {
    /**
     * Creates the default synchronous event dispatcher when no event publisher or dispatcher is
     * supplied.
     *
     * @return the core event dispatcher
     */
    @Bean
    @ConditionalOnMissingBean({EventDispatcher.class, EventPublisher.class})
    public EventDispatcher luxspecEventDispatcher() {
        return new SynchronousEventDispatcher((context, failure) -> {
            throw new IllegalStateException(
                    "Event listener failed for: " + context.getEvent().getClass().getName(),
                    failure
            );
        });
    }
}
