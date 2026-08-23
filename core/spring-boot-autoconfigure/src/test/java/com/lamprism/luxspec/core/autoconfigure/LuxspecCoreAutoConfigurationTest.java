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
import com.lamprism.luxspec.resource.ResourceIdGenerator;
import com.lamprism.luxspec.resource.UuidResourceIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

class LuxspecCoreAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LuxspecCoreAutoConfiguration.class));

    @Test
    void createsExecutionContextTaskDecoratorByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(TaskDecorator.class))
                .isInstanceOf(SpringExecutionContextTaskDecorator.class));
    }

    @Test
    void backsOffWhenApplicationProvidesTaskDecorator() {
        TaskDecorator applicationDecorator = runnable -> runnable;

        contextRunner
                .withBean(TaskDecorator.class, () -> applicationDecorator)
                .run(context -> assertThat(context.getBean(TaskDecorator.class))
                        .isSameAs(applicationDecorator));
    }

    @Test
    void createsUuidResourceIdGeneratorByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(ResourceIdGenerator.class))
                .isInstanceOf(UuidResourceIdGenerator.class));
    }

    @Test
    void backsOffWhenApplicationProvidesResourceIdGenerator() {
        ResourceIdGenerator<String> applicationGenerator = resourceType -> "application-id";

        contextRunner
                .withBean(ResourceIdGenerator.class, () -> applicationGenerator)
                .run(context -> assertThat(context.getBean(ResourceIdGenerator.class))
                        .isSameAs(applicationGenerator));
    }

    @Test
    void createsEventDispatcherByDefault() {
        contextRunner.run(context -> assertThat(context.getBean(EventDispatcher.class))
                .isInstanceOf(EventDispatcherImpl.class));
    }
}
